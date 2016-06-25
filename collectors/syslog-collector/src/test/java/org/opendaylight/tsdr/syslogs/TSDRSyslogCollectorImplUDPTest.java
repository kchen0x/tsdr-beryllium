/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.syslogs;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.tsdr.syslogs.server.datastore.SyslogDatastoreManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRLogRecordInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecord;


/**
 * @author Sharon Aicler(saichler@gmail.com)
 * @author Quentin Chen(quentin.chen@foxmail.com)
 **/
public class TSDRSyslogCollectorImplUDPTest {
    private DatagramSocket socket = null;
    private TsdrCollectorSpiService spiService = Mockito.mock(TsdrCollectorSpiService.class);
    private DataBroker dataBroker = Mockito.mock(DataBroker.class);
    private SyslogDatastoreManager manager = Mockito.mock(SyslogDatastoreManager.class);
    private BindingAwareBroker.ProviderContext session = Mockito.mock(BindingAwareBroker.ProviderContext.class);
    private TSDRSyslogCollectorImpl impl = new TSDRSyslogCollectorImpl(spiService);
    private final List<TSDRLogRecord> storedRecords = new ArrayList<>();
    private int numberOfTests=0;

    @Before
    public void setup() throws SocketException {
        impl.setUdpPort(8089);
        impl.setTcpPort(8088);
        Mockito.when(session.getSALService(DataBroker.class)).thenReturn(dataBroker);

        numberOfTests++;
        if(socket==null){
            //Arbitrary port
            socket = new DatagramSocket(1234);
            impl.setManager(manager);
            impl.onSessionInitiated(session);
            System.out.println(impl.tcpServer.getProtocol() + " server is listening: " + impl.getTcpPort());
            System.out.println(impl.udpServer.getProtocol() + " server is listening: " + impl.getUdpPort());
            Mockito.when(spiService.insertTSDRLogRecord(Mockito.any(InsertTSDRLogRecordInput.class))).thenAnswer(new Answer<Void>() {
                @Override
                public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                    InsertTSDRLogRecordInput input = (InsertTSDRLogRecordInput) invocationOnMock.getArguments()[0];
                    storedRecords.addAll(input.getTSDRLogRecord());
                    return null;
                }
            });
        }
    }

    public void sendSysLog(String message) throws IOException {
        byte[] data = message.getBytes();
        System.out.println("Messages send to: " + impl.getUdpPort());
        DatagramPacket packet = new DatagramPacket(data,data.length, InetAddress.getByName("127.0.0.1"),impl.getUdpPort());
        socket.send(packet);
    }

    @Test
    public void testSingleSyslog() throws IOException, InterruptedException {
        Assert.assertTrue(impl.isRunning());
        this.storedRecords.clear();
        sendSysLog("<30>1:quentin:May 24 12:22:25:TestProcess[1787]:%3-6-1:This is a test log of cisco.");
        sendSysLog("<30>2:quentin:May 24 12:22:25:TestProcess[1787]:%3-6-1:This is a test log of cisco.");
        sendSysLog("<30>3:quentin:May 24 12:22:25:TestProcess[1787]:%3-6-1:This is a test log of cisco.");
        sendSysLog("<30>4:quentin:May 24 12:22:25:TestProcess[1787]:%3-6-1:This is a test log of cisco.");
        //sleep 5 seconds as Syslog collector flush the buffer every 2.5 seconds
        Thread.sleep(5000);
        Assert.assertEquals(4,this.storedRecords.size());
    }

    @After
    public void tearDown(){
        if(numberOfTests==1){
            impl.close();
            this.socket.close();
        }
    }
}
