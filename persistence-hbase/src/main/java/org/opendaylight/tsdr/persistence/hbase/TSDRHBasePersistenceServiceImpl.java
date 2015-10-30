/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.hbase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;

import org.apache.hadoop.hbase.TableNotFoundException;
import org.opendaylight.tsdr.spi.model.TSDRConstants;
import org.opendaylight.tsdr.spi.persistence.TsdrPersistenceService;
import org.opendaylight.tsdr.spi.scheduler.SchedulerService;
import org.opendaylight.tsdr.spi.util.TsdrPersistenceServiceUtil;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRMetric;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.gettsdrmetrics.output.TSDRMetricRecordList;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrlogrecord.input.TSDRLogRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides HBase implementation of TSDRPersistenceService.
 *
 * @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
 *
 * Created: Feb 24, 2015
 *
 *
 * Revision: April 2, 2015
 * @author <a href="mailto:syedbahm@cisco.com">Basheeruddin Ahmed </a>
 *    --- Introduction of getMetrics in persistence SPI
 *
 *
 *
 */
public class TSDRHBasePersistenceServiceImpl  implements
    TsdrPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(TSDRHBasePersistenceServiceImpl.class);
    private ScheduledFuture future;

    /**
     * Constructor.
     */
    public TSDRHBasePersistenceServiceImpl(){
        TsdrPersistenceServiceUtil.setTsdrPersistenceService(this);
        log.info("TSDR HBase Data Store is initialized.");
        System.out.println("TSDR HBase Data Store is initialized. "
            + "Please do not install another TSDR Data Store without uninstalling HBase Data Store.");

    }


    /*
    *  This overloaded constructor version is added for UT purpose.
    *  Refrain from calling it(except from UT)
    */
    public TSDRHBasePersistenceServiceImpl(HBaseDataStore hbaseDataStore){
       HBaseDataStoreFactory.setHBaseDataStoreIfAbsent(hbaseDataStore);
    }

    /**
     * Store TSDRMetricRecord.        // TODO Auto-generated method stub
     */
    @Override
    public void store(TSDRMetricRecord metrics){
        log.debug("Entering store(TSDRMetricRecord)");
        //convert TSDRRecord to HBaseEntities
        try{
            HBaseEntity entity = convertToHBaseEntity(metrics);
            if ( entity == null){
                log.debug("the entity is null when converting TSDRMetricRecords into hbase entity");
            return;
            }
            HBaseDataStoreFactory.getHBaseDataStore().create(entity);
            flushCommit(entity.getTableName());
        } catch(TableNotFoundException e){
            synchronized(future){
                if(future.isDone() || future.isCancelled()){
                    log.info("Triggering CreateTableTask");
                    CreateTableTask createTableTask = new CreateTableTask();
                    Long interval = HBaseDataStoreContext.getPropertyInLong(HBaseDataStoreContext.HBASE_COMMON_PROP_CREATE_TABLE_RETRY_INTERVAL);
                    future = SchedulerService.getInstance().scheduleTaskAtFixedRate(createTableTask, 0L, interval);
                }
            }
        }
         log.debug("Exiting store(TSDRMetricRecord)");
     }

    /**
     * Store a list of TSDRMetricRecord.
    */
    @Override
    public void store(List<TSDRRecord> metricList){
        log.debug("Entering store(List<TSDRMetricRecord>)");
        //tableName, entityList Map
        Map<String, List<HBaseEntity>> entityListMap = new HashMap<String, List<HBaseEntity>>();
        List<HBaseEntity> entityList = new ArrayList<HBaseEntity>();
        if ( metricList != null && metricList.size() != 0){
            try{
                for(TSDRRecord metrics: metricList){
                    HBaseEntity entity = convertToHBaseEntity((TSDRMetricRecord)metrics);
                    if ( entity == null){
                        log.debug("the entity is null when converting TSDRMetricRecords into hbase entity");
                        return;
                    }
                    String tableName = entity.getTableName();
                    if ( entityListMap.get(tableName) == null){
                        entityListMap.put(tableName, new ArrayList<HBaseEntity>());
                    }
                    entityListMap.get(tableName).add(entity);
                    entityList.add(entity);
                }
                Set<String> keys = entityListMap.keySet();
                Iterator<String> iter = keys.iterator();
                while ( iter.hasNext()){
                    String tableName = iter.next();
                    HBaseDataStoreFactory.getHBaseDataStore().create(entityListMap.get(tableName));
                    flushCommit(tableName);
                }
                closeConnections();
            } catch(TableNotFoundException e){
                synchronized(future){
                    if(future.isDone() || future.isCancelled()){
                        log.info("Triggering CreateTableTask");
                        CreateTableTask createTableTask = new CreateTableTask();
                        Long interval = HBaseDataStoreContext.getPropertyInLong(HBaseDataStoreContext.HBASE_COMMON_PROP_CREATE_TABLE_RETRY_INTERVAL);
                        future = SchedulerService.getInstance().scheduleTaskAtFixedRate(createTableTask, 0L, interval);
                    }
                }
            }
        }
        log.debug("Exiting store(List<TSDRMetricRecord>)");
    }

    /**
     * Start TSDRHBasePersistenceService.
    }*/

    @Override
    public void start(int timeout) {
         log.debug("Entering start(timeout)");
         //create the HTables used in TSDR.
         CreateTableTask createTableTask = new CreateTableTask();
         future = SchedulerService.getInstance().scheduleTask(createTableTask);
         log.debug("Exiting start(timeout)");
    }
    /**
     * Stop TSDRHBasePersistenceService.
     */
    @Override public void stop(int timeout) {
       log.debug("Entering stop(timeout)");
        closeConnections();
        log.debug("Exiting stop(timeout)");
    }
/**
 * Retrieve a list of HBaseEntity based on metrics Category, start time, and end time.
 */
    @Override
    public List<?> getMetrics(String metricsCategory, Date startDateTime, Date endDateTime) {
        //this is for testing only. Eventually the metricsCategory is required argument in the list command
        if ( metricsCategory == null || metricsCategory.length() == 0){
            metricsCategory = TSDRConstants.PORT_STATS_CATEGORY_NAME;
        }
        List<HBaseEntity> resultEntities = null;
        long startTime = startDateTime == null? 0: startDateTime.getTime();
        long endTime = endDateTime == null? 0: endDateTime.getTime();
        if ( metricsCategory.equalsIgnoreCase(TSDRConstants.FLOW_STATS_CATEGORY_NAME)){
            String tableName = TSDRHBaseDataStoreConstants.FLOW_STATS_TABLE_NAME;
            resultEntities = HBaseDataStoreFactory.getHBaseDataStore().getDataByTimeRange
                (tableName,startTime, endTime);
        }else if (metricsCategory.equalsIgnoreCase(TSDRConstants.FLOW_TABLE_STATS_CATEGORY_NAME)){
            String tableName = TSDRHBaseDataStoreConstants.FLOW_TABLE_STATS_TABLE_NAME;
            resultEntities = HBaseDataStoreFactory.getHBaseDataStore().getDataByTimeRange
                (tableName,startTime, endTime);
        }else if ( metricsCategory.equalsIgnoreCase(TSDRConstants.PORT_STATS_CATEGORY_NAME)){
            String tableName = TSDRHBaseDataStoreConstants.INTERFACE_METRICS_TABLE_NAME;
            resultEntities = HBaseDataStoreFactory.getHBaseDataStore().getDataByTimeRange
                (tableName,startTime, endTime);
        }else if ( metricsCategory.equalsIgnoreCase(TSDRConstants.QUEUE_STATS_CATEGORY_NAME)){
            String tableName = TSDRHBaseDataStoreConstants.QUEUE_METRICS_TABLE_NAME;
            resultEntities = HBaseDataStoreFactory.getHBaseDataStore().getDataByTimeRange
                (tableName,startTime, endTime);
        }else if ( metricsCategory.equalsIgnoreCase(TSDRConstants.FLOW_GROUP_STATS_CATEGORY_NAME)){
            String tableName = TSDRHBaseDataStoreConstants.GROUP_METRICS_TABLE_NAME;
            resultEntities = HBaseDataStoreFactory.getHBaseDataStore().getDataByTimeRange
                (tableName,startTime, endTime);
        }else if ( metricsCategory.equalsIgnoreCase(TSDRConstants.FLOW_METER_STATS_CATEGORY_NAME)){
            String tableName = TSDRHBaseDataStoreConstants.METER_METRICS_TABLE_NAME;
            resultEntities = HBaseDataStoreFactory.getHBaseDataStore().getDataByTimeRange
                (tableName,startTime, endTime);
        }else{
            log.warn("The metricsCategory {} is not supported", metricsCategory);
            return null;
        }
        List<String> resultRecords = HBasePersistenceUtil.convertToStringResultList( resultEntities);
        return resultRecords;
    }


    @Override
    /**
     * Retrieve a list of TSDRMetricRecords from HBase data store based on the
     * specified data category, startTime, and endTime.
     */
    public List<?> getTSDRMetrics(DataCategory category, Long startTime, Long endTime){
        if ( category == null ){
            log.error("The data category is not supported");
            return null;
        }
        List<HBaseEntity> resultEntities = null;
        String tableName = HBasePersistenceUtil.getTableNameFrom(category);
        if ( tableName == null || tableName.length() == 0){
            return null;
        }
        resultEntities = HBaseDataStoreFactory.getHBaseDataStore().getDataByTimeRange
                (tableName,startTime, endTime);
        List<TSDRMetricRecordList> resultRecords = HBasePersistenceUtil.convertToTSDRMetrics( category,resultEntities);
        return resultRecords;
    }

    @Override
    public void purgeTSDRRecords(DataCategory category, Long retention_time){
         String tableName = HBasePersistenceUtil.getTableNameFrom(category);
         try{
             HBaseDataStoreFactory.getHBaseDataStore().deleteByTimestamp(tableName, retention_time);
         }catch ( IOException ioe){
             log.error("Error purging TSDR records in HBase data store {}", ioe);
         }
         return;
    }
    @Override
    public void purgeAllTSDRRecords(Long retention_time){
         DataCategory[] categories = DataCategory.values();
         for ( int i = 0; i < categories.length; i++ ){
            DataCategory category = categories[i];
            purgeTSDRRecords(category, retention_time);
         }
         return;
    }
    /**
     * convert TSDRMetricRecord to HBaseEntity.
     * @param metrics
     * @return
    */
    private HBaseEntity convertToHBaseEntity(TSDRMetricRecord metrics){
        log.debug("Entering convertToHBaseEntity(TSDRMetricRecord)");
        HBaseEntity entity = new HBaseEntity();

        TSDRMetric metricData = metrics;

        if ( metricData != null){
             DataCategory dataCategory = metricData.getTSDRDataCategory();
             if (dataCategory != null){
                 entity = HBasePersistenceUtil.getEntityFromMetricStats(metricData, dataCategory);
             }
        }
        log.debug("Exiting convertToHBaseEntity(TSDRMetricRecord)");
        return entity;
     }

    /**
     * Close connections to the data store.
     */
    public void closeConnections(){
        log.debug("Entering closeConnections()");
        List<String> tableNames = HBasePersistenceUtil.getTSDRHBaseTables();
        for ( String tableName: tableNames){
            HBaseDataStoreFactory.getHBaseDataStore().closeConnection(tableName);
        }
        log.debug("Exiting closeConnections()");
        return;
    }

    /**
     * Create TSDR Tables.
     */
    public void createTables() throws Exception{
        log.debug("Entering createTables()");
        List<String> tableNames = HBasePersistenceUtil.getTSDRHBaseTables();
        for ( String tableName: tableNames){
            HBaseDataStoreFactory.getHBaseDataStore().createTable(tableName);
        }
        log.debug("Exiting createTables()");
        return;
    }

    private  void flushCommit(String tableName){
        HBaseDataStoreFactory.getHBaseDataStore().flushCommit(tableName);
    }

    private void flushCommit(Set<String> tableNames){
        log.debug("Entering flushing commits");
        for ( String tableName: tableNames){
            flushCommit(tableName);
        }
        log.debug("Exiting flushing commits");
    }


    @Override
    public void store(TSDRLogRecord logRecord) {
        throw new UnsupportedOperationException("Log records are not yet supported by HBase");
    }
}
