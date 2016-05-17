/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.syslogs.server;
import io.netty.channel.socket.DatagramPacket;
import java.util.LinkedList;

/**
 * Created by lvlng on 15-12-25.
 */
public interface SyslogServer {
    /**
     * Start the syslog server
     */
    void startServer() throws InterruptedException;

    /**
     * Stop the syslog server
     */
    void stopServer() throws InterruptedException;

    /**
     * Check if the syslog server is running
     */
    boolean isRunning();

    /**
     * Set the port of syslog server
     *
     * @param port
     * @throws Exception when the server is running
     */
    void setPort(int port) throws Exception;

    /**
     * get the protocol used for syslog server
     */
    String getProtocol();
}
