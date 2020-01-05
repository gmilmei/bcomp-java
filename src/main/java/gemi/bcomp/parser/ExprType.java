package gemi.bcomp.parser;

import java.util.HashMap;
import java.util.Map;

import gemi.bcomp.scanner.TokenType;

public enum ExprType {
    
    AND,
    APP,
    ASAND,
    ASDIV,
    ASEQUALS,
    ASGE,
    ASGT,
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
    COMPL,
    COND,
    DEREF,
    DIV,
    EQUALS,
    GE,
    GROUP,
    GT,
    INDEX,
    LE,
    LSHIFT,
    LT,
    MINUS,
    MOD,
    MUL,
    NAME,
    NEG,
    NEQUALS,
    NOT,
    NUMBER,
    OR,
    PLUS,
    POSTDEC,
    POSTINC,
    PREDEC,
    PREINC,
    REF,
    RSHIFT,
    STRING,
    XOR;
    
    private static Map<TokenType,ExprType> tokenToExpr = new HashMap<>();
    
    static {
        tokenToExpr.put(TokenType.AND, AND);
        tokenToExpr.put(TokenType.ASAND, ASAND);
        tokenToExpr.put(TokenType.ASDIV, ASDIV);
        tokenToExpr.put(TokenType.ASEQUALS, ASEQUALS);
        tokenToExpr.put(TokenType.ASGE, ASGE);
        tokenToExpr.put(TokenType.ASGT, ASGT);
        tokenToExpr.put(TokenType.ASLE, ASLE);
        tokenToExpr.put(TokenType.ASLSHIFT, ASLSHIFT);
        tokenToExpr.put(TokenType.ASLT, ASLT);
        tokenToExpr.put(TokenType.ASMINUS, ASMINUS);
        tokenToExpr.put(TokenType.ASMOD, ASMOD);
        tokenToExpr.put(TokenType.ASMUL, ASMUL);
        tokenToExpr.put(TokenType.ASNEQUALS, ASNEQUALS);
        tokenToExpr.put(TokenType.ASOR, ASOR);
        tokenToExpr.put(TokenType.ASPLUS, ASPLUS);
        tokenToExpr.put(TokenType.ASRSHIFT, ASRSHIFT);
        tokenToExpr.put(TokenType.ASS, ASS);
        tokenToExpr.put(TokenType.ASXOR, ASXOR);
        tokenToExpr.put(TokenType.COMPL, COMPL);
        tokenToExpr.put(TokenType.DIV, DIV);
        tokenToExpr.put(TokenType.EQUALS, EQUALS);
        tokenToExpr.put(TokenType.GE, GE);
        tokenToExpr.put(TokenType.GT, GT);
        tokenToExpr.put(TokenType.LE, LE);
        tokenToExpr.put(TokenType.LSHIFT, LSHIFT);
        tokenToExpr.put(TokenType.LT, LT);
        tokenToExpr.put(TokenType.MINUS, MINUS);
        tokenToExpr.put(TokenType.MOD, MOD);
        tokenToExpr.put(TokenType.NEQUALS, NEQUALS);
        tokenToExpr.put(TokenType.NOT, NOT);
        tokenToExpr.put(TokenType.OR, OR);
        tokenToExpr.put(TokenType.PLUS, PLUS);
        tokenToExpr.put(TokenType.RSHIFT, RSHIFT);
        tokenToExpr.put(TokenType.STAR, MUL);
        tokenToExpr.put(TokenType.XOR, XOR);
    }
    
    public static ExprType fromTokenType(TokenType tokenType) {
        ExprType exprType = tokenToExpr.get(tokenType);
        if (exprType == null) {
            System.err.println("cannot convert token type "+tokenType+" to expression type");
            System.exit(1);
        }
        return exprType;
    }
}
