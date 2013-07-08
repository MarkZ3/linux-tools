package org.eclipse.linuxtools.tmf.core.trace;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.linuxtools.tmf.core.trace.index.ChunkCache;
import org.eclipse.linuxtools.tmf.core.trace.index.Database;
import org.eclipse.linuxtools.tmf.core.trace.index.IBTreeComparator;
import org.eclipse.linuxtools.tmf.core.trace.index.IBTreeVisitor;

/**
 * @author emalape
 * @since 3.0
 *
 */
@SuppressWarnings("javadoc")
public class TmfPersistentIndex implements ITmfIndex {

    private static final int BLOCKED_WRITE_LOCK_OUTPUT_INTERVAL = 30000;
    private static final int LONG_WRITE_LOCK_REPORT_THRESHOLD = 1000;
    private static final int LONG_READ_LOCK_WAIT_REPORT_THRESHOLD = 1000;
    static boolean sDEBUG_LOCKS= true; // initialized in the PDOMManager, because IBM needs PDOM independent of runtime plugin.

    private Database fDatabase;
//    private BTree fCheckpointTree;
//    private BTree fCheckpointRankTree;
    private ITmfTrace fTrace;
    private int size = 0;

    private final String INDEX_FILE_NAME = "index.ht"; //$NON-NLS-1$
    private int version = 2;

    /**
     *
     */
    public static final int VERSION_NUMBER_OFFSET = Database.DATA_AREA;
    /**
     *
     */
    public static final int FIRST_CHECKPOINT_OFFSET = VERSION_NUMBER_OFFSET + 4;

//    public static final int CHECKPOINT_TREE_OFFSET = FIRST_CHECKPOINT_OFFSET + 4;
//    public static final int CHECKPOINT_RANK_TREE_OFFSET = CHECKPOINT_TREE_OFFSET + 4;

    /**
     * @param trace
     *
     */
    public TmfPersistentIndex(ITmfTrace trace) {
        if (sDEBUG_LOCKS) {
            fLockDebugging= new HashMap<Thread, DebugLockInfo>();
            System.out.println("Debugging PDOM Locks"); //$NON-NLS-1$
        }
        fTrace = trace;
        createDatabase();
    }

    /**
     * @author emalape
     *
     */
    public static class CheckpointBTreeComparator implements IBTreeComparator {
        final private Database db;
        final private ITmfTrace fTrace;

        /**
         * @param database
         * @param trace
         */
        public CheckpointBTreeComparator(Database database, ITmfTrace trace) {
            db= database;
            fTrace = trace;
        }
        @Override
        public int compare(long record1, long record2) throws CoreException {
            ITmfCheckpoint aCheckpoint1 = fTrace.restoreCheckPoint(db, record1);
            ITmfCheckpoint aCheckpoint2 = fTrace.restoreCheckPoint(db, record2);
            return aCheckpoint1.compareTo(aCheckpoint2);
        }
    }

    private void createDatabase() {
        try {
            File indexFile = getIndexFile();
//            if (!indexFile.exists()) {
//                indexFile.createNewFile();
//            }
            fDatabase = new Database(indexFile, new ChunkCache(), version, false);
            if (fDatabase.getVersion() != version) {
                fDatabase.clear(version);
            } else {
                try {
                    int i = 0;
                    try {
                        acquireReadLock();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    long rec = fDatabase.getRecPtr(FIRST_CHECKPOINT_OFFSET);
                    while (rec != 0) {
                        rec= fDatabase.getRecPtr(rec);
                        ++i;
                    }
                    releaseReadLock();
                    size = i;
//                    if (fCheckpointTree == null) {
//                        fCheckpointTree= new BTree(fDatabase, CHECKPOINT_TREE_OFFSET, new CheckpointBTreeComparator(fDatabase, fTrace));
//                    }
//
//                    if (fCheckpointRankTree == null) {
//                        fCheckpointRankTree= new BTree(fDatabase, CHECKPOINT_RANK_TREE_OFFSET, new CheckpointBTreeComparator(fDatabase, fTrace));
//                    }
                } catch (CoreException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        } catch (CoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private File getIndexFile() {
        String directory = TmfTraceManager.getSupplementaryFileDir(fTrace);
        return new File(directory + INDEX_FILE_NAME);
    }

    @Override
    public void dispose() {
        try {
            acquireWriteLock();
            fDatabase.close();
        } catch (CoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        releaseWriteLock();
    }

    private void addToList(final long listRecord, long record) throws CoreException {
        final Database db= fDatabase;
        final long nextRec= db.getRecPtr(listRecord);
        db.putRecPtr(record + 0, nextRec);
        db.putRecPtr(listRecord, record);
    }

    @Override
    public void add(ITmfCheckpoint checkpoint) {
        try {
            acquireWriteLock();
            checkpoint.setRank(size);
            long record = checkpoint.serialize(fDatabase);
            // fCheckpointTree.insert(record);
            addToList(FIRST_CHECKPOINT_OFFSET, record);
            size++;
        } catch (CoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // fCheckpoints.add(checkpoint);
        catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        releaseWriteLock();
    }

    @Override
    public ITmfCheckpoint get(int checkpoint) {
        try {
            acquireReadLock();
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        System.out.println("searching for no: " + checkpoint);
        CheckPointRankTreeVisitor visitor = new CheckPointRankTreeVisitor(checkpoint, fDatabase, fTrace);
//        try {
////            fCheckpointRankTree.accept(visitor);
//        } catch (CoreException e1) {
//            // TODO Auto-generated catch block
//            e1.printStackTrace();
//        }

        if (visitor.isFound()) {
            System.out.println("found no "+ checkpoint + "!!");
            releaseReadLock();
            return visitor.getCheckpoint();
        }


        try {
            long rec= fDatabase.getRecPtr(FIRST_CHECKPOINT_OFFSET);
            int i = 0;
            while (rec != 0 && i != checkpoint) {
                rec= fDatabase.getRecPtr(rec);
                ++i;
            }

//            fCheckpointRankTree.insert(rec);
            ITmfCheckpoint restoredCheckPoint = fTrace.restoreCheckPoint(fDatabase, rec);
            releaseReadLock();
            return restoredCheckPoint;

        } catch (CoreException e) {
//            CCorePlugin.log(e);
        }

        releaseReadLock();
        return null;

       //fDatabase.get
        //return fCheckpoints.get(checkpoint);
    }

    class CheckPointTreeVisitor implements IBTreeVisitor {

        ITmfCheckpoint fCheckpointSearch;
        ITmfCheckpoint fCheckpointResult;
        final private Database fDb;
        final private ITmfTrace fTrace;
        private boolean fFound = false;

        public CheckPointTreeVisitor(ITmfCheckpoint checkpoint, Database db, ITmfTrace trace) {
            fCheckpointSearch = checkpoint;
            this.fDb = db;
            fTrace = trace;
        }

        @Override
        public int compare(long record) throws CoreException {
            ITmfCheckpoint restoreCheckPoint = fTrace.restoreCheckPoint(fDb, record);
            return restoreCheckPoint.compareTo(fCheckpointSearch);
        }

        @Override
        public boolean visit(long record) throws CoreException {
            if (record == 0) {
                return true;
            }

            fFound = true;
            fCheckpointResult = fTrace.restoreCheckPoint(fDb, record);
            return false;
        }

        boolean isFound() {
            return fFound;
        }

        ITmfCheckpoint getCheckpoint() {
            return fCheckpointResult;
        }

    }

    class CheckPointRankTreeVisitor implements IBTreeVisitor {

        ITmfCheckpoint fCheckpointResult;
        int fRank;
        final private Database fDb;
        final private ITmfTrace fTrace;
        private boolean fFound = false;

        public CheckPointRankTreeVisitor(int rank, Database db, ITmfTrace trace) {
            fRank = rank;
            this.fDb = db;
            fTrace = trace;
        }

        @Override
        public int compare(long record) throws CoreException {
            ITmfCheckpoint restoreCheckPoint = fTrace.restoreCheckPoint(fDb, record);
            return Integer.valueOf(restoreCheckPoint.getRank()).compareTo(Integer.valueOf(fRank));
        }

        @Override
        public boolean visit(long record) throws CoreException {
            if (record == 0) {
                return true;
            }

            fFound = true;
            fCheckpointResult = fTrace.restoreCheckPoint(fDb, record);
            return false;
        }

        boolean isFound() {
            return fFound;
        }

        ITmfCheckpoint getCheckpoint() {
            return fCheckpointResult;
        }

    }

    @Override
    public int binarySearch(ITmfCheckpoint checkpoint) {
        try {
            acquireReadLock();
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        System.out.println("searching for: " + checkpoint);
        CheckPointTreeVisitor visitor = new CheckPointTreeVisitor(checkpoint, fDatabase, fTrace);
//        try {
////            fCheckpointTree.accept(visitor);
//        } catch (CoreException e1) {
//            // TODO Auto-generated catch block
//            e1.printStackTrace();
//        }

        if (visitor.isFound()) {
            System.out.println("found!!");
        }

        try {
            long rec= fDatabase.getRecPtr(FIRST_CHECKPOINT_OFFSET);
            int i = 0;
            while (rec != 0) {
                ITmfCheckpoint aCheckpoint = fTrace.restoreCheckPoint(fDatabase, rec);
                if (aCheckpoint.compareTo(checkpoint) == 0) {
//                    fCheckpointTree.insert(rec);
                    releaseReadLock();
                    return i;
                }
                rec= fDatabase.getRecPtr(rec);
            }
        } catch (CoreException e) {
            e.printStackTrace();
        }

        releaseReadLock();

        return -1;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public int size() {
        return size;
    }

    // Read-write lock rules. Readers don't conflict with other readers,
    // Writers conflict with readers, and everyone conflicts with writers.
    private final Object mutex = new Object();
    private int lockCount;
    private int waitingReaders;
    private long lastWriteAccess= 0;
    private long lastReadAccess= 0;
    private long timeWriteLockAcquired;

    public void acquireReadLock() throws InterruptedException {
        long t = sDEBUG_LOCKS ? System.nanoTime() : 0;
        synchronized (mutex) {
            ++waitingReaders;
            try {
                while (lockCount < 0) {
                    mutex.wait();
                }
            } finally {
                --waitingReaders;
            }
            ++lockCount;
            fDatabase.setLocked(true);

            if (sDEBUG_LOCKS) {
                t = (System.nanoTime() - t) / 1000000;
                if (t >= LONG_READ_LOCK_WAIT_REPORT_THRESHOLD) {
                    System.out.println("Acquired index read lock after " + t + " ms wait."); //$NON-NLS-1$//$NON-NLS-2$
                }
                incReadLock(fLockDebugging);
            }
        }
    }

    public void releaseReadLock() {
        boolean clearCache= false;
        synchronized (mutex) {
            assert lockCount > 0: "No lock to release"; //$NON-NLS-1$
            if (sDEBUG_LOCKS) {
                decReadLock(fLockDebugging);
            }

            lastReadAccess= System.currentTimeMillis();
            if (lockCount > 0) {
                --lockCount;
            }
            mutex.notifyAll();
            clearCache= lockCount == 0;
            fDatabase.setLocked(lockCount != 0);
        }
    }

    /**
     * Acquire a write lock on this PDOM. Blocks until any existing read/write locks are released.
     * @throws InterruptedException
     * @throws IllegalStateException if this PDOM is not writable
     */
    public void acquireWriteLock() throws InterruptedException {
        acquireWriteLock(0);
    }

    // For debugging lock issues
    @SuppressWarnings("nls")
    private long reportBlockedWriteLock(long start, int giveupReadLocks) {
        long now= System.currentTimeMillis();
        if (now >= start + BLOCKED_WRITE_LOCK_OUTPUT_INTERVAL) {
            System.out.println();
            System.out.println("Blocked writeLock");
            System.out.println("  lockcount= " + lockCount + ", giveupReadLocks=" + giveupReadLocks + ", waitingReaders=" + waitingReaders);
            outputReadLocks(fLockDebugging);
            start= now;
        }
        return start;
    }

    /**
     * Acquire a write lock on this PDOM, giving up the specified number of read locks first. Blocks
     * until any existing read/write locks are released.
     * @throws InterruptedException
     * @throws IllegalStateException if this PDOM is not writable
     */
    public void acquireWriteLock(int giveupReadLocks) throws InterruptedException {
//        assert !isPermanentlyReadOnly();
        synchronized (mutex) {
            if (sDEBUG_LOCKS) {
                incWriteLock(giveupReadLocks);
            }

            if (giveupReadLocks > 0) {
                // give up on read locks
                assert lockCount >= giveupReadLocks: "Not enough locks to release"; //$NON-NLS-1$
                if (lockCount < giveupReadLocks) {
                    giveupReadLocks= lockCount;
                }
            } else {
                giveupReadLocks= 0;
            }

            // Let the readers go first
            long start= sDEBUG_LOCKS ? System.currentTimeMillis() : 0;
            while (lockCount > giveupReadLocks || waitingReaders > 0) {
                mutex.wait(BLOCKED_WRITE_LOCK_OUTPUT_INTERVAL);
                if (sDEBUG_LOCKS) {
                    start = reportBlockedWriteLock(start, giveupReadLocks);
                }
            }
            lockCount= -1;
            if (sDEBUG_LOCKS) {
                timeWriteLockAcquired = System.currentTimeMillis();
            }
            fDatabase.setExclusiveLock();
        }
    }

    final public void releaseWriteLock() {
        releaseWriteLock(0, true);
    }

    @SuppressWarnings("nls")
    public void releaseWriteLock(int establishReadLocks, boolean flush) {
        try {
            fDatabase.giveUpExclusiveLock(flush);
        } catch (CoreException e) {
//            CCorePlugin.log(e);
        }
        assert lockCount == -1;
//        if (!fEvent.isTrivial()) {
//            lastWriteAccess= System.currentTimeMillis();
//        }
//        final ChangeEvent event= fEvent;
//        fEvent= new ChangeEvent();
        synchronized (mutex) {
            if (sDEBUG_LOCKS) {
                long timeHeld = lastWriteAccess - timeWriteLockAcquired;
                if (timeHeld >= LONG_WRITE_LOCK_REPORT_THRESHOLD) {
                    System.out.println("Index write lock held for " + timeHeld + " ms");
                }
                decWriteLock(establishReadLocks);
            }

            if (lockCount < 0) {
                lockCount= establishReadLocks;
            }
            mutex.notifyAll();
            fDatabase.setLocked(lockCount != 0);
        }
//        fireChange(event);
    }

    // For debugging lock issues
    static class DebugLockInfo {
        int fReadLocks;
        int fWriteLocks;
        List<StackTraceElement[]> fTraces= new ArrayList<StackTraceElement[]>();

        public int addTrace() {
            fTraces.add(Thread.currentThread().getStackTrace());
            return fTraces.size();
        }
        @SuppressWarnings("nls")
        public void write(String threadName) {
            System.out.println("Thread: '" + threadName + "': " + fReadLocks + " readlocks, " + fWriteLocks + " writelocks");
            for (StackTraceElement[] trace : fTraces) {
                System.out.println("  Stacktrace:");
                for (StackTraceElement ste : trace) {
                    System.out.println("    " + ste);
                }
            }
        }
        public void inc(DebugLockInfo val) {
            fReadLocks+= val.fReadLocks;
            fWriteLocks+= val.fWriteLocks;
            fTraces.addAll(val.fTraces);
        }
    }

    // For debugging lock issues
    private Map<Thread, DebugLockInfo> fLockDebugging;

    // For debugging lock issues
    private static DebugLockInfo getLockInfo(Map<Thread, DebugLockInfo> lockDebugging) {
        assert sDEBUG_LOCKS;

        Thread key = Thread.currentThread();
        DebugLockInfo result= lockDebugging.get(key);
        if (result == null) {
            result= new DebugLockInfo();
            lockDebugging.put(key, result);
        }
        return result;
    }

    // For debugging lock issues
    @SuppressWarnings("nls")
    private static void outputReadLocks(Map<Thread, DebugLockInfo> lockDebugging) {
        System.out.println("---------------------  Lock Debugging -------------------------");
        for (Thread th: lockDebugging.keySet()) {
            DebugLockInfo info = lockDebugging.get(th);
            info.write(th.getName());
        }
        System.out.println("---------------------------------------------------------------");
    }

    // For debugging lock issues
    public void adjustThreadForReadLock(Map<Thread, DebugLockInfo> lockDebugging) {
        for (Thread th : lockDebugging.keySet()) {
            DebugLockInfo val= lockDebugging.get(th);
            if (val.fReadLocks > 0) {
                DebugLockInfo myval= fLockDebugging.get(th);
                if (myval == null) {
                    myval= new DebugLockInfo();
                    fLockDebugging.put(th, myval);
                }
                myval.inc(val);
                for (int i = 0; i < val.fReadLocks; i++) {
                    decReadLock(fLockDebugging);
                }
            }
        }
    }

    // For debugging lock issues
    static void incReadLock(Map<Thread, DebugLockInfo> lockDebugging) {
        DebugLockInfo info = getLockInfo(lockDebugging);
        info.fReadLocks++;
        if (info.addTrace() > 10) {
            outputReadLocks(lockDebugging);
        }
    }

    // For debugging lock issues
    @SuppressWarnings("nls")
    static void decReadLock(Map<Thread, DebugLockInfo> lockDebugging) throws AssertionError {
        DebugLockInfo info = getLockInfo(lockDebugging);
        if (info.fReadLocks <= 0) {
            outputReadLocks(lockDebugging);
            throw new AssertionError("Superfluous releaseReadLock");
        }
        if (info.fWriteLocks != 0) {
            outputReadLocks(lockDebugging);
            throw new AssertionError("Releasing readlock while holding write lock");
        }
        if (--info.fReadLocks == 0) {
            lockDebugging.remove(Thread.currentThread());
        } else {
            info.addTrace();
        }
    }

    // For debugging lock issues
    @SuppressWarnings("nls")
    private void incWriteLock(int giveupReadLocks) throws AssertionError {
        DebugLockInfo info = getLockInfo(fLockDebugging);
        if (info.fReadLocks != giveupReadLocks) {
            outputReadLocks(fLockDebugging);
            throw new AssertionError("write lock with " + giveupReadLocks + " readlocks, expected " + info.fReadLocks);
        }
        if (info.fWriteLocks != 0) {
            throw new AssertionError("Duplicate write lock");
        }
        info.fWriteLocks++;
    }

    // For debugging lock issues
    private void decWriteLock(int establishReadLocks) throws AssertionError {
        DebugLockInfo info = getLockInfo(fLockDebugging);
        if (info.fReadLocks != establishReadLocks)
         {
            throw new AssertionError("release write lock with " + establishReadLocks + " readlocks, expected " + info.fReadLocks); //$NON-NLS-1$ //$NON-NLS-2$
        }
        if (info.fWriteLocks != 1)
         {
            throw new AssertionError("Wrong release write lock"); //$NON-NLS-1$
        }
        info.fWriteLocks= 0;
        if (info.fReadLocks == 0) {
            fLockDebugging.remove(Thread.currentThread());
        }
    }

}
