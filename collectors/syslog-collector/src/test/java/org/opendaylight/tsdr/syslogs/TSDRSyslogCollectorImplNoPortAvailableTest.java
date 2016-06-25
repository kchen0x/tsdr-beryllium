/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.syslogs;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.tsdr.syslogs.server.datastore.SyslogDatastoreManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;

import java.io.IOException;

/**
 * Description:
 * Created by Quentin(quentin.chen@foxmail.com) on 16/6/25.
 */
public class TSDRSyslogCollectorImplNoPortAvailableTest {
    private TsdrCollectorSpiService spiService = Mockito.mock(TsdrCollectorSpiService.class);
    private TSDRSyslogCollectorImpl impl;
    private BindingAwareBroker.ProviderContext session = Mockito.mock(BindingAwareBroker.ProviderContext.class);
    private DataBroker dataBroker = Mockito.mock(DataBroker.class);
    private SyslogDatastoreManager manager = Mockito.mock(SyslogDatastoreManager.class);

    @Before
    public void setup() {
        Mockito.when(session.getSALService(DataBroker.class)).thenReturn(dataBroker);
        impl = new TSDRSyslogCollectorImpl(spiService);
        impl.setManager(manager);
    }

    @Test
    public void testFailToBindToUDPPorts() throws IOException, InterruptedException {
        //To make sure the port is not available
        impl.setUdpPort(80);

        try {
            impl.onSessionInitiated(session);
        }catch (Exception e){
            //Nothing to do with
        }

        Assert.assertTrue(!impl.isRunning());
    }

    @Test
    public void testFailToBindToTCPPorts() throws IOException, InterruptedException {
        //To make sure the port is not available
        impl.setTcpPort(81);

        try {
            impl.onSessionInitiated(session);
        }catch (Exception e){
            //Nothing to do with
        }
        Assert.assertTrue(!impl.isRunning());
    }
}
