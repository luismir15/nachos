package nachos.threads;

import nachos.machine.*;

import java.util.Stack;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {

    private Stack<Integer> wordStack = new Stack<Integer>();
    private Condition speakReady;
	private Condition listenReady;
	private int speaking = 0, listening = 0;
	private boolean messenger = false;
	private Lock lock;

    /**
     * Allocate a new communicator.
     */
    public Communicator() {
    	lock = new Lock();
    	speakReady = new Condition(lock);
    	listenReady = new Condition(lock);
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {
        lock.acquire();
        speaking++;
        wordStack.push(word);
        if (listening == 0) {
            speakReady.sleep();
        }
        else {
            listening--;
            messenger = wordStack.pop();
            listenReady.wake();
        }
        lock.release();
    	/*lock.acquire();
    	//speaking++;
    	while (listening == 0 || messenger) {
    		speakReady.sleep();
    	}
    	//listening--;
    	messenger = false;
    	speaking = word;

    	listenReady.wake();
    	lock.release();*/
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */
    public int listen() {
        lock.acquire();
        listening++;
        speakReady.wake();
        if(speaking == 0) {
            listenReady.sleep();
            int result = messenger;
            speaking--;
        }
        else{
            speaking--;
            int result = messenger;
        }
        lock.release();
        return result;
    	/*lock.acquire();

    	listening++;
    	speakReady.wake();
    	listenReady.sleep();

    	int result = speaking;
    	messenger = false;
    	listening--;
    	//int result = speaking;

    	speakReady.wake();
    	lock.release();

    	return result;*/
    }
}
