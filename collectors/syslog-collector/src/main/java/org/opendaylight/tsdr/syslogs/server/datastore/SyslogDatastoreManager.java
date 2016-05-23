package org.opendaylight.tsdr.syslogs.server.datastore;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.tsdr.syslogs.server.codec.Message;
import org.opendaylight.tsdr.syslogs.server.codec.MessageDecoder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.syslog.filter.FilterEntity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.syslog.filter.FilterEntityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.syslog.filter.Listener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.syslog.filter.ListenerKey;

import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.syslog.filter.*;


/**
 * Created by lailailai on 5/10/16.
 */
public class SyslogDatastoreManager implements TsdrSyslogCollectorService,DataChangeListener {
    private static SyslogDatastoreManager INSTANCE = new SyslogDatastoreManager();
    private static final Logger LOG = LoggerFactory.getLogger(TsdrSyslogCollectorService.class);
    private static AtomicInteger messageID = new AtomicInteger(0);
    private final ExecutorService threadpool;
    private DataBroker db;
    private String listenerId;

    private SyslogDatastoreManager() {
        this.db = null;
        this.threadpool = Executors.newCachedThreadPool();
    }

    public static SyslogDatastoreManager getInstance() {
        return INSTANCE;
    }

    public void setDataBroker(DataBroker db) {
        if (this.db == null) {
            this.db = db;
            this.initializeDataTree();
        } else {
            LOG.warn("Syslog DataStore Manager has been set! Ignore new databroker");
        }
    }

    public void execute(String ipaddress, String message) {
        int mid = SyslogDatastoreManager.messageID.addAndGet(1);
        INSTANCE.threadpool.execute(new WorkerThread(mid, ipaddress, message));
    }

//    private void initializeDataTree() {
//        LOG.info("FM: Preparing to initialize the greeting registry");
//        WriteTransaction transaction = db.newWriteOnlyTransaction();
//        InstanceIdentifier<SyslogData> iid = InstanceIdentifier.create(SyslogData.class);
//        SyslogData syslogdata = new SyslogDataBuilder()
//                .build();
//        transaction.put(LogicalDatastoreType.OPERATIONAL, iid, syslogdata);
//        transaction.submit();
//    }

    private void initializeDataTree() {
        LOG.info("Preparing to initialize the greeting registry");
        WriteTransaction transaction = db.newWriteOnlyTransaction();
        InstanceIdentifier<SyslogDispatcher> iid = InstanceIdentifier.create(SyslogDispatcher.class);
        SyslogDispatcher dispatcher = new SyslogDispatcherBuilder().build();
        transaction.put(LogicalDatastoreType.CONFIGURATION, iid, dispatcher);
        transaction.put(LogicalDatastoreType.OPERATIONAL, iid, dispatcher);
        transaction.submit();
    }

    @Override
    public Future<RpcResult<RegisterFilterOutput>> registerFilter(RegisterFilterInput input) {
        //TODO: need to create filter copy within manager.
        LOG.info("Received a lailai Resgister!!!");
        WriteTransaction transaction = db.newWriteOnlyTransaction();
        String filterID = UUID.randomUUID().toString();
        String listenerUUID = UUID.randomUUID().toString();

        //Create filter node
        FilterEntity filterEntity = new FilterEntityBuilder()
                .setSeverity(input.getSeverity())
                .setFacility(input.getFacility())
                .setHost(input.getHost())
                .setApplication(input.getApplication())
                .setSid(input.getSid())
                .setPid(input.getPid())
                .setContent(input.getContent())
                .build();
        InstanceIdentifier<SyslogFilter> filterIID = InstanceIdentifier.create(SyslogDispatcher.class)
                .child(SyslogFilter.class, new SyslogFilterKey(filterID));
        SyslogFilter filter = new SyslogFilterBuilder().setFilterId(filterID)
                .setFilterEntity(filterEntity).build();
        transaction.merge(LogicalDatastoreType.CONFIGURATION, filterIID, filter);

        //Add listener to ListenerList
        InstanceIdentifier<Listener> listenerIID =
                filterIID.child(Listener.class, new ListenerKey(listenerUUID));
        Listener listener = new ListenerBuilder().setListenerId(listenerUUID).build();
        transaction.merge(LogicalDatastoreType.CONFIGURATION, listenerIID, listener);
        //Create Listener on Operational Tree
        InstanceIdentifier<SyslogListener> syslogListenerIID =
                InstanceIdentifier.create(SyslogDispatcher.class)
                        .child(SyslogListener.class, new SyslogListenerKey(listenerUUID));
        SyslogListener syslogListener =
                new SyslogListenerBuilder().setListenerId(listenerUUID).setSyslogMessage("").build();
        transaction.merge(LogicalDatastoreType.OPERATIONAL,syslogListenerIID,syslogListener);

        try {
            transaction.submit().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        RegisterFilterOutput output = new RegisterFilterOutputBuilder()
                .setListenerId(listenerUUID).build();
        this.listenerId=listenerUUID;
        this.listen();

        return RpcResultBuilder.success(output).buildFuture();
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
            LOG.info("get updated message from " + listener.getListenerId());
            LOG.info(listener.getSyslogMessage());
        }
    }


    class WorkerThread implements Runnable {
        private final int mid;
        private final String ipaddr;
        private final String message;

        public WorkerThread(int mid, String ipaddr, String message) {
            this.mid = mid;
            this.ipaddr = ipaddr;
            this.message = message;
            LOG.info("worker thread");
        }

//        private InstanceIdentifier<SyslogDataEntry> toInstanceIdentifier(String str_sequenceid) {
//            InstanceIdentifier<SyslogDataEntry> iid = InstanceIdentifier.create(SyslogData.class)
//                    .child(SyslogDataEntry.class, new SyslogDataEntryKey(str_sequenceid));
//            return iid;
//        }
//
//        private void writeToSyslogDataEntry() {
//            if (mid % 1000 == 0) {
//                LOG.info("Datastore has put " + mid + " nodes.");
//            }
//            WriteTransaction transaction = INSTANCE.db.newWriteOnlyTransaction();
//            String str_sequenceid = String.valueOf(mid);
//            InstanceIdentifier<SyslogDataEntry> iid = toInstanceIdentifier(str_sequenceid);
//
//            SyslogDataEntry data_entry = new SyslogDataEntryBuilder()
//                    .setIpaddr(ipaddr)
//                    .setMessage(message)
//                    .setSyslogId(str_sequenceid)
//                    .build();
//
//            transaction.put(LogicalDatastoreType.OPERATIONAL, iid, data_entry);
//            try {
//                transaction.submit().checkedGet();
//            } catch (TransactionCommitFailedException e) {
//                e.printStackTrace();
//            }
//        }

        private List<SyslogFilter> getFilters() {
            ReadTransaction transaction = db.newReadOnlyTransaction();
            InstanceIdentifier<SyslogDispatcher> iid =
                    InstanceIdentifier.create(SyslogDispatcher.class);
            CheckedFuture<Optional<SyslogDispatcher>, ReadFailedException> future =
                    transaction.read(LogicalDatastoreType.CONFIGURATION, iid);
            Optional<SyslogDispatcher> optional = Optional.absent();
            try {
                optional = future.checkedGet();
            } catch (ReadFailedException e) {
                LOG.warn("Reading Filter failed:", e);
            }
            if (optional.isPresent()) {
                LOG.info("reading filter success");
                return optional.get().getSyslogFilter();
            } else {
                return null;
            }
        }

        private List<Listener> getListenerList(String filterID) {
            ReadTransaction transaction = db.newReadOnlyTransaction();
            InstanceIdentifier<SyslogFilter> iid = InstanceIdentifier.create(SyslogDispatcher.class)
                    .child(SyslogFilter.class, new SyslogFilterKey(filterID));
            CheckedFuture<Optional<SyslogFilter>, ReadFailedException> future =
                    transaction.read(LogicalDatastoreType.CONFIGURATION, iid);
            Optional<SyslogFilter> optional = Optional.absent();
            try {
                optional = future.checkedGet();
            } catch (ReadFailedException e) {
                LOG.warn("Reading Listener failed:", e);
            }
            if (optional.isPresent()) {
                return optional.get().getListener();
            } else {
                return null;
            }
        }

        private void update(List<Listener> nodes) {
            WriteTransaction transaction = db.newWriteOnlyTransaction();
            InstanceIdentifier<SyslogDispatcher> baseIID = InstanceIdentifier.create(SyslogDispatcher.class);
            for (Listener node : nodes) {
                String listenerUUID = node.getListenerId();
                InstanceIdentifier<SyslogListener> iid =
                        baseIID.child(SyslogListener.class, new SyslogListenerKey(listenerUUID));
                SyslogListener listener = new SyslogListenerBuilder()
                        .setListenerId(listenerUUID)
                        .setSyslogMessage(message)
                        .build();
                transaction.put(LogicalDatastoreType.OPERATIONAL,iid,listener);
            }
            transaction.submit();
            LOG.info("submit datastore");
        }

        @Override
        public void run() {
            LOG.info("RUNNNNNNNNNNNNN");
            //TODO: implement new Message Decoder
            Message msg = MessageDecoder.decode(this.message);
            List<Listener> nodes = new ArrayList<>();
            if (msg != null) {
                List<SyslogFilter> filters = this.getFilters();
                for (SyslogFilter filter : filters) {
                    MessageFilter messageFilter = MessageFilter.FilterBuilder.create(filter.getFilterEntity());
                    LOG.info("before equals");
                    if （messageFilter.equals(msg)) {
                        //Match
                        LOG.info("after equals");
                        nodes.addAll(getListenerList(filter.getFilterId()));
                    }
                }
            }
            update(nodes);
          
        }
    }
}
