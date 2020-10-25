package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    private Condition speakReady;
    private Condition listenReady;
    private int speaking = 0, listening = 0;
    private int messenger;
    private Lock lock;
	
    public Communicator() {
    	lock = new Lock();
    	speakReady = new Condition(lock);
    	listenReady = new Condition(lock);
    	
    }
    
    public void speak(int word) {
    	lock.acquire();
    	speaking++;
    	while(listening == 0){
    		listenReady.sleep();
    	}
    	listening--;
    	messenger = word;
    	speakReady.wake();
    	lock.release();
    } 
    public int listen() {
    lock.acquire();
    listening++;
    listenReady.wake();
    while(speaking == 0){
    	speakReady.sleep();
    }
    speaking--;
    int result = messenger;
    lock.release();
	return result;
    }
}
