package gemi.bcomp.parser;

import java.util.List;

public class Expr {

    public ExprType op;
    public Expr expr1 = null; 
    public Expr expr2 = null; 
    public Expr expr3 = null;
    public String name = null;
    public Integer number = null;
    public List<Expr> args = null;
    
    public int line = 0;
    public int col = 0;
        
    public Expr(ExprType op, int line, int col) {
        this.op = op;
        this.line = line;
        this.col = col;
    }

    public Expr(ExprType op, Expr expr1, int line, int col) {
        this.op = op;
        this.expr1 = expr1;
        this.line = line;
        this.col = col;
    }

    public Expr(ExprType op, Expr expr1, Expr expr2, int line, int col) {
        this.op = op;
        this.expr1 = expr1;
        this.expr2 = expr2;
        this.line = line;
        this.col = col;
    }

    public Expr(ExprType op, Expr expr1, Expr expr2, Expr expr3, int line, int col) {
        this.op = op;
        this.expr1 = expr1;
        this.expr2 = expr2;
        this.expr3 = expr3;
        this.line = line;
        this.col = col;
    }
    
    public Expr(ExprType op, String name, int line, int col) {
        this.op = op;
        this.name = name;
        this.line = line;
        this.col = col;
    }

    public Expr(ExprType op, int number, int line, int col) {
        this.op = op;
        this.number = number;
        this.line = line;
        this.col = col;
    }

    public void dump() {
        dump(0);
    }
    
    public void dump(int indent) {
        for (int i = 0; i < indent; i++) System.out.print("  ");
        System.out.print(op);
        if (name != null) System.out.print(": "+name);
        if (number != null) System.out.print(": "+number);
        System.out.println();
        if (expr1 != null) {
            expr1.dump(indent+1);
        }
        if (expr2 != null) {
            expr2.dump(indent+1);
        }
        if (expr3 != null) {
            expr3.dump(indent+1);
        }
        if (args != null) {
            for (Expr arg : args) {
                arg.dump(indent+1);
            }
        }
    }
    
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        switch (op) {
        case APP:
            buf.append(expr1);
            buf.append("(");
            int n = 0;
            for (Expr arg : args) {
                if (n > 0) buf.append(",");
                buf.append(arg.toString());
                n++;
            }
            buf.append(")");
            return buf.toString();
        case COND:
            return "("+expr1+")?("+expr2+"):("+expr3+")";
        case PLUS:
            return "("+expr1+")+("+expr2+")";
        case MINUS:
            return "("+expr1+")-("+expr2+")";
        case NEG:
            return "-("+expr1+")";
        case MUL:
            return "("+expr1+")*("+expr2+")";
        case DIV:
            return "("+expr1+")*("+expr2+")";
        case MOD:
            return "("+expr1+")%("+expr2+")";
        case ASS:
            return expr1+"="+expr2;
        case REF:
            return "&"+expr1;
        case DEREF:
            return "*"+expr1;
        case GROUP:
            return "("+expr1+")";
        case INDEX:
            return expr1+"["+expr2+"]";
        case NAME:
            return name;
        case NUMBER:
            return Long.toString(number);
        default:
            return "#"+op;
        }
    }
}
