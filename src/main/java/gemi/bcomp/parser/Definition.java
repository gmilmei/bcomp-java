package gemi.bcomp.parser;

import static gemi.bcomp.utilities.Utilities.indent;

import java.util.List;

import gemi.bcomp.scanner.Token;

public class Definition {

    public String name;
    public List<String> formals = null;
    public List<Statement> statements = null;
    public boolean isFunction = false;
    public boolean isVector = false;
    public boolean isString = false;
    public int size = 0;
    public List<Token> ivals = null;
    public int line = 0;
    public int col = 0;
    
    public Definition(int line, int col) {
        this.line = line;
        this.col = col;
    }
    
    public void dump(int indent) {
        indent(indent, "Definition: "+name+"\n");
        if (isFunction) indent(indent+1, "function\n");
        if (isVector) {
            indent(indent+1, "vector\n");
            indent(indent+1, "size: "+size+"\n");
        }
        if (formals != null && formals.size() > 0) {
            indent(indent+1, "formals\n");
            for (String formal : formals) {
                indent(indent+2, formal+"\n");
            }
        }
        if (statements != null && statements.size() > 0) {
            indent(indent+1, "statements\n");
            for (Statement statement : statements) {
                statement.dump(indent+2);
            }
        }
        if (ivals != null && ivals.size() > 0) {
            indent(indent+1, "ivals\n");
            for (Token token : ivals) {
                indent(indent+2, token+"\n");
            }
        }
    }
}
