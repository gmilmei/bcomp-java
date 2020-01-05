package gemi.bcomp.scanner;

import static gemi.bcomp.utilities.Utilities.int2hex;

public class Token {

    public TokenType type;
    
    // names
    public String text = null;
    // literal string (as array of words)
    public int[] string = null;
    // literal for integers or characters
    public int number = 0;
    // token position
    public int line = 0;
    public int col = 0;
    
    public Token(TokenType type, String text, int line, int col) {
        this.type = type;
        this.text = text;
        this.line = line;
        this.col = col;
    }
    
    public Token(TokenType type, int number, int line, int col) {
        this.type = type;
        this.number = number;
        this.line = line;
        this.col = col;
    }

    @Override
    public String toString() {
        switch (type) {
        case NUMBER:
            return type+"["+int2hex((int)(number&0xFFFFFFFF))+","+line+","+col+"]";
        case NAME:
            return type+"[\""+text+"\","+line+","+col+"]";
        case STRING:
            StringBuilder buf = new StringBuilder();
            for (int b : string) {
                if (buf.length() > 0) buf.append(' ');
                buf.append(int2hex(b));
            }
            return type+"["+buf+","+line+","+col+"]";
        default:
            return type+"["+line+","+col+"]";
        }
    }
}
