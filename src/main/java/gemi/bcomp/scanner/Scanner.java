package gemi.bcomp.scanner;

import static gemi.bcomp.scanner.TokenType.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import gemi.bcomp.utilities.ErrorHandler;

public class Scanner {
    
    public final static int NUL = 0x00;
    public final static int EOT = 0x04;
    public final static int LF  = 0x0A;
    public final static int HT  = 0x09;

    public final static int CHAR_QUOTE = '\'';
    public final static int STRING_QUOTE = '\"';

    private ErrorHandler errorHandler;
    private InputStream in;
    private int col = -1;
    private int line = 0;
    private int ch;
    
    public boolean kernighan = false;
        
    private final static Map<String,TokenType> keywords = new HashMap<>();

    public Scanner(InputStream in, ErrorHandler errorHandler) throws IOException {
        this.in = in;
        this.errorHandler = errorHandler;
        nextChar();
    }
    
    public Token next() {
        while (true) {
            // swallow whitespace
            while (Character.isWhitespace(ch)) nextChar();

            // end of file
            if (ch < 0) return makeToken(EOF, "", line, col);
            
            // save start position of token (for error handling)
            int cur_line = line;
            int cur_col = col;

            switch (ch) {
            case '=':
                nextChar();
                switch (ch) {
                case '=':
                    nextChar();
                    if (ch == '=')
                        return makeTokenAndAdvance(ASEQUALS, "===", cur_line, cur_col);
                    else
                        return makeToken(EQUALS, "==", cur_line, cur_col);
                case '!':
                    nextChar();
                    if (ch == '=')
                        return makeTokenAndAdvance(ASNEQUALS, "=!=", cur_line, cur_col);
                    else
                        error(line, col, "unexpected character "+(char)ch+" ("+ch+")");
                    break;
                case '*':
                    return makeTokenAndAdvance(ASMUL, "=*", cur_line, cur_col);
                case '+':
                    return makeTokenAndAdvance(ASPLUS, "=+", cur_line, cur_col);
                case '-':
                    return makeTokenAndAdvance(ASMINUS, "=-", cur_line, cur_col);
                case '/':
                    return makeTokenAndAdvance(ASMUL, "=/", cur_line, cur_col);
                case '%':
                    return makeTokenAndAdvance(ASMOD, "=%", cur_line, cur_col);
                case '^':
                    if (kernighan)
                        return makeTokenAndAdvance(ASXOR, "=^", cur_line, cur_col);
                    else
                        return makeToken(ASS, "=", cur_line, cur_col);
                case '|':
                    return makeTokenAndAdvance(ASOR, "=|", cur_line, cur_col);
                case '&':
                    return makeTokenAndAdvance(ASAND, "=&", cur_line, cur_col);
                case '<':
                    nextChar();
                    if (ch == '=')
                        return makeTokenAndAdvance(ASLE, "=<=", cur_line, cur_col);
                    else if (ch == '<')
                        return makeTokenAndAdvance(ASLSHIFT, "=<<", cur_line, cur_col);
                    else
                        return makeToken(ASLT, "=<", cur_line, cur_col);
                case '>':
                    nextChar();
                    if (ch == '=')
                        return makeTokenAndAdvance(ASGE, "=>=", cur_line, cur_col);
                    else if (ch == '>')
                        return makeTokenAndAdvance(ASRSHIFT, "=>>", cur_line, cur_col);
                    else
                        return makeToken(ASGT, "=>", cur_line, cur_col);
                default:
                    return makeToken(ASS, "=", cur_line, cur_col);
                }
            case '<':
                nextChar();
                if (ch == '=')
                    return makeTokenAndAdvance(LE, "<=", cur_line, cur_col);
                else if (ch == '<')
                    return makeTokenAndAdvance(LSHIFT, "<<", cur_line, cur_col);
                else
                    return makeToken(LT, "<", cur_line, cur_col);
            case '>':
                nextChar();
                if (ch == '=')
                    return makeTokenAndAdvance(GE, ">=", cur_line, cur_col);
                else if (ch == '>')
                    return makeTokenAndAdvance(RSHIFT, ">>", cur_line, cur_col);
                else
                    return makeToken(GT, ">", cur_line, cur_col);
            case '+':
                nextChar();
                if (ch == '+')
                    return makeTokenAndAdvance(INC, "++", cur_line, cur_col);
                else
                    return makeToken(PLUS, "+", cur_line, cur_col);
            case '-':
                nextChar();
                if (ch == '-')
                    return makeTokenAndAdvance(DEC, "--", cur_line, cur_col);
                else
                    return makeToken(MINUS, "-", cur_line, cur_col);
            case '*':
                return makeTokenAndAdvance(STAR, "*", cur_line, cur_col);
            case '%':
                return makeTokenAndAdvance(MOD, "%", cur_line, cur_col);
            case '|':
                return makeTokenAndAdvance(OR, "|", cur_line, cur_col);
            case '&':
                return makeTokenAndAdvance(AND, "&", cur_line, cur_col);
            case '^':
                if (kernighan)
                    return makeTokenAndAdvance(XOR, "^", cur_line, cur_col);
                else
                    break;
            case '~':
                if (kernighan)
                    return makeTokenAndAdvance(COMPL, "~", cur_line, cur_col);
                else
                    break;
            case '/':
                nextChar();
                if (ch == '*') {
                    // comment
                    nextChar();
                    while (ch >= 0) {
                        if (ch == '*') {
                            nextChar();
                            if (ch == '/') {
                                nextChar();
                                return next();
                            }
                        }
                        else {
                            nextChar();
                        }
                    }
                    return makeToken(EOF, "", line, col);
                }
                return makeToken(DIV, "/", cur_line, cur_col);
            case '(':
                return makeTokenAndAdvance(LPAREN, "(", cur_line, cur_col);
            case ')':
                return makeTokenAndAdvance(RPAREN, ")", cur_line, cur_col);
            case ';':
                return makeTokenAndAdvance(SEMICOLON, ";", cur_line, cur_col);
            case ',':
                return makeTokenAndAdvance(COMMA, ",", cur_line, cur_col);
            case ':':
                return makeTokenAndAdvance(COLON, ":", cur_line, cur_col);
            case '.':
                return makeTokenAndAdvance(PERIOD, ".", cur_line, cur_col);
            case '!':
                nextChar();
                if (ch == '=')
                    return makeTokenAndAdvance(NEQUALS, "!=", cur_line, cur_col);
                else
                    return makeToken(NOT, "!", cur_line, cur_col);
            case '[':
                return makeTokenAndAdvance(LBRACKET, "[", cur_line, cur_col);
            case ']':
                return makeTokenAndAdvance(RBRACKET, "]", cur_line, cur_col);
            case '{':
                return makeTokenAndAdvance(LBRACE, "{", cur_line, cur_col);
            case '}':
                return makeTokenAndAdvance(RBRACE, "}", cur_line, cur_col);
            case '?':
                return makeTokenAndAdvance(ASK, "?", cur_line, cur_col);
            }
            
            // string
            if (ch == STRING_QUOTE) return string();

            // character
            if (ch == CHAR_QUOTE) return character();

            // identifiers and keywords
            if (isNameStartingCharacter(ch)) {
                StringBuilder buf = new StringBuilder();
                buf.append((char)ch);
                nextChar();
                while (isNameCharacter(ch)) {                
                    buf.append((char)ch);
                    nextChar();
                }
                String text = buf.toString();
                int maxlength = 31;
                if (text.length() > maxlength) {
                    error(cur_line, cur_col, "identifier '"+text+"' too long, truncated to "+maxlength+" characters");
                    text = text.substring(0, maxlength);
                }
                
                Token token = new Token(TokenType.NAME, text, cur_line, cur_col);
                lookupKeyword(token);
                return token;
            }
    
            // number
            if (ch >= '0' && ch <= '9') return number();
            
            error(line, col, "unexpected character "+(char)ch+" ("+ch+")");
            nextChar();
        }
    }
    
    private Token number() {
        Token token = new Token(NUMBER, 0, line, col);
        
        // set base: 8 if starts with 0, 10 otherwise
        int b = (ch == '0')?8:10;

        // skip leading zeros
        while (ch == '0') nextChar();
        while (ch >= '0' && ch <= '9') {
            int d = ch-'0';
            if (d >= b) error(line, col, "invalid digit "+d);
            token.number = (token.number*b)+d;
            nextChar();
        }

        return token;
    }
    
    private Token string() {
        Token token = new Token(STRING, 0, line, col);
        nextChar();
        if (ch < 0) return makeToken(EOF, "", line, col);

        int[] buf = new int[1024];
        int bufi = 0;
        int n = 0;
        while (ch >= 0 && ch != STRING_QUOTE) {
            if (ch == '*') {
                nextChar();
                ch = escapeChar(ch);
            }
            buf[bufi] = buf[bufi] | ((ch&0xFF) << (n*8));
            n++;
            if (n == 4) {
                n = 0;
                bufi++;
            }
            nextChar();
        }

        if (ch != STRING_QUOTE)
            error(line, col, "string not terminated by \"");
        else
            nextChar();

        // append *e
        buf[bufi] = buf[bufi] | (EOT << (n*8));
        n++;
        if (n == 4) {
            n = 0;
            bufi++;
        }
        
        int len = bufi;
        if (n > 0) len++;
        if (len == 0) len = 1;
        token.string = Arrays.copyOf(buf, len);

        return token;
    }
    
    private Token character() {
        nextChar();
        if (ch < 0) return makeToken(EOF, "", line, col);
        Token token = new Token(NUMBER, 0, line, col);        

        int n = 0;
        while (ch >= 0 && ch != CHAR_QUOTE) {
            if (n == 4) error(line, col, "character letter has maximum length of 4");
            if (ch == '*') {
                nextChar();
                ch = escapeChar(ch);
            }
            token.number = token.number | ((ch&0xFF) << (n*8));
            n++;
            nextChar();            
        }

        if (ch != CHAR_QUOTE)
            error(line, col, "character literal not terminated by '");
        else
            nextChar();

        return token;
    }
    
    private void nextChar() {
        try {
            ch = in.read();
            if (ch == '\n') {
                col = -1;
                line++;
            }
            else {
                col++;
            }
        } catch (IOException e) {
            error(line, col, "I/O");
            ch = -1;
        }
    }
    
    private void error(int line, int col, String text) {
        errorHandler.error(line, col, text);
    }
    
    private Token makeTokenAndAdvance(TokenType tokenType, String text, int line, int col) {
        Token token = new Token(tokenType, text, line, col);
        nextChar();
        return token;
    }
   
    private static Token makeToken(TokenType tokenType, String text, int line, int col) {
        Token token = new Token(tokenType, text, line, col);
        return token;
    }

    private static boolean isNameStartingCharacter(int ch) {
        return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch == '_');
    }

    private static boolean isNameCharacter(int ch) {
        return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9') || (ch == '_');
    }
    
    private static final void lookupKeyword(Token token) {
        TokenType tokenType = keywords.get(token.text);
        if (tokenType != null) token.type = tokenType;
    }
    
    private final int escapeChar(int ch) {
        switch (ch) {
        case '0':
            ch = NUL;
            break;
        case 'e':
            ch = EOT;
            break;
        case '(':
            ch = '{';
            break;
        case ')':
            ch = '}';
            break;
        case 't':
            ch = HT;
            break;
        case '*':
            ch = '*';
            break;
        case CHAR_QUOTE:
            ch = CHAR_QUOTE;
            break;
        case STRING_QUOTE:
            ch = STRING_QUOTE;
            break;
        case 'n':
            ch = LF;
            break;
        default:
            error(line, col, "unknown escaped character *"+(char)ch);
            ch = '*';
        }
        return ch;
    }
    
    static {
        for (TokenType tokenType : TokenType.values()) {
            if (tokenType.keyword) keywords.put(tokenType.name, tokenType);
        }
    }
}
