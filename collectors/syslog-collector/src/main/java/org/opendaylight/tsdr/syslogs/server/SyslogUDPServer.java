/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.syslogs.server;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.tsdr.syslogs.server.codec.Message;
import org.opendaylight.tsdr.syslogs.server.codec.UDPMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by lvlng on 16-1-4.
 */
public class  SyslogUDPServer implements SyslogServer {
    private static SyslogUDPServer INSTANCE = new SyslogUDPServer();
    private AtomicInteger port;
    private final Bootstrap b;
    private final EventLoopGroup group;
    private AtomicBoolean status;
    private DataBroker db;
    private LinkedList<Message> incomingSyslogs = null;
    private UDPMessageHandler udpMessageHandler = null;
    private final static Logger LOGGER = LoggerFactory.getLogger(SyslogUDPServer.class);

    public static SyslogUDPServer getINSTANCE() {
        return INSTANCE;
    }

    private SyslogUDPServer() {
        port = new AtomicInteger(-1);
        b = new Bootstrap();
        group = new NioEventLoopGroup();
        status = new AtomicBoolean(false);
        udpMessageHandler = new UDPMessageHandler();
        b.group(group)
                .channel(NioDatagramChannel.class)
                .handler(udpMessageHandler);
    }

    @Override
    public void startServer() throws InterruptedException {
        udpMessageHandler.setIncomingSyslogs(incomingSyslogs);
        b.bind(port.get()).sync();
        status.set(true);
    }

    @Override
    public void stopServer() throws InterruptedException {
        group.shutdownGracefully().sync();
        status.set(false);
    }

    @Override
    public boolean isRunning() {
        return status.get();
    }

    @Override
    public void setPort(int port) throws Exception {
        if (isRunning()) {
            throw new Exception("UDP Server is running at port: " + port + ".");
        } else
            this.port.set(port);
    }

    @Override
    public String getProtocol() {
        return "UDP";
    }


    public void setIncomingSyslogs(LinkedList<Message> incomingSyslogs) {
        this.incomingSyslogs = incomingSyslogs;
    }
}
