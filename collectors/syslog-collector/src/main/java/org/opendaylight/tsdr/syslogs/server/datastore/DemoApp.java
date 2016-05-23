/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.syslogs.server.datastore;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.SyslogListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.SyslogListenerKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by lvlng on 16-1-13.
 */

public class DemoApp implements DataChangeListener {
    private final DataBroker db;
    private final Logger LOG = LoggerFactory.getLogger(DemoApp.class);
    private final TsdrSyslogCollectorService syslogsvrService;
    private String listenerId;

    public DemoApp(BindingAwareBroker.ProviderContext session) {
        this.db = session.getSALService(DataBroker.class);
        this.syslogsvrService = session.getRpcService(TsdrSyslogCollectorService.class);
    }

    public void registerAndListen(String content) {
        try {
            this.register(content);
            this.listen();
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error(e.getMessage());
        }
    }

    private void  register(String content) throws ExecutionException, InterruptedException {
        Future<RpcResult<RegisterFilterOutput>> future;
        //TODO: check interface status change syslog message string
        RegisterFilterInput input = new RegisterFilterInputBuilder()
                .setContent(content)
                .build();
        future = syslogsvrService.registerFilter(input);
        RegisterFilterOutput output = future.get().getResult();
        this.listenerId = output.getListenerId();
    }

    private InstanceIdentifier<SyslogListener> toInstanceIdentifier(String listenerId) {
        InstanceIdentifier<SyslogListener> iid = InstanceIdentifier.create(SyslogDispatcher.class)
                .child(SyslogListener.class, new SyslogListenerKey(listenerId));
        return iid;
    }

    private void listen() {
        InstanceIdentifier<SyslogListener> iid = this.toInstanceIdentifier(this.listenerId);
        db.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, iid,
                this, AsyncDataBroker.DataChangeScope.SUBTREE);
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        InstanceIdentifier<SyslogListener> iid = this.toInstanceIdentifier(this.listenerId);
        SyslogListener listener = (SyslogListener) change.getUpdatedData().get(iid);
        if (listener != null) {
            LOG.info(listener.getSyslogMessage());
        }

    }

}
