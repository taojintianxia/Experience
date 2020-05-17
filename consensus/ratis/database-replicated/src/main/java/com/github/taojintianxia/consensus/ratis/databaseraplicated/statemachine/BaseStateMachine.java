package com.github.taojintianxia.consensus.ratis.databaseraplicated.statemachine;

import com.codahale.metrics.Timer;
import org.apache.ratis.proto.RaftProtos;
import org.apache.ratis.protocol.Message;
import org.apache.ratis.protocol.RaftClientRequest;
import org.apache.ratis.protocol.RaftGroupId;
import org.apache.ratis.protocol.RaftPeerId;
import org.apache.ratis.server.RaftServer;
import org.apache.ratis.server.impl.RaftServerConstants;
import org.apache.ratis.server.protocol.TermIndex;
import org.apache.ratis.server.storage.RaftStorage;
import org.apache.ratis.statemachine.SnapshotInfo;
import org.apache.ratis.statemachine.StateMachine;
import org.apache.ratis.statemachine.StateMachineStorage;
import org.apache.ratis.statemachine.TransactionContext;
import org.apache.ratis.statemachine.impl.SnapshotRetentionPolicy;
import org.apache.ratis.util.LifeCycle;
import org.apache.ratis.util.Preconditions;

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Nianjun Sun
 * @date 2020/5/5 20:29
 */
public class BaseStateMachine implements StateMachine {

    protected final CompletableFuture<RaftServer> server = new CompletableFuture<>();
    protected volatile RaftGroupId groupId;
    protected final LifeCycle lifeCycle = new LifeCycle(getClass().getSimpleName());

    private final AtomicReference<TermIndex> lastAppliedTermIndex = new AtomicReference<>();

    private final SortedMap<Long, CompletableFuture<Void>> transactionFutures = new TreeMap<>();

    public BaseStateMachine() {
        setLastAppliedTermIndex(TermIndex.newTermIndex(0, -1));
    }

    public RaftPeerId getId() {
        return server.isDone() ? server.join().getId() : null;
    }

    @Override
    public LifeCycle.State getLifeCycleState() {
        return lifeCycle.getCurrentState();
    }

    @Override
    public void initialize(RaftServer server, RaftGroupId groupId, RaftStorage storage) throws IOException {
        this.groupId = groupId;
        this.server.complete(server);
        lifeCycle.setName("" + this);
    }

    @Override
    public SnapshotInfo getLatestSnapshot() {
        return getStateMachineStorage().getLatestSnapshot();
    }

    @Override
    public void notifyNotLeader(Collection<TransactionContext> pendingEntries) throws IOException {
        // do nothing
    }

    @Override
    public void pause() {
    }

    @Override
    public void reinitialize() throws IOException {
    }

    @Override
    public TransactionContext applyTransactionSerial(TransactionContext trx) {
        return trx;
    }

    @Override
    public CompletableFuture<Message> applyTransaction(TransactionContext trx) {
        // return the same message contained in the entry
        RaftProtos.LogEntryProto entry = Objects.requireNonNull(trx.getLogEntry());
        updateLastAppliedTermIndex(entry.getTerm(), entry.getIndex());
        return CompletableFuture.completedFuture(
                Message.valueOf(trx.getLogEntry().getStateMachineLogEntry().getLogData()));
    }

    @Override
    public TermIndex getLastAppliedTermIndex() {
        return lastAppliedTermIndex.get();
    }

    protected void setLastAppliedTermIndex(TermIndex newTI) {
        lastAppliedTermIndex.set(newTI);
    }

    @Override
    public void notifyIndexUpdate(long term, long index) {
        updateLastAppliedTermIndex(term, index);
    }

    protected boolean updateLastAppliedTermIndex(long term, long index) {
        final TermIndex newTI = TermIndex.newTermIndex(term, index);
        final TermIndex oldTI = lastAppliedTermIndex.getAndSet(newTI);
        if (!newTI.equals(oldTI)) {
            LOG.trace("{}: update lastAppliedTermIndex from {} to {}", getId(), oldTI, newTI);
            if (oldTI != null) {
                Preconditions.assertTrue(newTI.compareTo(oldTI) >= 0,
                        () -> getId() + ": Failed updateLastAppliedTermIndex: newTI = "
                                + newTI + " < oldTI = " + oldTI);
            }
            return true;
        }

        synchronized (transactionFutures) {
            for (long i; !transactionFutures.isEmpty() && (i = transactionFutures.firstKey()) <= index; ) {
                transactionFutures.remove(i).complete(null);
            }
        }
        return false;
    }

    @Override
    public long takeSnapshot() throws IOException {
        return RaftServerConstants.INVALID_LOG_INDEX;
    }

    @Override
    public StateMachineStorage getStateMachineStorage() {
        return new StateMachineStorage() {
            @Override
            public void init(RaftStorage raftStorage) throws IOException {
            }

            @Override
            public SnapshotInfo getLatestSnapshot() {
                return null;
            }

            @Override
            public void format() throws IOException {
            }

            @Override
            public void cleanupOldSnapshots(SnapshotRetentionPolicy snapshotRetentionPolicy) {
            }
        };
    }

    @Override
    public CompletableFuture<Message> queryStale(Message request, long minIndex) {
        if (getLastAppliedTermIndex().getIndex() < minIndex) {
            synchronized (transactionFutures) {
                if (getLastAppliedTermIndex().getIndex() < minIndex) {
                    return transactionFutures.computeIfAbsent(minIndex, key -> new CompletableFuture<>())
                            .thenCompose(v -> query(request));
                }
            }
        }
        return query(request);
    }

    @Override
    public CompletableFuture<Message> query(Message request) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public TransactionContext startTransaction(RaftClientRequest request) throws IOException {
        return TransactionContext.newBuilder()
                .setStateMachine(this)
                .setClientRequest(request)
                .build();
    }

    @Override
    public TransactionContext cancelTransaction(TransactionContext trx) throws IOException {
        return trx;
    }

    @Override
    public TransactionContext preAppendTransaction(TransactionContext trx) throws IOException {
        return trx;
    }

    @Override
    public void close() throws IOException {
        // do nothing
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ":"
                + (!server.isDone() ? "uninitialized" : getId() + ":" + groupId);
    }

    protected CompletableFuture<Message> recordTime(Timer timer, com.github.taojintianxia.consensus.ratis.databaseraplicated.statemachine.BaseStateMachine.Task task) {
        final Timer.Context timerContext = timer.time();
        try {
            return task.run();
        } finally {
            timerContext.stop();
        }
    }

    protected interface Task {
        CompletableFuture<Message> run();
    }
}
