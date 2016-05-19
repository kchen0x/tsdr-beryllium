/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.syslogs.server.codec;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.tsdr.syslogs.server.datastore.SyslogDatastoreManager;

import java.util.LinkedList;
//import org.opendaylight.tsdr.syslogs.datastore.SyslogDatastoreManager;

/**
 * Created by lvlng on 16-1-7.
 */
public class UDPMessageHandler extends SimpleChannelInboundHandler<DatagramPacket> {
    private SyslogDatastoreManager manager = SyslogDatastoreManager.getInstance();
    //private LinkedList<DatagramPacket> incomingSyslogs = null;
    private LinkedList<Message> incomingSyslogs = null;
    private final static Logger LOGGER = LoggerFactory.getLogger(UDPMessageHandler.class);

//    public UDPMessageHandler(LinkedList<DatagramPacket> incomingSyslogs) {
//        this.incomingSyslogs = incomingSyslogs;
//    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
        String s = msg.content().toString(CharsetUtil.UTF_8);
        LOGGER.info("===KUN===Received message: "+s);
        String ipaddress = msg.sender().getAddress().getHostAddress();
        manager.execute(ipaddress,s);
        Message message =  new Message.MessageBuilder().create()
                .content(s)
                .hostname(ipaddress)
                .build();
        incomingSyslogs.add(message);
        LOGGER.info("===KUN===message added into list");
    }

    public void setIncomingSyslogs(LinkedList<Message> incomingSyslogs) {
        LOGGER.info("===KUN===handler get the list");
        this.incomingSyslogs = incomingSyslogs;
    }
}
