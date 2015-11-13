/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.datastorage.persistence.hbase;

import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.lang.Long;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.tsdr.persistence.hbase.HBaseDataStoreFactory;
import org.opendaylight.tsdr.persistence.hbase.HBaseDataStore;
import org.opendaylight.tsdr.persistence.hbase.TSDRHBaseDataStoreConstants;
import org.opendaylight.tsdr.persistence.hbase.TSDRHBasePersistenceServiceImpl;
import org.opendaylight.tsdr.spi.model.TSDRConstants;
import org.opendaylight.tsdr.spi.util.TsdrPersistenceServiceUtil;
import org.opendaylight.tsdr.persistence.hbase.HBaseEntity;
import org.opendaylight.tsdr.persistence.hbase.HBaseColumn;
import org.opendaylight.tsdr.persistence.hbase.HBasePersistenceUtil;

import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeysBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRMetric;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Counter32;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Counter64;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.statistics.FlowStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.statistics.FlowStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.flow.table.statistics.FlowTableStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.flow.table.statistics.FlowTableStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.group.statistics.GroupStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.group.statistics.GroupStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.BucketId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.Bucket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.BucketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.BucketKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.statistics.BucketsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.statistics.buckets.BucketCounter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.statistics.buckets.BucketCounterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.statistics.buckets.BucketCounterKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.statistics.types.rev130925.duration.DurationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.statistics.types.rev130925.node.connector.statistics.PacketsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.flow.capable.node.connector.statistics.FlowCapableNodeConnectorStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.flow.capable.node.connector.statistics.FlowCapableNodeConnectorStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.flow.capable.node.connector.queue.statistics.FlowCapableNodeConnectorQueueStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.flow.capable.node.connector.queue.statistics.FlowCapableNodeConnectorQueueStatisticsBuilder;

import org.mockito.Mockito;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Matchers.any;
import org.mockito.invocation.InvocationOnMock;

import org.mockito.stubbing.Answer;
/**
 * Unit Test for HBase data store under TSDR.
 * * @author <a href="mailto:hariharan_sethuraman@dell.com">Hariharan Sethuraman</a>
 *
 * Created: Apr 27, 2015
 */
import org.opendaylight.yangtools.yang.common.RpcResult;

public class HBaseDataStoreTest {

    public TSDRHBasePersistenceServiceImpl storageService;
    private HBaseDataStore hbaseDataStore;
    private static Map<String, Map<String,List<HBaseEntity>>> tableEntityMap;

    @Before
    public void setup() {
        hbaseDataStore = mock(HBaseDataStore.class);
        storageService = new TSDRHBasePersistenceServiceImpl(hbaseDataStore);
        tableEntityMap = new HashMap<String, Map<String, List<HBaseEntity>>>();

        try{
            doAnswer(new Answer<HBaseEntity>() {
                @Override
                public HBaseEntity answer(InvocationOnMock invocation) throws Throwable {
                    Object[] arguments = invocation.getArguments();
                    if (arguments != null && arguments.length > 0 && arguments[0] != null) {
                        HBaseEntity entity = (HBaseEntity) arguments[0];
                        if(!tableEntityMap.containsKey(entity.getTableName())){
                            tableEntityMap.put(entity.getTableName(),new TreeMap<String,List<HBaseEntity>>());
                        }
                        Map<String,List<HBaseEntity>> entityMap = tableEntityMap.get(entity.getTableName());
                        if(!entityMap.containsKey(entity.getRowKey())){
                            entityMap.put(entity.getRowKey(), new ArrayList<HBaseEntity>());
                        }
                        List<HBaseEntity> entitiesCol = entityMap.get(entity.getRowKey());
                        entitiesCol.add(entity);
                        System.out.println("Creating entity:"+entity.getRowKey() + ",table:" + entity.getTableName());
                        return entity;
                    }
                    return null;
                }
            }).when(hbaseDataStore).create(any(HBaseEntity.class));
        }catch(Exception e){}

        Answer answerDataByRowFamilyQualifier = new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                List<HBaseEntity> entityCol = new ArrayList<HBaseEntity>();
                if (arguments != null && arguments.length > 0 && arguments[0] != null) {
                    String tableName = (String) arguments[0];
                    String startRow = (String) arguments[1];
                    String endRow = (String) arguments[2];
                    String family = (String) arguments[3];
                    String qualifier = (String) arguments[4];
                    Long pagesize = 0l;
                    boolean pagesizeset = false;
                    if(arguments.length == 5){
                        pagesizeset = true;
                        pagesize = (Long) arguments[5];
                    }

                    TreeMap<String, List<HBaseEntity>> entityMap = (TreeMap<String, List<HBaseEntity>>)tableEntityMap.get(tableName);
                    if(entityMap == null){
                        return entityCol;
                    }

                    entityMap = new TreeMap<String, List<HBaseEntity>>(entityMap.subMap(startRow, endRow));
                    for(List<HBaseEntity> entityValues: entityMap.values()){
                        for(HBaseEntity entity: entityValues){
                            for (HBaseColumn currentColumn : entity.getColumns()) {
                                if(currentColumn.getColumnFamily().equals(family) && currentColumn.getColumnQualifier().equals(qualifier)){
                                    entityCol.add(entity);
                                    if(pagesizeset){
                                        pagesize--;
                                        if(pagesize == 0){
                                            break;
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    }
                    System.out.println("Retrieving entities:"+entityCol + ",table:" + tableName);
                }
                return entityCol;
            }
        };

        doAnswer(answerDataByRowFamilyQualifier).when(hbaseDataStore).getDataByRowFamilyQualifier(any(String.class), any(String.class), any(String.class)
                                                            ,any(String.class), any(String.class));
        doAnswer(answerDataByRowFamilyQualifier).when(hbaseDataStore).getDataByRowFamilyQualifier(any(String.class), any(String.class), any(String.class)
                                                            ,any(String.class), any(String.class), any(Long.class));

       doAnswer(new Answer() {
            @Override
            public List<HBaseEntity> answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                List<HBaseEntity> entityCol = new ArrayList<HBaseEntity>();
                if (arguments != null && arguments.length > 0 && arguments[0] != null) {
                    String tableName = (String) arguments[0];
                    Long startTime = (Long) arguments[1];
                    Long endTime = (Long) arguments[2];
                    Map<String, List<HBaseEntity>> entityMap = tableEntityMap.get(tableName);
                    if(entityMap == null){
                        return entityCol;
                    }

                    for(List<HBaseEntity> entityValues: entityMap.values()){
                        for(HBaseEntity entity: entityValues){
                            for (HBaseColumn currentColumn : entity.getColumns()) {
                                if(currentColumn.getTimeStamp() >= startTime && currentColumn.getTimeStamp() <= endTime){
                                     entityCol.add(entity);
                                     break;
                                }
                            }
                        }
                    }
                    System.out.println("Retrieving entities:"+entityCol + ",table:" + tableName + " from date:"+startTime + " end date" + endTime);
                }
                return entityCol;
            }
       }).when(hbaseDataStore).getDataByTimeRange(any(String.class), any(Long.class), any(Long.class));

        try{
        doAnswer(new Answer() {
        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable {
        Object[] args = invocation.getArguments();
        List<HBaseEntity> Deletelist = new ArrayList<HBaseEntity>();
        if (args != null && args.length > 0 && args[0] != null ) {
                String tableName = (String) args[0];
                Long retentionTime = (Long) args[1];
                Map<String, List<HBaseEntity>> entityMap = tableEntityMap.get(tableName);
                for(List<HBaseEntity> entityValues: entityMap.values()){
                        for(HBaseEntity entity: entityValues){
                                for (HBaseColumn currentColumn : entity.getColumns()) {
                                        if(currentColumn.getTimeStamp() <= retentionTime){
                                                Deletelist.add(entity);
                                                }
                                        }
                                }
                        }
                for (HBaseEntity entity: Deletelist){
                        entityMap.remove(entity.getRowKey());
                }
                System.out.println(" Purging entries:"+ Deletelist + " from table:" + tableName + " till time:"+retentionTime);
        }
        return null;
        }}).when(hbaseDataStore).deleteByTimestamp(any(String.class), any(Long.class));
        } catch(Exception e){}


       try{
           Mockito.doNothing().when(hbaseDataStore).createTable(any(String.class));//.thenReturn(true);
       } catch(Exception e){}
       Mockito.doNothing().when(hbaseDataStore).closeConnection(any(String.class));//.thenReturn(true);
    }

    @Test
    public void testFlowStatistics() {
        String timeStamp = (new Long((new Date()).getTime())).toString();
        List<RecordKeys> recordKeys = new ArrayList<RecordKeys>();
        RecordKeys recordKey1 = new RecordKeysBuilder()
            .setKeyName(TSDRConstants.FLOW_KEY_NAME)
            .setKeyValue("flow1").build();
        RecordKeys recordKey2 = new RecordKeysBuilder()
            .setKeyName(TSDRConstants.FLOW_TABLE_KEY_NAME)
            .setKeyValue("table1").build();
        recordKeys.add(recordKey1);
        recordKeys.add(recordKey2);

        TSDRMetricRecordBuilder builder1 = new TSDRMetricRecordBuilder();

        TSDRMetric tsdrMetric1 =   builder1.setMetricName("PacketCount")
            .setMetricValue(new BigDecimal(Double.parseDouble("10000000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.FLOWSTATS)
            .setTimeStamp(new Long(timeStamp)).build();

        TSDRMetricRecordBuilder builder2 = new TSDRMetricRecordBuilder();

        TSDRMetric tsdrMetric2 =   builder2.setMetricName("ByteCount")
            .setMetricValue(new BigDecimal(Double.parseDouble("10000000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.FLOWSTATS)
            .setTimeStamp(new Long(timeStamp)).build();

        boolean result = true;
        try{
            storageService.store((TSDRMetricRecord)tsdrMetric1);
            storageService.store((TSDRMetricRecord)tsdrMetric2);

            result = ((storageService.getMetrics(TSDRConstants.FLOW_STATS_CATEGORY_NAME,new Date(0L),new Date(Long.parseLong(timeStamp)))).size() == 2);


        }catch(Exception ee){
            System.out.println("Error retrieving metrics from flow stats table with specified time range.");
            result = false;
            ee.printStackTrace();
        }
        assertTrue(result);
    }

    @Test
    public void testFlowTableStatistics() {
        String timeStamp = (new Long((new Date()).getTime())).toString();
        List<RecordKeys> recordKeys = new ArrayList<RecordKeys>();
        RecordKeys recordKey = new RecordKeysBuilder()
            .setKeyName(TSDRConstants.FLOW_TABLE_KEY_NAME)
            .setKeyValue("table1").build();
        recordKeys.add(recordKey);

        TSDRMetricRecordBuilder builder1 = new TSDRMetricRecordBuilder();

        TSDRMetric tsdrMetric1 =   builder1.setMetricName("PacketsMatched")
            .setMetricValue(new BigDecimal(Double.parseDouble("20000000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.FLOWTABLESTATS)
            .setTimeStamp(new Long(timeStamp)).build();

        TSDRMetricRecordBuilder builder2 = new TSDRMetricRecordBuilder();

        TSDRMetric tsdrMetric2 =   builder2.setMetricName("ActiveFlows")
            .setMetricValue(new BigDecimal(Double.parseDouble("20000000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.FLOWTABLESTATS)
            .setTimeStamp(new Long(timeStamp)).build();

        TSDRMetricRecordBuilder builder3 = new TSDRMetricRecordBuilder();

        TSDRMetric tsdrMetric3 =   builder3.setMetricName("PacketsLookedUp")
            .setMetricValue(new BigDecimal(Double.parseDouble("20000000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.FLOWTABLESTATS)
            .setTimeStamp(new Long(timeStamp)).build();

        boolean result = true;
        try{
            storageService.store((TSDRMetricRecord)tsdrMetric1);
            storageService.store((TSDRMetricRecord)tsdrMetric2);
            storageService.store((TSDRMetricRecord)tsdrMetric3);

            result = ((storageService.getMetrics(TSDRConstants.FLOW_TABLE_STATS_CATEGORY_NAME,new Date(0L),new Date(Long.parseLong(timeStamp)))).size() == 3);

        }catch(Exception ee){
            System.out.println("Error retrieving metrics from flowtable stats table with specified time range.");
            result = false;
            ee.printStackTrace();
        }
        assertTrue(result);
    }

    @Test
    public void testPortStatistics() {
        String timeStamp = (new Long((new Date()).getTime())).toString();
        List<RecordKeys> recordKeys = new ArrayList<RecordKeys>();
        RecordKeys recordKey = new RecordKeysBuilder()
            .setKeyName(TSDRConstants.INTERNFACE_KEY_NAME)
            .setKeyValue("port1").build();
        recordKeys.add(recordKey);

        TSDRMetricRecordBuilder builder1 = new TSDRMetricRecordBuilder();

        TSDRMetric tsdrMetric1 =   builder1.setMetricName("CollisionCount")
            .setMetricValue(new BigDecimal(Double.parseDouble("2000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.PORTSTATS)
            .setTimeStamp(new Long(timeStamp)).build();


        TSDRMetricRecordBuilder builder2 = new TSDRMetricRecordBuilder();

        TSDRMetric tsdrMetric2 =   builder2.setMetricName("ReceiveCRCError")
            .setMetricValue(new BigDecimal(Double.parseDouble("2000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.PORTSTATS)
            .setTimeStamp(new Long(timeStamp)).build();


        TSDRMetricRecordBuilder builder3 = new TSDRMetricRecordBuilder();

        TSDRMetric tsdrMetric3 =   builder3.setMetricName("ReceivedDrops")
            .setMetricValue(new BigDecimal(Double.parseDouble("2000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.PORTSTATS)
            .setTimeStamp(new Long(timeStamp)).build();


        TSDRMetricRecordBuilder builder4 = new TSDRMetricRecordBuilder();

        TSDRMetric tsdrMetric4 =   builder4.setMetricName("ReceivedErrors")
            .setMetricValue(new BigDecimal(Double.parseDouble("2000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.PORTSTATS)
            .setTimeStamp(new Long(timeStamp)).build();


        TSDRMetricRecordBuilder builder5 = new TSDRMetricRecordBuilder();

        TSDRMetric tsdrMetric5 =   builder5.setMetricName("ReceiveFrameError")
            .setMetricValue(new BigDecimal(Double.parseDouble("2000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.PORTSTATS)
            .setTimeStamp(new Long(timeStamp)).build();


        TSDRMetricRecordBuilder builder6 = new TSDRMetricRecordBuilder();

        TSDRMetric tsdrMetric6 =   builder6.setMetricName("ReceiveOverRunError")
            .setMetricValue(new BigDecimal(Double.parseDouble("2000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.PORTSTATS)
            .setTimeStamp(new Long(timeStamp)).build();


        TSDRMetricRecordBuilder builder7 = new TSDRMetricRecordBuilder();

        TSDRMetric tsdrMetric7 =   builder7.setMetricName("TransmitDrops")
            .setMetricValue(new BigDecimal(Double.parseDouble("2000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.PORTSTATS)
            .setTimeStamp(new Long(timeStamp)).build();


        TSDRMetricRecordBuilder builder8 = new TSDRMetricRecordBuilder();

        TSDRMetric tsdrMetric8 =   builder8.setMetricName("TransmitErrors")
            .setMetricValue(new BigDecimal(Double.parseDouble("2000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.PORTSTATS)
            .setTimeStamp(new Long(timeStamp)).build();


        TSDRMetricRecordBuilder builder9 = new TSDRMetricRecordBuilder();

        TSDRMetric tsdrMetric9 =   builder9.setMetricName("ReceivedPackets")
            .setMetricValue(new BigDecimal(Double.parseDouble("2000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.PORTSTATS)
            .setTimeStamp(new Long(timeStamp)).build();

        TSDRMetricRecordBuilder builder10 = new TSDRMetricRecordBuilder();

        TSDRMetric tsdrMetric10 =   builder10.setMetricName("TransmittedPackets")
            .setMetricValue(new BigDecimal(Double.parseDouble("2000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.PORTSTATS)
            .setTimeStamp(new Long(timeStamp)).build();


        TSDRMetricRecordBuilder builder11 = new TSDRMetricRecordBuilder();

        TSDRMetric tsdrMetric11 =   builder11.setMetricName("ReceivedBytes")
            .setMetricValue(new BigDecimal(Double.parseDouble("20000000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.PORTSTATS)
            .setTimeStamp(new Long(timeStamp)).build();


        TSDRMetricRecordBuilder builder12 = new TSDRMetricRecordBuilder();

        TSDRMetric tsdrMetric12 =   builder12.setMetricName("TransmittedBytes")
            .setMetricValue(new BigDecimal(Double.parseDouble("20000000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.PORTSTATS)
            .setTimeStamp(new Long(timeStamp)).build();

        TSDRMetricRecordBuilder builder13 = new TSDRMetricRecordBuilder();

        TSDRMetric tsdrMetric13 =   builder13.setMetricName("DurationInSeconds")
            .setMetricValue(new BigDecimal(Double.parseDouble("20")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.PORTSTATS)
            .setTimeStamp(new Long(timeStamp)).build();


        TSDRMetricRecordBuilder builder14 = new TSDRMetricRecordBuilder();

        TSDRMetric tsdrMetric14 =   builder14.setMetricName("DurationInNanoSeconds")
            .setMetricValue(new BigDecimal(Double.parseDouble("20")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.PORTSTATS)
            .setTimeStamp(new Long(timeStamp)).build();

        boolean result = true;
        try{
            storageService.store((TSDRMetricRecord)tsdrMetric1);
            storageService.store((TSDRMetricRecord)tsdrMetric2);
            storageService.store((TSDRMetricRecord)tsdrMetric3);
            storageService.store((TSDRMetricRecord)tsdrMetric4);
            storageService.store((TSDRMetricRecord)tsdrMetric5);
            storageService.store((TSDRMetricRecord)tsdrMetric6);
            storageService.store((TSDRMetricRecord)tsdrMetric7);
            storageService.store((TSDRMetricRecord)tsdrMetric8);
            storageService.store((TSDRMetricRecord)tsdrMetric9);
            storageService.store((TSDRMetricRecord)tsdrMetric10);
            storageService.store((TSDRMetricRecord)tsdrMetric11);
            storageService.store((TSDRMetricRecord)tsdrMetric12);
            storageService.store((TSDRMetricRecord)tsdrMetric13);
            storageService.store((TSDRMetricRecord)tsdrMetric14);

            result = ((storageService.getMetrics(TSDRConstants.PORT_STATS_CATEGORY_NAME,new Date(0L),new Date(Long.parseLong(timeStamp)))).size() == 14);
        }catch(Exception ee){
            System.out.println("Error retrieving metrics from port stats table with specified time range.");
            result = false;
            ee.printStackTrace();
        }
        assertTrue(result);
    }

    @Test
    public void testQueueStatistics() {
        String timeStamp = (new Long((new Date()).getTime())).toString();
        List<RecordKeys> recordKeys = new ArrayList<RecordKeys>();
        RecordKeys recordKey1 = new RecordKeysBuilder()
            .setKeyName(TSDRConstants.QUEUE_KEY_NAME)
            .setKeyValue("queue1").build();
        RecordKeys recordKey2 = new RecordKeysBuilder()
            .setKeyName(TSDRConstants.INTERNFACE_KEY_NAME)
            .setKeyValue("port1").build();
        recordKeys.add(recordKey1);
        recordKeys.add(recordKey2);

        TSDRMetricRecordBuilder builder1 = new TSDRMetricRecordBuilder();

        TSDRMetric tsdrMetric1 =   builder1.setMetricName("TransmissionErrors")
            .setMetricValue(new BigDecimal(Double.parseDouble("3000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.QUEUESTATS)
            .setTimeStamp(new Long(timeStamp)).build();

       TSDRMetricRecordBuilder builder2 = new TSDRMetricRecordBuilder();

       TSDRMetric tsdrMetric2 =   builder2.setMetricName("TransmittedBytes")
            .setMetricValue(new BigDecimal(Double.parseDouble("3000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.QUEUESTATS)
            .setTimeStamp(new Long(timeStamp)).build();

       TSDRMetricRecordBuilder builder3 = new TSDRMetricRecordBuilder();

       TSDRMetric tsdrMetric3 =   builder3.setMetricName("TransmittedPackets")
            .setMetricValue(new BigDecimal(Double.parseDouble("3000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.QUEUESTATS)
            .setTimeStamp(new Long(timeStamp)).build();

        boolean result = true;

        try{
           storageService.store((TSDRMetricRecord)tsdrMetric1);
           storageService.store((TSDRMetricRecord)tsdrMetric2);
           storageService.store((TSDRMetricRecord)tsdrMetric3);

            result = ((storageService.getMetrics(TSDRConstants.QUEUE_STATS_CATEGORY_NAME,new Date(0L),new Date(Long.parseLong(timeStamp)))).size() == 3);

        }catch(Exception ee){
            System.out.println("Error retrieving metrics from queue meter stats table with specified time range.");
            result = false;
            ee.printStackTrace();
        }
        assertTrue(result);
    }

    @Test
    public void testFlowMeterStatistics() {
        List<TSDRMetricRecord> metricCol = new ArrayList<TSDRMetricRecord>();
        String timeStamp = (new Long((new Date()).getTime())).toString();
        List<RecordKeys> recordKeys = new ArrayList<RecordKeys>();
        RecordKeys recordKey1 = new RecordKeysBuilder()
            .setKeyName(TSDRConstants.METER_KEY_NAME)
            .setKeyValue("meter1").build();
        RecordKeys recordKey2 = new RecordKeysBuilder()
            .setKeyName(TSDRConstants.GROUP_KEY_NAME)
            .setKeyValue("group1").build();
        recordKeys.add(recordKey1);
        recordKeys.add(recordKey2);

        TSDRMetricRecordBuilder builder1 = new TSDRMetricRecordBuilder();

        TSDRMetric tsdrMetric1 =   builder1.setMetricName("PacketCount")
            .setMetricValue(new BigDecimal(Double.parseDouble(new Long(40).toString())))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.FLOWMETERSTATS)
            .setTimeStamp(new Long(timeStamp)).build();

       TSDRMetricRecordBuilder builder2 = new TSDRMetricRecordBuilder();

       TSDRMetric tsdrMetric2 =   builder2.setMetricName("ByteCount")
            .setMetricValue(new BigDecimal(Double.parseDouble(new Long(40).toString())))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.FLOWMETERSTATS)
            .setTimeStamp(new Long(timeStamp)).build();

       TSDRMetricRecordBuilder builder3 = new TSDRMetricRecordBuilder();

       TSDRMetric tsdrMetric3 =   builder3.setMetricName("RefCount")
            .setMetricValue(new BigDecimal(Double.parseDouble("4000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.FLOWMETERSTATS)
            .setTimeStamp(new Long(timeStamp)).build();

        boolean result = true;
        try{
            storageService.store((TSDRMetricRecord)tsdrMetric1);
            storageService.store((TSDRMetricRecord)tsdrMetric2);
            storageService.store((TSDRMetricRecord)tsdrMetric3);

            result = storageService.getMetrics(TSDRConstants.FLOW_METER_STATS_CATEGORY_NAME,new Date(0L),new Date(Long.parseLong(timeStamp))).size() == 3;
        }catch(Exception ee){
            System.out.println("Error retrieving metrics from flow meter stats table with specified time range.");
            result = false;
            ee.printStackTrace();
        }

        assertTrue(result);
    }


    @Test
    public void testGroupStatistics() {
        String timeStamp = (new Long((new Date()).getTime())).toString();
        List<RecordKeys> recordKeys = new ArrayList<RecordKeys>();
        RecordKeys recordKey1 = new RecordKeysBuilder()
            .setKeyName(TSDRConstants.GROUP_KEY_NAME)
            .setKeyValue("group1").build();
        RecordKeys recordKey2 = new RecordKeysBuilder()
            .setKeyName(TSDRConstants.BUCKET_KEY_NAME)
            .setKeyValue("bucket1").build();
        recordKeys.add(recordKey1);
        recordKeys.add(recordKey2);

        TSDRMetricRecordBuilder builder1 = new TSDRMetricRecordBuilder();

        TSDRMetric tsdrMetric1 =   builder1.setMetricName("PacketCount")
            .setMetricValue(new BigDecimal(Double.parseDouble(new Long(40).toString())))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.FLOWGROUPSTATS)
            .setTimeStamp(new Long(timeStamp)).build();

       TSDRMetricRecordBuilder builder2 = new TSDRMetricRecordBuilder();

       TSDRMetric tsdrMetric2 =   builder2.setMetricName("ByteCount")
            .setMetricValue(new BigDecimal(Double.parseDouble(new Long(40).toString())))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.FLOWGROUPSTATS)
            .setTimeStamp(new Long(timeStamp)).build();

       TSDRMetricRecordBuilder builder3 = new TSDRMetricRecordBuilder();

       TSDRMetric tsdrMetric3 =   builder3.setMetricName("RefCount")
            .setMetricValue(new BigDecimal(Double.parseDouble("4000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.FLOWGROUPSTATS)
            .setTimeStamp(new Long(timeStamp)).build();

        boolean result = true;

        try{
           storageService.store((TSDRMetricRecord)tsdrMetric1);
           storageService.store((TSDRMetricRecord)tsdrMetric2);
           storageService.store((TSDRMetricRecord)tsdrMetric3);

            result = ((storageService.getMetrics(TSDRConstants.FLOW_GROUP_STATS_CATEGORY_NAME,new Date(0L),new Date(Long.parseLong(timeStamp)))).size() == 3);

        }catch(Exception ee){
            System.out.println("Error retrieving metrics from flow group stats table with specified time range.");
            result = false;
            ee.printStackTrace();
        }
        assertTrue(result);
    }

    @Test
    public void testPurgeStatistics(){
        String timeStamp = (new Long((new Date()).getTime())).toString();
        Long LatestStamp = new Long(timeStamp)+500;
        List<RecordKeys> recordKeys = new ArrayList<RecordKeys>();
        RecordKeys recordKey1 = new RecordKeysBuilder()
            .setKeyName(TSDRConstants.GROUP_KEY_NAME)
            .setKeyValue("group1").build();
        RecordKeys recordKey2 = new RecordKeysBuilder()
            .setKeyName(TSDRConstants.BUCKET_KEY_NAME)
            .setKeyValue("bucket1").build();
        recordKeys.add(recordKey1);
        recordKeys.add(recordKey2);

        TSDRMetricRecordBuilder builder1 = new TSDRMetricRecordBuilder();

        TSDRMetric tsdrMetric1 =   builder1.setMetricName("PacketCount")
            .setMetricValue(new BigDecimal(Double.parseDouble(new Long(40).toString())))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.FLOWGROUPSTATS)
            .setTimeStamp(new Long(timeStamp)).build();

       TSDRMetricRecordBuilder builder2 = new TSDRMetricRecordBuilder();

       TSDRMetric tsdrMetric2 =   builder2.setMetricName("ByteCount")
            .setMetricValue(new BigDecimal(Double.parseDouble(new Long(40).toString())))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.FLOWGROUPSTATS)
            .setTimeStamp(new Long(timeStamp)).build();

       TSDRMetricRecordBuilder builder3 = new TSDRMetricRecordBuilder();

       TSDRMetric tsdrMetric3 =   builder3.setMetricName("RefCount")
            .setMetricValue(new BigDecimal(Double.parseDouble("4000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.FLOWGROUPSTATS)
            .setTimeStamp(new Long(timeStamp)).build();

        TSDRMetricRecordBuilder builder4 = new TSDRMetricRecordBuilder();

        TSDRMetric tsdrMetric4 =   builder4.setMetricName("PacketCount")
            .setMetricValue(new BigDecimal(Double.parseDouble(new Long(40).toString())))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.FLOWGROUPSTATS)
            .setTimeStamp(LatestStamp).build();

       TSDRMetricRecordBuilder builder5 = new TSDRMetricRecordBuilder();

       TSDRMetric tsdrMetric5 =   builder5.setMetricName("ByteCount")
            .setMetricValue(new BigDecimal(Double.parseDouble(new Long(40).toString())))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.FLOWGROUPSTATS)
            .setTimeStamp(LatestStamp).build();

       TSDRMetricRecordBuilder builder6 = new TSDRMetricRecordBuilder();

       TSDRMetric tsdrMetric6 =   builder6.setMetricName("RefCount")
            .setMetricValue(new BigDecimal(Double.parseDouble("4000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.FLOWGROUPSTATS)
            .setTimeStamp(LatestStamp).build();
        boolean result = true;
        try{
            storageService.store((TSDRMetricRecord)tsdrMetric1);
            storageService.store((TSDRMetricRecord)tsdrMetric2);
            storageService.store((TSDRMetricRecord)tsdrMetric3);
            storageService.store((TSDRMetricRecord)tsdrMetric4);
            storageService.store((TSDRMetricRecord)tsdrMetric5);
            storageService.store((TSDRMetricRecord)tsdrMetric6);
            result = ((storageService.getMetrics(TSDRConstants.FLOW_GROUP_STATS_CATEGORY_NAME,new Date(0L),new Date(LatestStamp))).size() == 6);
            storageService.purgeTSDRRecords(DataCategory.FLOWGROUPSTATS,Long.parseLong(timeStamp));
            result = ((storageService.getMetrics(TSDRConstants.FLOW_GROUP_STATS_CATEGORY_NAME,new Date(0L),new Date(LatestStamp))).size() == 3);
        }catch(Exception ee){
            System.out.println("Error purging stats with specified time.");
            result = false;
            ee.printStackTrace();
        }

        assertTrue(result);
    }

    @After
    public void teardown() {
        tableEntityMap.clear();
        tableEntityMap = null;
    }
}
