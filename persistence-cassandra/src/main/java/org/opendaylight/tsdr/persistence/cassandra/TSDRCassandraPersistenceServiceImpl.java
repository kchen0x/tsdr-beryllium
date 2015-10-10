/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.cassandra;

import java.util.Date;
import java.util.List;

import org.opendaylight.tsdr.spi.persistence.TsdrPersistenceService;
import org.opendaylight.tsdr.spi.util.TsdrPersistenceServiceUtil;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecord;
/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class TSDRCassandraPersistenceServiceImpl implements TsdrPersistenceService{

    private CassandraStore store = null;

    public TSDRCassandraPersistenceServiceImpl(){
        TsdrPersistenceServiceUtil.setTsdrPersistenceService(this);
        System.out.println("Cassandra Store was initialized...");
    }

    @Override
    public void store(TSDRMetricRecord metricRecord) {
        store.store(metricRecord);
    }

    @Override
    public void store(List<TSDRMetricRecord> metricRecordList) {
       for(TSDRMetricRecord record:metricRecordList){
           store(record);
       }
    }

    @Override
    public void start(int timeout) {
        store = new CassandraStore();
    }

    @Override
    public void stop(int timeout) {
        store.shutdown();
    }

    @Override
    public List<?> getMetrics(String metricsCategory, Date startDateTime,Date endDateTime) {
        return store.getMetrics(metricsCategory, startDateTime, endDateTime);
    }

    @Override
    public List<?> getTSDRMetrics(DataCategory category, Long startTime, Long endTime){
        return null;
       }

    @Override
    public void purgeTSDRRecords(DataCategory category, Long retention_time){
        return;
    }
}