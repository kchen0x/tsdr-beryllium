/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.syslogs.server.codec;

/**
 * Created by lvlng on 16-1-7.
 */

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.handler.codec.TooLongFrameException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.List;

import static org.opendaylight.tsdr.syslogs.server.codec.DecoderUtil.*;

/**
 * Frames syslog messages per RFC-6587.
 */
public class SyslogFramer extends ReplayingDecoder<ByteBuf> {

    private final static Logger LOGGER = LoggerFactory.getLogger(SyslogFramer.class);

    public static final int DEFAULT_MAX_MESSAGE_SIZE = 64 * 1024;

    private final int maxMessageSize;

    public SyslogFramer() {
        this(DEFAULT_MAX_MESSAGE_SIZE);
    }

    public SyslogFramer(int maxMessageSize) {
        this.maxMessageSize = maxMessageSize;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) throws Exception {
        if (buffer.readableBytes() > 0) {
            LOGGER.info("===KUN===Begin to decode.");
            // Decode the content length
            final int length = readDigit(buffer);
            expect(buffer, ' ');
            if (length > maxMessageSize) {
                throw new TooLongFrameException("Received a message of length " + length + ", maximum message length is " + maxMessageSize);
            }
            out.add(buffer.readSlice(length).retain());
        }
        else {
            LOGGER.error("===KUN===get invalid message");
        }
    }

}