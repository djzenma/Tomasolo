package MemoryAndBuffer;

import Instruction.Instruction;
import Main.Main;
import Main.Controller;


public class LoadBuffer {

    private int load_size;
    private int store_size;
    private final int LOAD_CAPACITY = 2;
    private final int STORE_CAPACITY = 2;
    private final int MAX_CAPACITY = LOAD_CAPACITY + STORE_CAPACITY;

    private int[] buffer;   // stores the addresses
    private MemoryInterface memInterface;
    private NextCCInterface clkInterface;

    public LoadBuffer(Controller controller) {

        buffer = new int[MAX_CAPACITY];
        for (int i = 0; i < MAX_CAPACITY; i++) {
            buffer[i] = 0;
        }
        load_size = 0;
        store_size = 0;
        memInterface = (MemoryInterface) controller;
        clkInterface = Main.clkH;
    }


    /*
    *   Inserts Instruction in Load Buffer and returns Loaded Data from Memory
    * */
    public boolean insertInstr(Instruction instr, int rob_index) throws Exception {
        if( !freeSlot(instr) ) {
            return false;
        }
        else {
            int index;

            if(instr.getName().equals(Instruction.LW)) {
                index = getFreeLoadSlot(load_size);
                load_size++;
            }
            else {
                index = getFreeStoreSlot(store_size);
                store_size++;
            }

            // Computing Address for Load or Store

            // CC 1: Issuing
            // Starting Computing Address:
            // A = Imm

            buffer[index] = (int) instr.getImm();
            System.out.println("CC: " + Main.CC + " Load Buffer: entry #" + index + ", Address: " + buffer[index]);
            
            // CC 2: A = Imm + Regs[Rs2]
            nextCycle(Main.CC);
            System.out.println("Expected : 2, Found: " + Main.CC);
            try {
                buffer[index] += RegFile.read(instr.getRegB());
            } catch (Exception e) {
                e.printStackTrace();
                throw new Exception("Cannot read from Register File");
            }

            System.out.println("CC: " + Main.CC + " Load Buffer: entry #" + index + ", Address: " + buffer[index]);


            // CC 3: Writing
            nextCycle(Main.CC);
            // Case Load
            if(instr.getName().equals(Instruction.LW)) {
                // Free the slot
                load_size--;
                // Load from Memory
                int loadedValue = memInterface.loadFromMem(buffer[index]);
                // TODO:: Change the -1 to be another error value
                if(loadedValue == -1)
                    return false;
                // Callback once done
                else
                {
                    //System.out.println ("HNNA!!" + rob_index + " "+loadedValue );
                    return memInterface.memoryLoadDone(rob_index, loadedValue);
                }
            }
            // Case Store
            else {
                // Free the slot
                store_size--;
                // Store in Memory
                return memInterface.storeInMem(rob_index, buffer[index], instr.getRegA());
            }
        }
    }

    private int getFreeStoreSlot(int stSize) {
        return stSize + 2;
    }

    private int getFreeLoadSlot(int ldSize) {
        return ldSize;
    }

    private boolean freeSlot(Instruction instr) {
        if( (instr.getName().equals(Instruction.LW) && load_size >= LOAD_CAPACITY) ||
                (instr.getName().equals(Instruction.SW) && store_size >= STORE_CAPACITY))
            return false;
        else
            return true;
    }

    public boolean loadIsFree() {
        return load_size < LOAD_CAPACITY;
    }

    public boolean storeIsFree() {
        return load_size < LOAD_CAPACITY;
    }

    private void nextCycle(int cc) {
        boolean flag= true;
        while(flag) {
            System.out.print("");
            clkInterface.nextCycle(true);
            if (cc < Main.CC) {
                flag = false;
            }
        }
    }

    public interface MemoryInterface {
        // Load Interface
        int loadFromMem(int address);
        boolean memoryLoadDone(int rob_index, int data);

        // Store Interface
        boolean storeInMem(int rob_index, int address, int data);
    }
    
    public interface NextCCInterface {
        void nextCycle(boolean loadEx);
    }
}
