/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.syslogs.server.codec;

import org.opendaylight.tsdr.syslogs.server.codec.Message.MessageBuilder;
import java.util.regex.*;


/**
 * Created by lvlng on 16-1-29.
 */
public class MessageDecoder {

    /*regex for syslog:
    * (<[0-9]+>)([0-9]+):[\s]*([^:]*):[\s]*(.*)[\s]*:[\s]*(.+)\[(.+)\][\s]*:[\s]*%(.*)-([0-7])-(.*)[\s]*:[\s]*(.*)
    * Group 1: Pri (Facility * 8 + Severity)
    * Group 2: SequenceID
    * Group 3: Hostname
    * Group 4: Timestamp
    * Group 5: Application
    * Group 6: ProcessID
    * Group 7: MNEMONIC
    * Group 8: Description
    */

    private static final String regex =
            "<([0-9]+)>([0-9]+):([^:]*):(.+):(.+)\\[([0-9]*)]:[\\s]*%.+-[0-7]-(.*):(.*)";
    private static Pattern pattern = Pattern.compile(regex);

    public static Message decode(String msg) {
        Matcher matcher = pattern.matcher(msg);
        matcher.find();
        MessageBuilder builder = new MessageBuilder();
        int pri = Integer.parseInt(matcher.group(1));
        int facility = pri / 8;
        int severity = pri % 8;
        builder.facility(Message.Facility.values()[facility])
                .severity(Message.Severity.values()[severity])
                .sequenceId(matcher.group(2).trim())
                .hostname(matcher.group(3).trim())
                .timestamp(matcher.group(4).trim())
                .applicationName(matcher.group(5).trim())
                .processId(matcher.group(6).trim())
                .content(matcher.group(7).trim() + " : " + matcher.group(8).trim());
        return builder.build();
    }
}
