package gemi.bcomp.utilities;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import gemi.bcomp.scanner.Scanner;

public class Utilities {
    
    public static String int2hex(int n) {
        StringBuilder buf = new StringBuilder();
        for (int i = 7; i >= 0; i--) {
            int v = (n >> i*4)&0xF;
            if (v < 10)
                buf.append((char)('0'+v));
            else
                buf.append((char)('A'+(v-10)));
        }
        return buf.toString();
    }
    
    public static int hex2int(String hex) {
        int n = 0;
        for (int i = 0; i < hex.length(); i++) {
            char ch = hex.charAt(i);
            if (ch >= '0' && ch <= '9') {
                n = (n<<4)+(ch-'0');
            }
            else if (ch >= 'A' && ch <= 'F') {
                n = (n<<4)+(10+ch-'A');
            }
            else {
                System.err.println("ERROR");
            }
        }
        return n;
    }
    
    /**
     * Checks whether the collection is <code>null</code> or empty.
     */
    public static boolean isNotEmpty(Collection<?> c) {
        return c != null && c.size() > 0;
    }

    /**
     * Extracts opcode.
     */
    public static int opcode(int IR) {
        int opcode = (IR >> 24)&0xFF;
        if ((opcode & 0b10000000) != 0) opcode = opcode & 0xF0;
        return opcode;
    }
    
    /**
     * Adds opcode to instruction.
     */
    public static int addOpcode(int instr, int op) {
        return ((op & 0xFF) << 24)|instr;
    }
    
    /**
     * Extracts address.
     */
    public static int adr(int instr) {
        return instr & 0xFFFFFFF;
    }
    
    /**
     * Adds address to instruction.
     */
    public static int addAdr(int instr, int adr) {
        return (adr & 0xFFFFFFF)|instr;
    }

    /**
     * Extracts rb register.
     */
    public static int rb(int instr) {
        return (instr >> 22) & 0b11;
    }

    /**
     * Add rb register to instruction.
     */
    public static int addRb(int instr, int reg) {
        return ((reg & 0b11) << 22)|instr;
    }

    /**
     * Extract ra register.
     */
    public static int ra(int instr) {
        return (instr >> 18) & 0b11;
    }

    /**
     * Add ra register to instruction.
     */
    public static int addRa(int instr, int reg) {
        return ((reg & 0b11) << 18)|instr;
    }

    /**
     * Whether the instr is register to register.
     */
    public static boolean r2r(int instr) {
        return ((instr >> 20) & 0b11) == 0;
    }

    /**
     * Adds whether the instr is register to register.
     */
    public static int addR2r(int instr, boolean is_r2r) {
        if (is_r2r)
            return instr;
        else
            return (0b11 << 20)|instr;
    }

    /**
     * Extracts literal number.
     */
    public static int lit(int instr) {
        int val = instr & 0xFFFFF;
        if ((instr & 0x80000) != 0) val |= 0xFFF00000;
        return val;
    }

    public static int addLit(int instr, int lit) {
        return (lit & 0xFFFFF)|instr;
    }

    /**
     * Extracts offset.
     */
    public static int offset(int instr) {
        int val = instr & 0xFFFFFF;
        if ((instr & 0x800000) != 0) val |= 0xFF000000;
        return val;
    }

    /**
     * Adds offset to instruction.
     */
    public static int addOffset(int instr, int offset) {
        return (offset & 0xFFFFFF)|instr;
    }

    /**
     * Returns the reversed list.
     */
    public static <T> List<T> reverse(List<T> list) {
        List<T> reversed = new LinkedList<T>();
        if (list == null) return reversed;
        for (T e : list) {
            reversed.add(0, e);
        }
        return reversed;
    }
    
    public static long string2long(String s) {
        try {
            return Long.parseLong(s);
        }
        catch (Exception e) {
            return 0;
        }        
    }
    
    public static int string2int(String s) {
        try {
            return Integer.parseInt(s);
        }
        catch (Exception e) {
            return 0;
        }        
    }
    
    public static int[] string2words(String s) {
        byte[] bytes = s.getBytes();
        int[] buf = new int[bytes.length];
        int bufi = 0;
        int n = 0;
        for (byte b : bytes) {
            buf[bufi] = buf[bufi] | ((((int)b)&0xFF) << (n*8));
            n++;
            if (n == 4) {
                n = 0;
                bufi++;
            }
        }
        // append *e
        buf[bufi] = buf[bufi] | (Scanner.EOT << (n*8));
        n++;
        if (n == 4) {
            n = 0;
            bufi++;
        }
        int len = bufi;
        if (n > 0) len++;
        if (len == 0) len = 1;
        return Arrays.copyOf(buf, len);
    }

    public static void indent(int indent, String s) {
        for (int i = 0; i < indent; i++) System.out.print("  ");
        System.out.print(s);
    }
}
