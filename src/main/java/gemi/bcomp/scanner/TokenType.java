package gemi.bcomp.scanner;

import java.util.HashSet;
import java.util.Set;

public enum TokenType {

    // special tokens
    EOF,
    ERROR,
    
    // literals
    NAME,
    NUMBER,
    STRING,
    
    // symbols
    AND,
    ASAND,
    ASDIV,
    ASEQUALS,
    ASGE,
    ASGT,
    ASK,
    ASLE,
    ASLSHIFT,
    ASLT,
    ASMINUS,
    ASMOD,
    ASMUL,
    ASNEQUALS,
    ASOR,
    ASPLUS,
    ASRSHIFT,
    ASS,
    ASXOR,
    COLON,
    COMMA,
    COMPL,
    DEC,
    DIV,
    EQUALS,
    GE,
    GT,
    INC,
    LBRACE,
    LBRACKET,
    LE,
    LPAREN,
    LSHIFT,
    LT,
    MINUS,
    MOD,
    NEQUALS,
    NOT,
    OR,
    PERIOD,
    PLUS,
    RBRACE,
    RBRACKET,
    RPAREN,
    RSHIFT,
    SEMICOLON,
    STAR,
    XOR,
    
    // reserved words (keywords)
    AUTO ("auto"),
    BREAK ("break"),
    CASE ("case"),
    DEFAULT ("default"),
    ELSE ("else"),
    EXTRN ("extrn"),
    GOTO ("goto"),
    IF ("if"),
    RETURN ("return"),
    SWITCH ("switch"),
    WHILE ("while");
    
    public String name = null;
    public boolean keyword = false;
    
    private TokenType() {}
    
    private TokenType(String name) {
        this.name = name;
        this.keyword = true;
    }
    
    public static Set<TokenType> assignmentTypes = new HashSet<>();
    
    static {
        assignmentTypes.add(ASAND);
        assignmentTypes.add(ASDIV);
        assignmentTypes.add(ASEQUALS);
        assignmentTypes.add(ASGE);
        assignmentTypes.add(ASGT);
        assignmentTypes.add(ASLE);
        assignmentTypes.add(ASLSHIFT);
        assignmentTypes.add(ASLT);
        assignmentTypes.add(ASMINUS);
        assignmentTypes.add(ASMOD);
        assignmentTypes.add(ASMUL);
        assignmentTypes.add(ASNEQUALS);
        assignmentTypes.add(ASOR);
        assignmentTypes.add(ASPLUS);
        assignmentTypes.add(ASRSHIFT);
        assignmentTypes.add(ASS);
        assignmentTypes.add(ASXOR);
    }
}
