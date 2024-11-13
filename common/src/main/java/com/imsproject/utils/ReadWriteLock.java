package com.imsproject.utils;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This lock gives priority to writers. If a writer is waiting, no readers can enter.
 * A writer will block until all readers have left.
 * If no writers are waiting, the amount of readers that can enter is unlimited.
 * @implNote This lock uses a fair semaphore in the write queue to avoid starvation.
 */
public class ReadWriteLock {

    private final AtomicInteger readers;
    private final AtomicInteger waitingToWrite;
    private final Object readerLock;
    private final SpinLock writeQueueLock;
    private final List<Object> writeQueue;

    public ReadWriteLock() {
        readers = new AtomicInteger();
        waitingToWrite = new AtomicInteger();
        readerLock = new Object();
        writeQueueLock = new SpinLock();
        writeQueue = new LinkedList<>();
    }

    public void acquireRead() {
        synchronized (readerLock){
            while(waitingToWrite.get() > 0){
                try{
                    readerLock.wait();
                } catch(InterruptedException ignored){}
            }

            // ==============================================
            // this needs to be inside the synchronized block
            // because writers check the number of readers
            // inside the synchronized(entryLock) block
            // before proceeding to the write queue
            increment(readers);
            // ==============================================
        }
    }

    public void releaseRead() {
        synchronized(readerLock){
            decrement(readers); // this needs to be inside the synchronized block
            if(readers.get() == 0){
                readerLock.notifyAll();
            }
        }
    }

    public void acquireWrite(){
        increment(waitingToWrite);

        // wait for the busy wait lock
        writeQueueLock.acquire();

        // create a new object to wait on and add it to the queue
        Object obj = new Object();
        writeQueue.add(obj);
        writeQueueLock.release(); // release the busy wait lock

        // wait for all readers to leave
        synchronized(readerLock){
            while(readers.get() > 0){
                try{
                    readerLock.wait();
                } catch(InterruptedException ignored){}
            }
        }

        // wait for your turn
        writeQueueLock.acquire();
        if(writeQueue.get(0) != obj){
            // you are not next in line
            synchronized (obj){
                writeQueueLock.release();
                try{
                    obj.wait();
                } catch(InterruptedException ignored){}
            }
        }else {
            // you are next in line
            writeQueueLock.release();
        }
    }

    public void releaseWrite(){
        decrement(waitingToWrite);

        writeQueueLock.acquire();
        writeQueue.remove(0);

        if(waitingToWrite.get() == 0){
            // no one is waiting to write
            writeQueueLock.release();
            synchronized(readerLock){
                readerLock.notifyAll();
            }
        } else if(!writeQueue.isEmpty()) {
            // waitingToWrite > 0 && writeQueue.size() > 0
            Object next = writeQueue.get(0);
            writeQueueLock.release();
            synchronized(next){
                next.notify();
            }
        } else {
            // waitingToWrite > 0 && writeQueue.size() == 0
            // this means that the write queue is empty
            // but a writer is waiting on the write queue lock
            writeQueueLock.release();
        }
    }

    private void increment(AtomicInteger num){
        int curr;
        do{
            curr = num.get();
        } while(!num.compareAndSet(curr, curr + 1));
    }

    private void decrement(AtomicInteger num){
        int curr;
        do{
            curr = num.get();
        } while(!num.compareAndSet(curr, curr - 1));
    }

    private static class SpinLock {

        private final AtomicBoolean locked;

        public SpinLock(){
            locked = new AtomicBoolean(false);
        }

        /**
         * Acquire the lock non-blocking. will spin until the lock is acquired
         */
        public void acquire(){
            while(!locked.compareAndSet(false,true)){
                Thread.onSpinWait();
            }
        }

        public void release(){
            locked.set(false);
        }
    }
}
