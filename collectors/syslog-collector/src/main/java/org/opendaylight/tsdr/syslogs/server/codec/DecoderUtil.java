/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.syslogs.server.codec;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;

class DecoderUtil {
    static int readDigit(ByteBuf buffer) {
        int digit = 0;
        while (buffer.readableBytes() > 0 && Character.isDigit(peek(buffer))) {
            digit = digit * 10 + buffer.readByte() - '0';
        }
        return digit;
    }

    static byte peek(ByteBuf buffer) {
        return buffer.getByte(buffer.readerIndex());
    }

    static void expect(ByteBuf buffer, char c) {
        if (buffer.readByte() != c) {
            throw new DecoderException("Expected " + c + " at index " + buffer.readerIndex());
        }
    }

    static String readStringToSpace(ByteBuf buffer, boolean checkNull) {
        if (checkNull && peek(buffer) == '-') {
            buffer.readByte();
            return null;
        }
        int length = -1;
        for (int i = buffer.readerIndex(); i < buffer.capacity(); i++) {
            if (buffer.getByte(i) == ' ') {
                length = i - buffer.readerIndex();
                break;
            }
        }
        if (length < 0) {
            length = buffer.readableBytes();
        }
        return new String(buffer.readBytes(length).array());
    }
}