package nachos.threads;

import nachos.machine.*;
import java.util.TreeMap;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
	
	private TreeMap<Long, KThread> waitingThreads;
	
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
    	waitingThreads = new TreeMap<Long, KThread>();
    	Machine.timer().setInterruptHandler(new Runnable() {
    		public void run() { timerInterrupt(); }
	    });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {
    	boolean status = Machine.interrupt().disable();
    	
    	long time = Machine.timer().getTime();
    	
    	while (!waitingThreads.isEmpty() && waitingThreads.firstKey() <= time) {
    		waitingThreads.pollFirstEntry().getValue().ready();
    	}
    	
    	Machine.interrupt().restore(status);
    	
    	KThread.currentThread().yield();
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
    	// for now, cheat just to get something working (busy waiting is bad)
    	boolean status = Machine.interrupt().disable();
    	
    	waitingThreads.put(Machine.timer().getTime() + x, KThread.currentThread());
    	
    	KThread.sleep();
    	
    	Machine.interrupt().restore(status);
    }
}
