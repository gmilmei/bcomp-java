package gemi.bcomp.assembler;

import static gemi.bcomp.assembler.Arg.ArgType.*;
import static gemi.bcomp.assembler.Opcodes.*;
import static gemi.bcomp.utilities.Utilities.*;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import gemi.bcomp.assembler.Arg.ArgType;
import gemi.bcomp.utilities.ErrorHandler;

public class Assembler {

    public static Map<String,Integer> registers = new HashMap<>();
    public static int SP = 0; 
    public static int FP = 1; 
    public static int R0 = 2; 
    public static int R1 = 3; 

    private ErrorHandler errorHandler;
    private PrintStream out = null;
    private BufferedReader reader;
    private int[] text = new int[0xFFFF];
    private int instrPtr = 0;
    private Set<String> externals = new TreeSet<>();
    private Set<String> exports = new TreeSet<>();
    private Set<String> internals = new TreeSet<>();
    private Map<String,List<Integer>> externalRef = new TreeMap<>();
    private Map<String,List<Integer>> internalRef = new TreeMap<>();
    private Map<String,Integer> labels = new TreeMap<>();
    private Map<String,Integer> data = new TreeMap<>();
    private Map<String,List<Integer>> labelRef = new TreeMap<>();
    private int line = 0;
    
    public Assembler(PrintStream out, ErrorHandler errorHandler) {
        this.out = out;
        this.errorHandler = errorHandler;
    }

    public void assemble(InputStream in) throws Exception {
        reader = new BufferedReader(new InputStreamReader(in));
        String s;
        while ((s = reader.readLine()) != null) {
            assemble(s);
            line++;
        }
        output();
    }
    
    private void output() {
        for (Entry<String,List<Integer>> e : externalRef.entrySet()) {
            out.print(".external "+e.getKey()+":");
            for (int p : e.getValue()) {
                out.print(" ");
                out.print(int2hex(p));
            }
            out.println();
        }
        for (Entry<String,List<Integer>> e : internalRef.entrySet()) {
            out.print(".internal "+e.getKey()+":");
            for (int p : e.getValue()) {
                out.print(" ");
                out.print(int2hex(p));
            }
            out.println();
        }
        for (Entry<String,Integer> e : labels.entrySet()) {
            if (exports.contains(e.getKey())) {
                out.println(".export "+e.getKey()+": "+int2hex(e.getValue()));
            }
        }
        for (Entry<String,Integer> e : data.entrySet()) {
            if (exports.contains(e.getKey()))
                out.println(".export "+e.getKey());
            out.println(".data "+e.getKey()+": "+e.getValue());
        }
        out.println(".text "+instrPtr);
        for (int i = 0; i < instrPtr; i++) {
            out.println(int2hex(text[i]));
        }
    }
    
    private void assemble(String line) {
        if (line.startsWith(".external ")) {
            String e = line.substring(".external ".length()).trim();
            externals.add(e);
        }
        else if (line.startsWith(".export ")) {
            String e = line.substring(".export ".length()).trim();
            exports.add(e);
        }
        else if (line.startsWith(".internal ")) {
            String e = line.substring(".internal ".length()).trim();
            internals.add(e);
        }
        else if (line.startsWith(".data ")) {
            String e = line.substring(".data ".length()).trim();
            String parts[] = e.split("\\s+");
            data.put(parts[0], string2int(parts[1]));
        }
        else if (line.startsWith(".text")) {
            // code
        }
        else if (line.startsWith("//")) {
            // comment
        }
        else if (line.startsWith(" ")) {
            // instruction
            line = line.trim();
            if (line.length() == 0 || line.startsWith("//")) return;
            int i = line.indexOf(" ");
            if (i < 0) {
                instr(line);
            }
            else {
                String op = line.substring(0, i).trim();
                String[] args = line.substring(i+1).trim().split("[ ]*,[ ]*");
                instr(op, args);
            }
        }
        else {
            line = line.trim();
            if (line.endsWith(":")) {
                // label:
                String label = line.substring(0, line.length()-1);
                addLabel(label, instrPtr);
                List<Integer> refs = labelRef.get(label);
                if (refs != null) {
                    for (int ref : refs) {
                        int offset = instrPtr-ref-1;
                        text[ref] = addOffset(text[ref], offset);
                    }
                    refs.clear();
                }
            }
        }
    }
    
    private void instr(String op, String ... args) {
        Arg arg1 = (args.length > 0)?new Arg(args[0]):new Arg();
        Arg arg2 = (args.length > 1)?new Arg(args[1]):new Arg();
        int instr = 0;
        switch (op) {
        case "NOP":            
            if (arg1.type != NOT_SPECIFIED || arg2.type != NOT_SPECIFIED)
                illegalOperands(line, op, arg1, arg2);
            instr = addOpcode(instr, NOP);
            break;
        case "RET":            
            if (arg1.type != NOT_SPECIFIED || arg2.type != NOT_SPECIFIED)
                illegalOperands(line, op, arg1, arg2);
            instr = addOpcode(instr, RET);
            break;
        case "HALT":
            if (arg1.type != NOT_SPECIFIED || arg2.type != NOT_SPECIFIED)
                illegalOperands(line, op, arg1, arg2);
            instr = addOpcode(instr, HALT);
            break;
        case "CALL":
            if (arg1.type == REGISTER) {
                instr = addOpcode(instr, CALL_R);
                instr = addRb(instr, arg1.reg);
            }
            else if (arg1.type == ADDRESS_LABEL) {
                instr = addOpcode(instr, CALL_A);
                addRef(arg1.label, instrPtr);
            }
            else if (arg1.type == ADDRESS_LITERAL) {
                instr = addOpcode(instr, CALL_A);
                instr = addAdr(instr, arg1.literal);
            }
            else {
                illegalOperands(line, op, arg1, arg2);
            }
            break;
        case "JMP":
            if (arg1.type == REGISTER) {
                instr = addOpcode(instr, JMP_R);
                instr = addRb(instr, arg1.reg);
            }
            else if (arg1.type == ADDRESS_LABEL) {
                instr = addOpcode(instr, JMP_A);
                addRef(arg1.label, instrPtr);
            }
            else if (arg1.type == ADDRESS_LITERAL) {
                instr = addOpcode(instr, JMP_A);
                instr = addAdr(instr, arg1.literal);
            }
            else {
                illegalOperands(line, op, arg1, arg2);
            }
            break;
        case "LAD":
            if (arg2.type != REGISTER || arg2.reg != R0) {
                illegalOperands(line, op, arg1, arg2);
            }
            else if (arg1.type == ADDRESS_LABEL) {
                instr = addOpcode(instr, LAD_A0);
                addRef(arg1.label, instrPtr);
            }
            else if (arg1.type == ADDRESS_LITERAL) {
                instr = addOpcode(instr, LAD_A0);
                instr = addAdr(instr, arg1.literal);
            }
            else {
                illegalOperands(line, op, arg1, arg2);
            }
            break;
        case "MOV":
            instr = mov(arg1, arg2);
            break;
        case "ADD":
            instr = addOpcode(instr, ADD);
            instr = rrop(instr, arg1, arg2);
            break;
        case "SUB":
            instr = addOpcode(instr, SUB);
            instr = rrop(instr, arg1, arg2);
            break;
        case "MUL":
            instr = addOpcode(instr, MUL);
            instr = rrop(instr, arg1, arg2);
            break;
        case "DIV":
            instr = addOpcode(instr, DIV);
            instr = rrop(instr, arg1, arg2);
            break;
        case "MOD":
            instr = addOpcode(instr, DIV);
            instr = rrop(instr, arg1, arg2);
            break;
        case "NEG":
            if (arg1.type != REGISTER) {
                illegalOperands(line, op, arg1, arg2);
            }
            else {
                instr = addOpcode(instr, NEG);
                instr = rrop(instr, arg1, arg2);
            }
            break;
        case "AND":
            instr = addOpcode(instr, AND);
            instr = rrop(instr, arg1, arg2);
            break;
        case "OR":
            instr = addOpcode(instr, OR);
            instr = rrop(instr, arg1, arg2);
            break;
        case "XOR":
            instr = addOpcode(instr, XOR);
            instr = rrop(instr, arg1, arg2);
            break;
        case "NOT":
            if (arg1.type != REGISTER) {
                illegalOperands(line, op, arg1, arg2);
            }
            else {
                instr = addOpcode(instr, NOT);
                instr = rrop(instr, arg1, arg2);
            }
            break;
        case "CPL":
            if (arg1.type != REGISTER) {
                illegalOperands(line, op, arg1, arg2);
            }
            else {
                instr = addOpcode(instr, CPL);
                instr = rrop(instr, arg1, arg2);
            }
            break;
        case "LSH":
            instr = addOpcode(instr, LSH);
            instr = rrop(instr, arg1, arg2);
            break;
        case "RSH":
            instr = addOpcode(instr, RSH);
            instr = rrop(instr, arg1, arg2);
            break;
        case "EXCH":
            if (arg1.type != REGISTER) {
                illegalOperands(line, op, arg1, arg2);
            }
            else {
                instr = addOpcode(instr, EXCH);
                instr = rrop(instr, arg1, arg2);
            }
            break;
        case "POP":
            if (arg1.type != REGISTER || arg2.type != NOT_SPECIFIED) {
                illegalOperands(line, op, arg1, arg2);
            }
            else {
                instr = addOpcode(instr, POP);
                instr = addRb(instr, arg1.reg);
            }
            break;
        case "PUSH":
            if (arg1.type != REGISTER || arg2.type != NOT_SPECIFIED) {
                illegalOperands(line, op, arg1, arg2);
            }
            else {
                instr = addOpcode(instr, PUSH);
                instr = addRb(instr, arg1.reg);
            }
            break;
        case "BNE":
            instr = branch(instr, BNE, "BNE", arg1, arg2);
            break;
        case "BEQ":
            instr = branch(instr, BEQ, "BEQ", arg1, arg2);
            break;
        case "BPS":
            instr = branch(instr, BPS, "BPS", arg1, arg2);
            break;
        case "BNG":
            instr = branch(instr, BNG, "BNG", arg1, arg2);
            break;
        case "JRL":
            instr = branch(instr, JRL, "JRL", arg1, arg2);
            break;
        case "SYS":
            if (arg1.type == REGISTER) {
                instr = addOpcode(instr, SYS_L);
                instr = addRb(instr, arg1.reg);
            }
            else if (arg1.type == LITERAL) {
                instr = addOpcode(instr, SYS_L);
                instr = addLit(instr, arg1.literal);
            }
            else {
                illegalOperands(line, op, arg1, arg2);
            }
            break;
        default:
            error(line, "unknown operation "+op);
            return;
        }
        addInstr(instr);
    }
    
    private int branch(int instr, int op, String opname, Arg arg1, Arg arg2) {
        if (arg2.type != NOT_SPECIFIED) {
            illegalOperands(line, opname, arg1, arg2);
        }
        instr = addOpcode(instr, op);
        if (arg1.type == ADDRESS_LABEL) {
            Integer offset = labels.get(arg1.label);
            if (offset != null) {
                instr = addOffset(instr, offset-instrPtr-1);
            }
            else {
                addLabelRef(arg1.label, instrPtr);
            }
        }
        else if (arg1.type == ADDRESS_LITERAL) {
            instr =  addOffset(instr, arg1.literal); 
        }
        else {
            illegalOperands(line, opname, arg1, arg2);
        }
        return instr;
    }
    
    private int mov(Arg arg1, Arg arg2) {
        int instr = 0;
        if (arg1.type == ArgType.LITERAL && arg2.type == ArgType.REGISTER) {
            // MOV #lit,R0
            // MOV #lit,R1
            int lit = arg1.literal;
            if (arg2.reg == R0) {
                instr = addOpcode(instr, MOV_L0);
                instr = addOffset(instr, lit);
            }
            else if (arg2.reg == R1) {
                instr = addOpcode(instr, MOV_L1);
                instr = addOffset(instr, lit);
            }
            else {
                error(line, "instruction MOV "+arg1.arg+","+arg2.arg+" not supported");
            }
        }
        else if (arg1.type == ArgType.REGISTER && arg2.type == ArgType.REGISTER) {
           // MOV ra,rb
            instr = addOpcode(instr, MOV_RR);
            instr = rrop(instr, arg1, arg2);
        }
        else if (arg1.type == ArgType.ADDRESS_LITERAL && arg2.type == ArgType.REGISTER) {
            // MOV adr,R0
            if (arg2.reg != R0) {
                error(line, "instruction MOV "+arg1.arg+","+arg2.arg+" not supported");
            }
            else {
                instr = addOpcode(instr, MOV_A0);
                instr = addAdr(instr, arg1.literal);
            }
            
        }
        else if (arg1.type == ArgType.ADDRESS_LABEL && arg2.type == ArgType.REGISTER) {
            // MOV name,R0
            if (arg2.reg != R0) {
                error(line, "instruction MOV "+arg1.arg+","+arg2.arg+" not supported");
            }
            else {
                instr = addOpcode(instr, MOV_A0);
                addRef(arg1.label, instrPtr);
            }
        }
        else if (arg1.type == ArgType.REGISTER && arg2.type == ArgType.ADDRESS_LITERAL) {
            // MOV R0,adr
            if (arg1.reg != R0) {
                error(line, "instruction MOV "+arg1.arg+","+arg2.arg+" not supported");
            }
            else {
                instr = addOpcode(instr, MOV_0A);
                instr = addAdr(instr, arg2.literal);
            }
            
        }
        else if (arg1.type == ArgType.REGISTER && arg2.type == ArgType.ADDRESS_LABEL) {
            // MOV R0,name
            if (arg1.reg != R0) {
                error(line, "instruction MOV "+arg1.arg+","+arg2.arg+" not supported");
            }
            else {
                instr = addOpcode(instr, MOV_0A);
                addRef(arg2.label, instrPtr);
            }
        }
        else if (arg1.type == ArgType.REGISTER && arg2.type == ArgType.REGISTER_INDIRECT) {
            // MOV ra,[rb+offset]
            instr = addOpcode(instr, MOV_RI);
            instr = instr | (arg1.reg << 22);
            instr = instr | (arg2.reg << 20);
            instr = addLit(instr, arg2.offset);
        }
        else if (arg1.type == ArgType.REGISTER_INDIRECT && arg2.type == ArgType.REGISTER) {            
            // MOV [ra+offset],rb
            instr = addOpcode(instr, MOV_IR);
            instr = instr | (arg1.reg << 22);
            instr = instr | (arg2.reg << 20);
            instr = addLit(instr, arg1.offset);
        }
        else if (arg1.type == ArgType.REGISTER_OFFSET && arg2.type == ArgType.REGISTER) {
            // MOV ra+offset,rb
            instr = addOpcode(instr, MOV_OR);
            instr = instr | (arg1.reg << 22);
            instr = instr | (arg2.reg << 20);
            instr = addLit(instr, arg1.offset);
        }
        else {
            error(line, "instruction MOV "+arg1.arg+","+arg2.arg+" not supported");
        }
        return instr;
    }

    private int rrop(int instr, Arg arg1, Arg arg2) {
        if (arg1.type == ArgType.REGISTER && arg2.type == ArgType.REGISTER) {
            instr = addRb(instr, arg2.reg);
            instr = addRa(instr, arg1.reg);
            instr = addR2r(instr, true);
            return instr;
        }
        else if (arg1.type == ArgType.LITERAL && arg2.type == ArgType.REGISTER) {
            instr = addRb(instr, arg2.reg);
            instr = addLit(instr, arg1.literal);
            instr = addR2r(instr, false);
            return instr;
        }
        else {
            error(line, "illegal instruction");
            return 0;
        }
    }

    private void addInstr(int instr) {
        text[instrPtr] = instr;
        instrPtr++;
    }
    
    private void addRef(String name, int instrPtr) {
        if (internals.contains(name))
            addInternalRef(name, instrPtr);
        else
            addExternalRef(name, instrPtr);
    }
    
    private void addExternalRef(String name, int instrPtr) {
        List<Integer> list = externalRef.get(name);
        if (list == null) {
            list = new LinkedList<>();
            externalRef.put(name, list);
        }
        list.add(instrPtr);
    }
    
    private void addInternalRef(String name, int instrPtr) {
        List<Integer> list = internalRef.get(name);
        if (list == null) {
            list = new LinkedList<>();
            internalRef.put(name, list);
        }
        list.add(instrPtr);
    }

    private void addLabel(String name, int IP) {
        if (labels.get(name) != null)
            error(line, "duplicate label: "+name);
        labels.put(name, IP);
    }

    private void addLabelRef(String name, int IP) {
        List<Integer> list = labelRef.get(name);
        if (list == null) {
            list = new LinkedList<>();
            labelRef.put(name, list);
        }
        list.add(IP);
    }

    public void error(int line, String msg) {
        errorHandler.error(line, 0, msg);
    }

    public void illegalOperands(int line, String op, Arg arg1, Arg arg2) {
        String instr = op;
        if (arg1.type != NOT_SPECIFIED) instr += " "+arg1.arg; 
        if (arg2.type != NOT_SPECIFIED) instr += ","+arg2.arg; 
        errorHandler.error(line, 0, "illegal operands: "+instr);
    }

    public static void warning(String msg) {
        System.err.println("b-as: warning: "+msg);
    }

    public static void error(String msg) {
        System.err.println("b-as: error: "+msg);
        System.exit(1);
    }
    
    static {
        registers.put("SP", SP);
        registers.put("FP", FP);
        registers.put("R0", R0);
        registers.put("R1", R1);
    }

    public static void main(String[] args) {
        String filename = null;
        String outname = null;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--help") || args[i].equals("-h")) {
                System.out.println("Usage: b-as [-o FILE] FILE");
                System.exit(0);
            }
            else if (args[i].startsWith("-o")) {
                if (args[i].length() > 2)
                    outname = args[i].substring(2); 
                else if (i < args.length-1)
                    outname = args[++i];
                else
                    error("missing filename after -o");
            }
            else if (args[i].startsWith("-")) {
                warning("unknown option '"+args[i]+"'");
            }
            else if (filename == null) {
                filename = args[i];
            }
            else {
                warning("unknown option '"+args[i]+"'");
            }
        }
        
        if (filename == null) error("no input file");
        
        if (!filename.endsWith(".bs")) error("invalid input file '"+filename+"'");
        
        try {
            if (outname == null) outname = filename.substring(0, filename.length()-"bs".length())+"bo";
            InputStream in = new FileInputStream(filename);
            PrintStream out = new PrintStream(outname);
            ErrorHandler errorHandler = new ErrorHandler(filename, System.out);
            Assembler assembler = new Assembler(out, errorHandler);
            assembler.assemble(in);            
            out.close();
            in.close();
            if (errorHandler.errorCount > 0) {
                new File(outname).delete();
                System.exit(1);
            }
        }
        catch (Exception e) {
            new File(outname).delete();
            error("cannot open input file '"+filename+"'");
        }
    }
}
