/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.syslogs;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.tsdr.syslogs.filters.SyslogFilterManager;
import org.opendaylight.tsdr.syslogs.server.SyslogTCPServer;
import org.opendaylight.tsdr.syslogs.server.SyslogUDPServer;
import org.opendaylight.tsdr.syslogs.server.codec.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRLogRecordInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramSocket;
import java.util.LinkedList;

/**
 * @author Sharon Aicler(saichler@gmail.com)
 * @author Quentin Chen(quentin.chen@foxmail.com)
 **/
public class TSDRSyslogCollectorImpl extends Thread implements BindingAwareProvider{
    public static final int UDP_PORT = 514;
    public static final int TCP_PORT = 513;
    public static final long QUEUE_WAIT_INTERVAL = 2000;
    public static final long STORE_FLUSH_INTERVAL = 2500;

    SyslogTCPServer tcpServer;
    SyslogUDPServer udpServer;

    private TsdrCollectorSpiService collectorSPIService = null;
    private DatagramSocket socket = null;
    private boolean running = true;
    private Logger logger = LoggerFactory.getLogger(TSDRSyslogCollectorImpl.class);
    private LinkedList<Message> udpPacketList = new LinkedList<>();
    private LinkedList<Message> tcpMessageList = new LinkedList<>();
    private LinkedList<TSDRLogRecord> syslogQueue = new LinkedList<>();
    private SyslogFilterManager filterManager = new SyslogFilterManager();
    private long lastPersisted = System.currentTimeMillis();
    private int udpPort = -1;
    private int tcpPort = -1;

    /***
     * constructor of collector
     * @param _collectorSPIService
     *      invoke collector SPI service to implement tsdr data insertion
     */
    public TSDRSyslogCollectorImpl(TsdrCollectorSpiService _collectorSPIService) {
        super("TSDR Syslog Listener");
        this.collectorSPIService = _collectorSPIService;
    }

    public void setUdpPort(int udpPort) {
        this.udpPort = udpPort;
    }

    public void setTcpPort(int tcpPort) {
        this.tcpPort = tcpPort;
    }

    public boolean isRunning(){
        return this.running;
    }

    /**
     * initiated when the data binding broker is registered
     * in TSDRSyslogModule
     * @param session binding aware broker's provider context
     */
    @Override
    public void onSessionInitiated(BindingAwareBroker.ProviderContext session) {
        this.setDaemon(true);
        logger.info("Syslog Collector Session Initiated");
        this.udpPort = this.udpPort != -1 ?
                this.udpPort : UDP_PORT;
        this.tcpPort = this.tcpPort != -1 ?
                this.tcpPort : TCP_PORT;

        //Start TCP syslog server
        logger.info("Start TCP server");
        try {
            tcpServer = SyslogTCPServer.getINSTANCE();
            tcpServer.setPort(tcpPort);
            tcpServer.setIncomingSyslogs(this.tcpMessageList);
            tcpServer.startServer();
            new SyslogProcessor(this.tcpMessageList).start();
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
        logger.info("TCP server started at port: " + tcpPort + ".");

        //Start UDP syslog server
        logger.info("Start UDP server");
        try {
            udpServer = SyslogUDPServer.getINSTANCE();
            udpServer.setPort(udpPort);
            udpServer.setIncomingSyslogs(this.udpPacketList);
            udpServer.startServer();
//            this.start();
            new SyslogProcessor(this.udpPacketList).start();
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
        logger.info("UDP server started at port: " + udpPort + ".");

//        try{
//            socket = new DatagramSocket(this.selectedPort);
//            logger.info("Syslog collector started on listening on " +
//                    "port " + this.selectedPort);
//            this.start();
//            new SyslogProcessor().start();
//        }catch(Exception err){
//            logger.error("Failed to bind to port "
//                    + this.selectedPort +
//                    ", syslog collector will shutdonw.",err);
//            running = false;
//        }
    }


    public void run(){
    }

    public void close(){
        running = false;
        socket.close();
    }

    private class SyslogProcessor extends Thread {
        private LinkedList<Message> messages = null;
        public SyslogProcessor(LinkedList<Message> messages){
            super("TSDR Syslog Processor");
            this.setDaemon(true);
            this.messages = messages;
        }
        public void run(){
            Message message = null;
            while(running){
                synchronized(messages){
                    if(messages.isEmpty()){
                        try{
                            messages.wait(QUEUE_WAIT_INTERVAL);}catch(InterruptedException e){}
                    }
                    if(!messages.isEmpty()){
                        message = messages.removeFirst();
                    }
                }
                TSDRLogRecord logRecord = filterManager.applyFilters(message);
                if(logRecord!=null){
                    syslogQueue.add(logRecord);
                }
                if(System.currentTimeMillis()-lastPersisted>STORE_FLUSH_INTERVAL && !syslogQueue.isEmpty()){
                    LinkedList<TSDRLogRecord> queue = null;
                    synchronized(filterManager){
                        //Currently there is only one SyslogProcessor thread so this check seems meaningless
                        //If the future if we decide to have a few of those we need to make sure the queue
                        //has something and the interval has passed inside the synchronize block.
                        if(System.currentTimeMillis()-lastPersisted>STORE_FLUSH_INTERVAL && !syslogQueue.isEmpty()){
                            lastPersisted = System.currentTimeMillis();
                            queue = syslogQueue;
                            syslogQueue = new LinkedList<>();
                        }
                    }
                    if(queue!=null){
                        store(queue);
                    }
                }
                message = null;
            }
        }
    }

    private void store(LinkedList<TSDRLogRecord> queue){
        InsertTSDRLogRecordInputBuilder input = new InsertTSDRLogRecordInputBuilder();
        input.setTSDRLogRecord(queue);
        input.setCollectorCodeName("SyslogCollector");
        collectorSPIService.insertTSDRLogRecord(input.build());
    }

    public static int getUdpPort() {
        return UDP_PORT;
    }

    public static int getTcpPort() {
        return TCP_PORT;
    }
}
