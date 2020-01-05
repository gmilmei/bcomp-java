package gemi.bcomp.assembler;

public class Opcodes {

    public final static int NOP    = 0b00000000;
    public final static int RET    = 0b00000001;
    public final static int HALT   = 0b00000010;
    public final static int CALL_R = 0b01000000;
    public final static int CALL_A = 0b10000000;
    public final static int JMP_R  = 0b01000001;
    public final static int JMP_A  = 0b10010000;
    public final static int LAD_A0 = 0b10100000;
    public final static int MOV_A0 = 0b10110000;
    public final static int MOV_0A = 0b11000000;
    public final static int ADD    = 0b01000010;
    public final static int SUB    = 0b01000011;
    public final static int MUL    = 0b01000100;
    public final static int DIV    = 0b01000101;
    public final static int MOD    = 0b01000110;
    public final static int NEG    = 0b01000111;
    public final static int AND    = 0b01001000;
    public final static int OR     = 0b01001001;
    public final static int XOR    = 0b01001010;
    public final static int NOT    = 0b01001011;
    public final static int CPL    = 0b01001100;
    public final static int LSH    = 0b01001101;
    public final static int RSH    = 0b01001110;
    public final static int EXCH   = 0b01001111;
    public final static int POP    = 0b01010000;
    public final static int PUSH   = 0b01010001;
    public final static int MOV_RR = 0b01010010;
    public final static int MOV_L0 = 0b01010011;
    public final static int MOV_L1 = 0b01010100;
    public final static int MOV_RI = 0b01010101;
    public final static int MOV_IR = 0b01010110;
    public final static int MOV_OR = 0b01010111;
    public final static int BNE    = 0b01011000;
    public final static int BEQ    = 0b01011001;
    public final static int BNG    = 0b01011010;
    public final static int BPS    = 0b01011011;
    public final static int JRL    = 0b01011100;
    public final static int SYS_R  = 0b01011101;
    public final static int SYS_L  = 0b01011110;
}
