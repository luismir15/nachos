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
     * VERIFY
     * 
     * Initializes page tables for this process so that the executable can be
     * demand-paged.
     *
     * @return	<tt>true</tt> if successful.
     */
    protected boolean loadSections() {

        int vpn = 0;

        for (int index = 0; index < coff.getNumSection(); index++) {

            CoffSection section = coff.getSection(index);

            CoffConstructor constructor;

            vpn += section.getLength();

            for (int i = section.getFirstVPN(); i < vpn; i++) {

                constructor = new CoffConstructor(section, i);

                thunk.put(i, constructor);
            }

            for (; vpn < numPages - 1; vpn++) {

                thunk.put(vpn, new StackConstructor(vpn));
            }
        }

	    return true;
    }

   @Override
	protected void unloadSections() {
		kernel.freePages(PID, numPages);
	}

	@Override
	protected void loadArguments(int entryOffset, int stringOffset, byte[][] argv) {
		thunkedSections.put(numPages - 1, new ArgConstructor(entryOffset, stringOffset, argv));
	}

     * @param	cause	the user exception that occurred.
     */
    public void handleException(int cause) {
	    Processor processor = Machine.processor();

	    switch (cause) {
        
            case Processor.exceptionTLBMiss:
                handleTLBMiss(processor.readRegister(processor.regBadVAddr));
                break;
            
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

        TranslationEntry retrieve = retrievePage(Processor.pageFromAddress(vaddr));

        boolean unwritten = true;

        Processor process = Machine.processor();

        for (int index = 0; i < process.getTLBSize() && unwritten; index++) {

            TranslationEntry tlb = process.readTLBEntry(index);

            if (tlb.ppn == retrive.ppn) {

                if (unwritten) {

                    process.writeTLBEntry(index, retrive);
                    unwritten = false;
                }

                else if (tlb.valid) {

                    tlb.valid = false;
                    
                    process.writeTLBEntry(index, tlb);
                }
            }

            else if (unwritten && !tlb.valid) {

                process.writeTLBEntry(index, retrieve);

                unwritten = false;
            }
        }

        if (unwritten) {

            Random random = new Random();

            int tlbIndex = random.nextInt(process.getTLBSize());

            TranslationEntry entry = process.readTLBEntry(tlbIndex);

            if (entry.dirty || entry.used) {

                kernel.propagateEntry(entry.ppn, entry.used, entry.dirty);
            }

            process.writeTLBEntry(random, retrieve);
        }

        kernel.unpit(retrieve.ppn);
    }

    public TranslationEntry retrieve(int vpn) {

        TranslationEntry entry = null;

        if(thunked.constainsKey(vpn)) {

            entry = thunked.get(vpn).execute();
        }

        else if((entry = kernel.pinIfExists(vpn, PID)) == null) {

            entry = kernel.pageFault(vpn, PID);
        }

        Lib.assertTrue(entry != null);

        return entry;
    }

    public int readVirtualMemory(int address, byte[] data, int offset, int lenght) {

        int bytes = 0;
        LinkedList<VMMemoryAccess> memoryAccesses = create(vaddr, data, offset, length, AccessType.READ, true);

        if (memoryAccesses != null) {

            int temporary;

            for (VMMemoryAccess memory : memoryAccesses) {

                temporary = memory.executeAccess();

                if (temporary == 0) {

                    break;
                }

                else {

                    bytes += temporary;
                }
            }

            return bytes;
        }
    }

    public int writeVirtualMemory(int address, byte[] data, int offset, int length) {

        return writeVirtualMemory(address, data, offset, length, true);
    }

    public int writeVirtualMemory(int address, byte[] data, int offset, int length, boolean unpin) {

        int bytes = 0;
		LinkedList<VMMemoryAccess> memoryAccesses = createMemoryAccesses(vaddr, data, offset, length, AccessType.WRITE, unpin);

		if (memoryAccesses != null) {

            int temporary;
            
			for (VMMemoryAccess memory : memoryAccesses) {

                temporary = memory.executeAccess();
                
				if (temporary == 0) {

                    break;
                }

				else {

                    bytes += temporary;
                }
			}
		}

		return bytes;
    }

    public LinkedList<VMMemoryAccess> create(int vaddr, byte[] data, int offset, int length, AccessType accessType, boolean unpin) {

        Lib.assertTrue(offset >= 0 && length >= 0 && offset + length <= data.length);
        LinkedList<VMMemoryAccess> list = null;

        if (validAddress(vaddr)) {

            list = new LinkedList<VMMemoryAccess>();

            while (length > 0) {

                int vpn = Processor.pageFromAddress(vaddr);
                int page = Processor.pageSize - Processor.offsetFromAddress(vaddr);
                int access = length < page ? length : page;

                list.add(new VMMemoryAccess(accessType, data, vpn, offset, Processor.offsetFromAddress(vaddr), accessType, unpin));
                length -= access;
                vaddr += access;
                offset += access;
            }
        }

        return list;
    }

    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
    private static final char dbgVM = 'v';
    public static VMKernel kernel = null;
    protected int PID;
    protected int numPages;
    public HashMap<Integer, Constructor> thunked = new HashMap<Integer, Constructor>();

    protected class VMMemoryAccess extends UserProcess.MemoryAccess {

		VMMemoryAccess(AccessType access, byte[] d, int vpn, int start, int seconStart, int length, boolean _unpin) {

            super(access, d, vpn, start, secondStart, length);
            
			unpin = _unpin;
        }
        
		public int executeAccess() {
			
			translationEntry = retrievePage(vpn);

			int bytes = super.executeAccess();

			if (unpin) {

                kernel.unpin(translationEntry.ppn);
            }

			return bytes;
		}

		public boolean unpin;
	}
}

public abstract class Constructor {
		abstract TranslationEntry execute();
	}

	public class CoffigConstructor extends Constructor {
		CoffigConstructor(CoffSection sec, int vpn1) {
			coffSection = sec;
			vpn = vpn1;
		}

		@Override
		TranslationEntry execute() {
			//Also, remove yourself from all the thunked section mappings you're in
			int sectionNumber = vpn - coffSection.getFirstVPN();
			Lib.assertTrue(thunkedSections.remove(vpn) != null);
			
			//Get a free page
			TranslationEntry returnEntry = kernel.requestFreePage(vpn, PID);
			coffSection.loadPage(sectionNumber, returnEntry.ppn);
			
			returnEntry.readOnly = coffSection.isReadOnly() ? true : false;

			return returnEntry;
		}

		public CoffSection coffSection;
		public int vpn;
	}

	public class StackConstructor extends Constructor {
		StackConstructor(int vpn1) {
			vpn = vpn1;
		}

		@Override
		TranslationEntry execute() {
			//Remove yourself from the mapping
			Lib.assertTrue(thunkedSections.remove(vpn) != null);

			TranslationEntry te = kernel.requestFreePage(vpn, PID);
			te.readOnly = false;
			return te;
		}

		public int vpn;
	}

	public class ArgConstructor extends Constructor {
		ArgConstructor(int _entryOffset, int _stringOffset, byte[][] _argv) {
			entryOffset = _entryOffset; stringOffset = _stringOffset; argv = _argv;
		}

		@Override
		TranslationEntry execute() {
			Lib.assertTrue(thunkedSections.remove(numPages - 1) != null);

			TranslationEntry te = kernel.requestFreePage(numPages - 1, PID);//get a free page

			//The page is pinned, so just use writeVM to load in the info
			for (int i = 0; i < argv.length; i++) {
				byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
				Lib.assertTrue(writeVirtualMemory(entryOffset, stringOffsetBytes, false) == 4);
				entryOffset += 4;
				Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i], false) == argv[i].length);
				stringOffset += argv[i].length;
				Lib.assertTrue(writeVirtualMemory(stringOffset, new byte[] { 0 }, false) == 1);
				stringOffset += 1;
			}
			
			te.readOnly = true;
			
			return te;
		}

		public int entryOffset, stringOffset;
		public byte[][] argv;
	}
}
