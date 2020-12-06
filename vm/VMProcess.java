package nachos.vm;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

/**
 * A <tt>UserProcess</tt> that supports demand-paging.
 */
public class VMProcess extends UserProcess {
    /**
     * Allocate a new process.
     */
    public VMProcess() {
	    super();
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
	    super.saveState();
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
	    super.restoreState();
    }

    /**
     * Initializes page tables for this process so that the executable can be
     * demand-paged.
     *
     * @return	<tt>true</tt> if successful.
     */
    protected boolean loadSections() {
	    return super.loadSections();
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
	    super.unloadSections();
    }    

    /**
     * EDIT -->
     * 
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param	cause	the user exception that occurred.
     */
    public void handleException(int cause) {
	    Processor processor = Machine.processor();

	    switch (cause) {
	    default:
	        super.handleException(cause);
	        break;
	    }
    }

    /**
     * TLB manager to manage add/remove/find TLB entry
     * @param address
     */
    public void handleTLB(int address) {
    }

    public TranslationEntry retrieve(int vpn) {

    }

    public int readVirtualMemory(int address, byte[] data, int offset, int lenght) {

    }

    public int writeVirtualMemory(int address, byte[] data, int offset, int length) {

    }

    public int writeVirtualMemory(int address, byte[] data, boolean unpin) {

    }

    
	
    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
    private static final char dbgVM = 'v';
}
