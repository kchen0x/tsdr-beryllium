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
 * @author lvlng(huwenbo1988@gmail.com)
 * @author Quentin Chen(quentin.chen@foxmail.com)
 */
public class UDPMessageHandler extends SimpleChannelInboundHandler<DatagramPacket> {
    private SyslogDatastoreManager manager = SyslogDatastoreManager.getInstance();
    private LinkedList<Message> incomingSyslogs = null;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
        String s = msg.content().toString(CharsetUtil.UTF_8);
        String ipaddress = msg.sender().getAddress().getHostAddress();
        System.out.println("received new message");
        manager.execute(ipaddress,s);

        Message message =  new Message.MessageBuilder().create()
                .content(s)
                .hostname(ipaddress)
                .build();
        incomingSyslogs.add(message);
    }

    public void setIncomingSyslogs(LinkedList<Message> incomingSyslogs) {
        this.incomingSyslogs = incomingSyslogs;
    }
}
