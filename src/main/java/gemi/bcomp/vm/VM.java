package gemi.bcomp.vm;

import static gemi.bcomp.assembler.Opcodes.*;
import static gemi.bcomp.utilities.Utilities.*;
import static gemi.bcomp.vm.VMResult.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;

import gemi.bcomp.disassembler.Disassembler;

public class VM {
    
    private int[] mem;
    
    public int[] regs = {0, 0, 0, 0};
    
    public static int SP   = 0;
    public static int FP   = 1;
    public static int R0   = 2;
    public static int R1   = 3;
    public static int acc  = R0;
    public static int reg1 = R1;

    // instruction pointer (program counter)
    public int IP = 0;
    // instruction register
    public int IR = 0;
    // opcode from instruction register
    public int opcode = 0;
    
    // the first address not allocated to code or static data
    public int data = 0;
    
    // contains an illegal address that has been accessed
    public Integer illegalAddress = null;
    public boolean tracing = false;
    public PrintStream traceOut = System.err;
    
    private int r, r0, r1;
    private SystemCalls systemCalls;
    
    public VM(int[] mem, int data) {
        this.mem = mem;
        this.data = data;
        systemCalls = new SystemCalls(this);
    }

    public void argv(List<String> argv) {
        systemCalls.argv(argv);
    }
    
    public VMResult run() {
        return run(0);
    }
    
    public VMResult run(int startIP) {
        regs[SP] = mem.length-1;
        regs[FP] = 0;
        regs[R0] = 0;
        regs[R1] = 0;
        IP = startIP;
        illegalAddress = null;
        
        VMResult res = OK;
        while (res == OK && illegalAddress == null) {
            res = execute();
        }

        if (illegalAddress != null)
           return ILLEGAL_MEMORY_ACCESS;
        return res;
    }

    public VMResult execute() {
        IR = mem(IP);
        trace();
        opcode = opcode(IR);
        IP++;
        switch (opcode) {
        case NOP:
            return OK;
        case RET:
            IP = mem(regs[FP]);            
            regs[SP] = regs[FP];
            regs[SP]++;
            regs[FP] = mem(regs[SP]);
            return OK;
        case HALT:
            return HALTED;
        case CALL_R:
            mem(regs[SP], regs[FP]);
            regs[SP]--;
            regs[FP] = regs[SP];
            mem(regs[SP], IP);
            regs[SP]--;
            IP = regs[rb(IR)];
            return OK;
        case CALL_A:
            mem(regs[SP], regs[FP]);
            regs[SP]--;
            regs[FP] = regs[SP];
            mem(regs[SP], IP);
            regs[SP]--;
            IP = adr(IR);
            return OK;
        case JMP_R:
            IP = regs[rb(IR)];
            return OK;
        case JMP_A:
            IP = adr(IR);
            return OK;
        case LAD_A0:
            regs[R0] = adr(IR);
            return OK;
        case MOV_A0:
            regs[R0] = mem(adr(IR));
            return OK;
        case MOV_0A:
            mem(adr(IR), regs[R0]);
            return OK;
        case ADD:
            r = rb(IR);
            regs[r] = regs[r] + sval(IR);
            return OK;
        case SUB:
            r = rb(IR);
            regs[r] = regs[r] - sval(IR);
            return OK;
        case MUL:
            r = rb(IR);
            regs[r] = regs[r] * sval(IR);
            return OK;
        case DIV:
            r = rb(IR);
            if (sval(IR) == 0) return DIVISION_BY_ZERO;
            regs[r] = regs[r] / sval(IR);
            return OK;
        case MOD:
            r = rb(IR);
            if (sval(IR) == 0) return DIVISION_BY_ZERO;
            regs[r] = regs[r] % sval(IR);
            return OK;
        case NEG:
            regs[rb(IR)] = -regs[ra(IR)];
            return OK;
        case AND:
            r = rb(IR);
            regs[r] = regs[r] & sval(IR);
            return OK;
        case OR:
            r = rb(IR);
            regs[r] = regs[r] | sval(IR);
            return OK;
        case XOR:
            r = rb(IR);
            regs[r] = regs[r] ^ sval(IR);
            return OK;
        case NOT:
            regs[rb(IR)] = regs[ra(IR)] == 0?1:0;
            return OK;
        case CPL:
            regs[rb(IR)] = ~regs[ra(IR)];
            return OK;
        case LSH:
            r = rb(IR);
            r0 = sval(IR);
            if (r0 > 0)
                regs[r] = regs[r] << r0;
            else
                regs[r] = regs[r] >> -r0;
            return OK;
        case RSH:
            r = rb(IR);
            r0 = sval(IR);
            if (r0 > 0)
                regs[r] = regs[r] >> r0;
            else
                regs[r] = regs[r] << -r0;
            return OK;
        case EXCH:
            r0 = rb(IR);
            r1 = ra(IR);
            r = regs[r0];
            regs[r0] = regs[r1];
            regs[r1] = r;
            return OK;
        case POP:
            regs[SP]++;
            regs[rb(IR)] = mem(regs[SP]);
            return OK;
        case PUSH:
            mem(regs[SP], regs[rb(IR)]);
            regs[SP]--;
            return OK;
        case MOV_RR:
            regs[rb(IR)] = regs[ra(IR)];
            return OK;
        case MOV_L0:
            regs[R0] = offset(IR);
            return OK;
        case MOV_L1:
            regs[R1] = offset(IR);
            return OK;
        case MOV_RI:
            r0 = (IR >> 20) & 0b11; // rb
            r1 = (IR >> 22) & 0b11; // ra
            mem(regs[r0] + lit(IR), regs[r1]);
            return OK;
        case MOV_IR:
            r0 = (IR >> 20) & 0b11; // rb
            r1 = (IR >> 22) & 0b11; // ra
            regs[r0] = mem(regs[r1] + lit(IR));
            return OK;
        case MOV_OR:
            r0 = (IR >> 20) & 0b11; // rb
            r1 = (IR >> 22) & 0b11; // ra
            regs[r0] = regs[r1] + lit(IR);
            return OK;
        case BNE:
            if (regs[R0] == 0) IP += offset(IR);
            return OK;
        case BEQ:
            if (regs[R0] != 0) IP += offset(IR);
            return OK;
        case BNG:
            if (regs[R0] < 0) IP += offset(IR);
            return OK;
        case BPS:
            if (regs[R0] > 0) IP += offset(IR);
            return OK;
        case JRL:
            IP += offset(IR);
            return OK;
        case SYS_R:
            return systemCalls.syscall(rb(IR));
        case SYS_L:
            return systemCalls.syscall(lit(IR));
        default:
            return ILLEGAL_INSTRUCTION;
        }
    }
    
    /**
     * Start execution at <code>startIP</code> and stop.
     * Proceed using <code>step</code>.
     */
    public void start(int startIP) {
        regs[SP] = mem.length-1;
        regs[FP] = 0;
        regs[R0] = 0;
        regs[R1] = 0;
        illegalAddress = null;
        IP = startIP;
    }
    
    public VMResult step() {
        VMResult res = execute();
        if (illegalAddress != null)
            return ILLEGAL_MEMORY_ACCESS;
        else
            return res;
    }
    
    public final void trace() {
        if (tracing) {
            traceOut.println();
            dumpRegs(traceOut);
            dumpStack(traceOut);
            traceOut.println();
            traceOut.println(int2hex(IP)+": "+Disassembler.disassemble(IR));
        }
    }
    
    public final void dumpRegs(PrintStream out) {
        out.print("Registers:\n");
        out.println("  SP: "+int2hex(regs[SP]));
        out.println("  FP: "+int2hex(regs[FP]));
        out.println("  R0: "+int2hex(regs[R0]));
        out.println("  R1: "+int2hex(regs[R1]));
    }
    
    public final void dumpStack(PrintStream out) {
        out.print("Stack:\n");
        for (int i = mem.length-1; i >= regs[SP]; i--) {
            if (i == regs[SP]) 
                out.println("  "+int2hex(mem[i])+" <- SP");
            else if (i == regs[FP])
                out.println("  "+int2hex(mem[i])+" <- FP");
            else
                out.println("  "+int2hex(mem[i]));
        }
    }

    public final int mem(int adr) {
        if (adr < mem.length)
            return mem[adr];
        illegalAddress = adr;
        return 0;
    }
    
    public final void mem(int adr, int value) {
        if (adr >= 0 && adr < mem.length)
            mem[adr] = value;
        else
            illegalAddress = adr;
    }

    private final int sval(int instr) {
        if (r2r(instr))
            return regs[ra(instr)];
        else
            return lit(instr);
    }
    
    public static void error(String msg) {
        System.err.println("b-vm: error: "+msg);
        System.exit(1);
    }
    
    public static void main(String[] args) {
        String exefilename = null;
        List<String> argv = new LinkedList<>();
        int memsize = 1024*1024;
        int data = 0;
        int[] mem = null;
        int n = 0;
        boolean tracing = false;

        for (int i = 0; i < args.length; i++) {
            if ("-t".equals(args[i])) {
                tracing = true;
            }
            else if (exefilename == null) {
                exefilename = args[i];
            }
            else {
                argv.add(args[i]);
            }
        }
        
        if (exefilename == null) {
            error("no executable file");
            System.exit(1);
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(exefilename))) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#")) {
                    continue;
                }
                else if (line.startsWith(".memsize ")) {
                    int s = hex2int(line.substring(".memsize ".length()));
                    if (s < 1)
                        error("memsize must be positive");
                    else
                        memsize = s;
                }
                else if (line.startsWith(".data ")) {
                    int s = hex2int(line.substring(".data ".length()));
                    if (s < 1)
                        error("data must be positive");
                    else
                        data = s;
                }
                else if (line.length() == 8) {
                    if (mem == null) mem = new int[memsize];
                    mem[n] = hex2int(line);
                    n++;
                }
            }
            VM vm = new VM(mem, data);
            vm.argv(argv);
            vm.tracing = tracing;
            VMResult result = vm.run();
            switch (result) {
            case HALTED:
                System.exit(vm.regs[R0]);
            case DIVISION_BY_ZERO:
                System.err.println("Fatal: division by zero at "+int2hex(vm.IP-1));
                System.exit(1);
                break;
            case ILLEGAL_INSTRUCTION:
                System.err.println("Fatal: illegal instruction at "+int2hex(vm.IP-1));
                System.exit(1);
                break;
            case ILLEGAL_MEMORY_ACCESS:
                System.err.println("Fatal: illegal memory access at "+int2hex(vm.IP-1)+": "+int2hex(vm.illegalAddress));
                System.exit(1);
                break;
            case NOT_YET_IMPLEMENTED:
                System.err.println(result);
                System.exit(1);
                break;
            case UNDEFINED_SYSTEM_CALL:
                System.err.println("Fatal: undefined system call at "+int2hex(vm.IP-1));
                System.exit(1);
                break;
            case UNSUPPORTED_SYSTEM_CALL:
                System.err.println("Fatal: unsupported system call at "+int2hex(vm.IP-1));
                System.exit(1);
                break;
            case OK:
                // should never be the case
                System.exit(0);
                break;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            error("cannot read executable file "+exefilename);
        }
    }
}
