package gemi.bcomp.parser;

import java.util.List;

import static gemi.bcomp.utilities.Utilities.*;

public class Statement {
    
    public enum StatementType {
        AUTO,
        BREAK,
        CASE,
        COMPOUND,
        EXPR,
        EXTRN,
        GOTO,
        IF,
        LABEL,
        NULL,
        RETURN,
        SWITCH,
        WHILE
    }

    public StatementType type = null;
    public List<Statement> statements = null;
    public List<Expr> exprs = null;
    public String name = null;

    public int line = 0;
    public int col = 0;
    
    public Statement(StatementType type, int line, int col) {
        this.type = type;
        this.line = line;
        this.col = col;
    }
    
    public static int allocateStack(List<Statement> statements) {
        int s = 0;
        if (isNotEmpty(statements)) {
            for (Statement st : statements) {
                s += st.allocateStack();
            }
        }
        return s;
    }
    
    public int allocateStack() {
        int s = 0, sc = 0;
        switch (type) {
        case BREAK:
        case CASE:
        case EXPR:
        case EXTRN:
        case GOTO:
        case LABEL:
        case NULL:
        case RETURN:
            return 0;
        case AUTO:
            if (isNotEmpty(exprs)) {
                s += exprs.size();
            }
            break;
        case IF:
            if (isNotEmpty(statements)) {
                for (Statement st : statements) {
                    s = Math.max(s, st.allocateStack());
                }
            }
            return s;
        case COMPOUND:
        case SWITCH:
        case WHILE:
            sc = 0;
            if (isNotEmpty(statements)) {
                for (Statement st : statements) {
                    if (st.type == StatementType.COMPOUND)
                        sc = Math.max(sc, st.allocateStack());
                    else
                        s += st.allocateStack();
                }
            }
            return s+sc;
        default:
            System.err.println("allocateStack: missing case "+type);
            System.exit(1);
        }
        return s;
    }
    
    public void dump(int indent) {
        indent(indent, "Statement: "+type+"\n");
        if (name != null) {
            indent(indent+1, "name: "+name+"\n");            
        }
        if (isNotEmpty(statements)) {
            indent(indent+1, "statements\n");            
            for (Statement statement : statements) {
                statement.dump(indent+2);
            }
        }
        if (isNotEmpty(exprs)) {
            indent(indent+1, "exprs\n");            
            for (Expr expr : exprs) {
                expr.dump(indent+2);
            }
        }
    }
}
