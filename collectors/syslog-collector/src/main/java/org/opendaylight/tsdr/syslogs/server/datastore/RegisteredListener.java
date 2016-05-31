package org.opendaylight.tsdr.syslogs.server.datastore;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.SyslogListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.SyslogListenerKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by lailailai on 5/11/16.
 */

public class RegisteredListener implements DataChangeListener {
    private DataBroker db;
    private final Logger LOG = LoggerFactory.getLogger(RegisteredListener.class);
    private String listenerId;
    private String callbackurl;

    public RegisteredListener(DataBroker db, String listenerId, String url) {
        this.db = db;
        this.listenerId=listenerId;
        this.callbackurl=url;
    }

    private InstanceIdentifier<SyslogListener> toInstanceIdentifier(String listenerId) {
        InstanceIdentifier<SyslogListener> iid = InstanceIdentifier.create(SyslogDispatcher.class)
                .child(SyslogListener.class, new SyslogListenerKey(listenerId));
        return iid;
    }

    public void listen() {
        InstanceIdentifier<SyslogListener> iid = this.toInstanceIdentifier(this.listenerId);
        ListenerRegistration<DataChangeListener> a =db.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, iid,
                this, AsyncDataBroker.DataChangeScope.SUBTREE);


    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        InstanceIdentifier<SyslogListener> iid = this.toInstanceIdentifier(this.listenerId);
        SyslogListener listener = (SyslogListener) change.getUpdatedData().get(iid);
        if (listener != null) {
            LOG.info("  get updated message from: "+listener.getListenerId());
            LOG.info("  the updated message is: "+listener.getSyslogMessage());

            if (callbackurl !=null) {
                try {
                    String url = callbackurl;
                    URL callbackUrl = new URL(url);
                    URLConnection urlConnection = (HttpURLConnection) callbackUrl.openConnection();
                    urlConnection.setDoOutput(true);
                    urlConnection.setDoInput(true);

                    urlConnection.setRequestProperty("content-type", "application/x-www-form-urlencoded");

                    OutputStreamWriter out = new OutputStreamWriter(urlConnection.getOutputStream());

                    out.write("received updated message "+listener.getSyslogMessage());


                    out.flush();
                    out.close();
                    int responseCode = ((HttpURLConnection) urlConnection).getResponseCode();
                    if (HttpURLConnection.HTTP_OK == responseCode) {
                        String readLine;
                        BufferedReader responseReader;
                        responseReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                        while ((readLine = responseReader.readLine()) != null) {
                            System.out.println(readLine);
                        }
                        responseReader.close();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

    }

}
