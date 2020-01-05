package gemi.bcomp.disassembler;

import static gemi.bcomp.assembler.Opcodes.*;
import static gemi.bcomp.utilities.Utilities.*;

import java.io.BufferedReader;
import java.io.FileReader;

public class Disassembler {
    
    private static String[] regnames = { "SP", "FP", "R0", "R1" };

    public static String disassemble(int instr) {
        int op = opcode(instr);
        switch (op) {
        case NOP:
            return "NOP";
        case RET:
            return "RET";
        case HALT:
            return "HALT";
        case CALL_R:
            return "CALL "+regnames[rb(instr)]; 
        case JMP_R:
            return "JMP  "+regnames[rb(instr)];
        case MOV_RR:
            return "MOV  "+regnames[ra(instr)]+","+regnames[rb(instr)];
        case MOV_L0:
            return "MOV  #x"+int2hex(offset(instr))+",R0";
        case MOV_L1:
            return "MOV  #x"+int2hex(offset(instr))+",R1";
        case MOV_RI:
            int ra = (instr>>22)&0b11;
            int rb = (instr>>20)&0b11;
            int offset = lit(instr);
            if (offset == 0)
                return "MOV  "+regnames[ra]+",["+regnames[rb]+"]";
            else if (offset < 0) 
                return "MOV  "+regnames[ra]+",["+regnames[rb]+"-"+(-offset)+"]";
            else
                return "MOV  "+regnames[ra]+",["+regnames[rb]+"+"+offset+"]";
        case MOV_IR:
            ra = (instr>>22)&0b11;
            rb = (instr>>20)&0b11;
            offset = lit(instr);
            if (offset == 0)
                return "MOV  ["+regnames[ra]+"],"+regnames[rb];
            else if (offset < 0) 
                return "MOV  ["+regnames[ra]+"-"+(-offset)+"],"+regnames[rb];
            else
                return "MOV  ["+regnames[ra]+"+"+offset+"],"+regnames[rb];
        case MOV_OR:
            ra = (instr>>22)&0b11;
            rb = (instr>>20)&0b11;
            offset = lit(instr); 
            if (offset == 0)
                return "MOV  "+regnames[ra]+","+regnames[rb];
            else if (offset < 0)
                return "MOV  "+regnames[ra]+"-"+(-offset)+","+regnames[rb];
            else
                return "MOV  "+regnames[ra]+"+"+offset+","+regnames[rb];
        case ADD:
            return rrop("ADD ", instr);
        case SUB:
            return rrop("SUB ", instr);
        case MUL:
            return rrop("MUL ", instr);
        case DIV:
            return rrop("DIV ", instr);
        case MOD:
            return rrop("MOD ", instr);
        case NEG:
            return rrop("NEG ", instr);
        case AND:
            return rrop("AND ", instr);
        case OR:
            return rrop("OR  ", instr);
        case XOR:
            return rrop("XOR ", instr);
        case NOT:
            return rrop("NOT ", instr);
        case CPL:
            return rrop("CPL ", instr);
        case LSH:
            return rrop("LSH ", instr);
        case RSH:
            return rrop("RSH ", instr);
        case EXCH:
            return rrop("EXCH", instr);
        case POP:
            return "POP  "+regnames[rb(instr)];
        case PUSH:
            return "PUSH "+regnames[rb(instr)];
        case BNE:
            return "BNE  "+offset(instr);
        case BEQ:
            return "BEQ  "+offset(instr);
        case BNG:
            return "BNG  "+offset(instr);
        case BPS:
            return "BPS  "+offset(instr);
        case JRL:
            return "JRL  "+offset(instr);
        case SYS_L:
            return "SYS  #"+lit(instr);
        case SYS_R:
            return "SYS  "+regnames[rb(instr)];
        default:
            op = op&0xF0;
            switch (op) {
            case LAD_A0:
                return "LAD  x"+int2hex(adr(instr))+",R0";
            case JMP_A:
                return "JMP  x"+int2hex(adr(instr));
            case CALL_A:
                return "CALL x"+int2hex(adr(instr));
            case MOV_A0:
                return "MOV  x"+int2hex(adr(instr))+",R0";
            case MOV_0A:
                return "MOV  R0,x"+int2hex(adr(instr));
            default:
                return "ILLEGAL: "+int2hex(instr);
            }
        }
    }
    
    private static String rrop(String opname, int instr) {
        if (r2r(instr))
            return opname+" "+regnames[ra(instr)]+","+regnames[rb(instr)];
        else
            return opname+" #x"+int2hex(lit(instr))+","+regnames[rb(instr)];
    }
    
    public static void error(String msg) {
        System.err.println("b-dis: error: "+msg);
        System.exit(1);
    }
    
    public static void main(String[] args) {
        String filename = null;
        for (int i = 0; i < args.length; i++) {
            if (filename == null) filename = args[i];
        }

        if (filename == null) error("not input file");
        
        try (BufferedReader reader = new BufferedReader(new FileReader(args[0]))) {
            int n = 0;
            String line = null;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#")) {
                    continue;
                }
                else if (line.startsWith(".memsize ")) {
                    // ignore
                }
                else if (line.length() == 8) {
                    System.out.print(int2hex(n)+": ["+line+"] ");
                    System.out.print(disassemble(hex2int(line)));
                    System.out.println();
                    n++;
                }
            }
        }
        catch (Exception e) {
            error("cannot open input file '"+filename+"'");
        }
    }
}
