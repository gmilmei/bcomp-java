package gemi.bcomp.assembler;

import static gemi.bcomp.utilities.Utilities.string2int;

public class Arg {

    public enum ArgType {
        REGISTER,
        REGISTER_INDIRECT,
        REGISTER_OFFSET,
        ADDRESS_LABEL,
        ADDRESS_LITERAL,
        LITERAL,
        UNKNOWN,
        NOT_SPECIFIED
    }
    
    public ArgType type = ArgType.UNKNOWN;
    public String arg = null;
    public int literal = 0;
    public int reg = 0;
    public int offset = 0;
    public String label = null;
    
    public Arg() {
        this.type = ArgType.NOT_SPECIFIED;
        this.arg = "unspecified";
    }
    
    public Arg(String arg) {
        this.arg = arg;
        Integer reg = Assembler.registers.get(arg);

        if (reg != null) {
            this.reg = reg;
            this.type = ArgType.REGISTER;
            return;
        }

        if (arg.matches("#[-]?[0-9]+")) {
            arg = arg.substring(1);
            int sign = 1;
            if (arg.startsWith("-")) {
                arg = arg.substring(1);
                sign = -1;
            }
            this.literal =sign* string2int(arg);
            this.type = ArgType.LITERAL;
            return;
        }

        if (arg.matches("[a-zA-Z$_][a-zA-Z$0-9_.]*")) {
            this.label = arg;
            this.type = ArgType.ADDRESS_LABEL;
            return;
        }

        if (arg.matches("[0-9]+")) {
            this.literal = string2int(arg.substring(0));
            this.type = ArgType.ADDRESS_LITERAL;
            return;
        }

        if (arg.matches("\\[.+\\]")) {
            arg = arg.substring(1);
            arg = arg.substring(0, arg.length()-1);
            int i = arg.indexOf("-");
            if (i > 0) {
                reg = Assembler.registers.get(arg.substring(0, i));
                if (reg == null) return;
                this.reg = reg;
                this.offset = string2int(arg.substring(i));
                this.type = ArgType.REGISTER_INDIRECT;
                return;
            }
            i = arg.indexOf("+");
            if (i > 0) {
                reg = Assembler.registers.get(arg.substring(0, i));
                if (reg == null) return;
                this.reg = reg;
                this.offset = string2int(arg.substring(i));
                this.type = ArgType.REGISTER_INDIRECT;
                return;
            }
            reg = Assembler.registers.get(arg);
            if (reg != null) {
                this.reg = reg;
                this.type = ArgType.REGISTER_INDIRECT;
            }
            return;
        }
        return;
    }
    
    @Override
    public String toString() {
        return "'"+arg+"',"+type+",literal="+literal+",reg="+reg+",label="+label+",offset="+offset;
    }
}
