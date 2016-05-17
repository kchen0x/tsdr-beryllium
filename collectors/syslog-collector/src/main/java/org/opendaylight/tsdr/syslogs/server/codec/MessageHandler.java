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

import java.net.InetSocketAddress;
import java.util.LinkedList;

/**
 * Created by lvlng on 16-1-4.
 */
public class MessageHandler extends SimpleChannelInboundHandler<String> {
    //private SyslogDatastoreManager manager = SyslogDatastoreManager.getInstance();
    private LinkedList<Message> incomingSyslogs = null;
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, String msg) throws Exception {
        String ipaddress=((InetSocketAddress) channelHandlerContext.channel().remoteAddress())
                .getAddress().getHostAddress();
        //manager.execute(ipaddress,s);
        Message message = new Message.MessageBuilder().create()
                .hostname(ipaddress)
                .content(msg)
                .build();
        incomingSyslogs.add(message);
    }

    public void setIncomingSyslogs(LinkedList<Message> incomingSyslogs) {
        this.incomingSyslogs = incomingSyslogs;
    }
}
