package gemi.bcomp.parser;

import static gemi.bcomp.scanner.TokenType.*;

import java.util.LinkedList;
import java.util.List;

import gemi.bcomp.parser.Statement.StatementType;
import gemi.bcomp.scanner.Scanner;
import gemi.bcomp.scanner.Token;
import gemi.bcomp.scanner.TokenType;
import gemi.bcomp.utilities.ErrorHandler;

public class Parser {

    private Scanner scanner;
    private ErrorHandler errorHandler;
    private Token curToken = null;
    private Token lastToken = null;
    private int label = 0;
    private List<Definition> definitions = new LinkedList<>();


    public Parser(Scanner scanner, ErrorHandler errorHandler) {
        this.scanner = scanner;
        this.errorHandler = errorHandler;
    }
 
    public List<Definition> parse() {
        next();
        while (at(NAME)) {
            Definition definition = definition();
            if (definition != null) definitions.add(definition);
        }
        if (!at(EOF)) error(curToken.line, curToken.col, "unexpected token "+curToken.text);
        return definitions;
    }
    
    private Definition definition() {
        Definition definition = new Definition(curToken.line, curToken.col);
        definition.name = curToken.text;
        next();
        if (at(LPAREN)) {
            definition.formals = new LinkedList<>();
            next();
            while (at(NAME)) {
                definition.formals.add(curToken.text);
                next();
                if (at(COMMA)) next();
            }
            if (at(RPAREN))
                next();
            else
                error(curToken.line, curToken.col, "expected ), got "+curToken.text);
            definition.statements = new LinkedList<>();
            statement(definition.statements);
            definition.isFunction = true;
            return definition;
        }
        else if (at(LBRACKET)) {
            next();
            definition.ivals = new LinkedList<>();
            definition.isVector = true;
            definition.size = 0;
            if (at(RBRACKET)) {
                next();
            }
            else if (at(NUMBER)) { 
                definition.size = curToken.number;
                next();
                if (at(RBRACKET))
                    next();                
                else
                    error(curToken.line, curToken.col, "expected ], got "+curToken.text);
            }
            
            while (!at(EOF) && !at(SEMICOLON)) {
                if (curToken.type == NUMBER || curToken.type == NAME ) {
                    definition.ivals.add(curToken);
                    next();
                }
                else if (curToken.type == STRING) {
                    Expr expr = defineString(curToken.line, curToken.col, curToken);
                    curToken.text = expr.name;
                    definition.ivals.add(curToken);
                    next();
                }
                else {
                    error(curToken.line, curToken.col, "unexpected token "+curToken.text);
                }
                if (at(COMMA))
                    next();
                else
                    break;
            }
            
            if (definition.ivals.size() > definition.size)
                definition.size = definition.ivals.size();
            
            if (at(SEMICOLON))
                next();
            else
                error(curToken.line, curToken.col, "expected ;, got "+curToken.text);
            
            return definition;
        }
        else {
            definition.ivals = new LinkedList<>();
            if (at(SEMICOLON)) {            
            }
            else {
                while (!at(EOF) && !at(SEMICOLON)) {
                    if (curToken.type == NUMBER || curToken.type == NAME) {
                        definition.ivals.add(curToken);
                        next();
                    }
                    else if (curToken.type == STRING) {
                        Expr expr = defineString(curToken.line, curToken.col, curToken);
                        curToken.text = expr.name;
                        definition.ivals.add(curToken);
                        next();
                    }
                    else {
                        error(curToken.line, curToken.col, "unexpected token "+curToken.text);
                    }
                    if (at(COMMA))
                        next();
                    else
                        break;
                }
                if (!at(SEMICOLON)) {
                    error(curToken.line, curToken.col, "expected ;, got "+curToken.text);
                }
            }
            
            if (at(SEMICOLON))
                next();
            else
                error(curToken.line, curToken.col, "expected ;, got "+curToken.text);
            return definition;
        }
    }
    
    private boolean statement(List<Statement> statements) {
        if (at(LBRACE)) {
            // { statement ... }
            Statement statement = new Statement(StatementType.COMPOUND, curToken.line, curToken.col);
            statement.statements = new LinkedList<>();
            next();
            while (!at(EOF) && !at(RBRACE)) {
                statement(statement.statements);
            }
            if (at(RBRACE))
                next();
            else
                error(curToken.line, curToken.col, "expected }, got "+curToken.text);
            statements.add(statement);
            return true;
        }
        else if (at(SEMICOLON)) {
            // empty statement
            statements.add(new Statement(StatementType.NULL, curToken.line, curToken.col));
            next();
            return true;
        }
        else if (at(AUTO)) {
            // auto NAME [ival], NAME [ival], ... 
            Statement statement = new Statement(StatementType.AUTO, curToken.line, curToken.col);
            next();
            statement.exprs = new LinkedList<>();
            while (at(NAME) && !at(EOF)) {
                String name = curToken.text;
                Expr expr = new Expr(ExprType.NAME, name, curToken.line, curToken.col);
                next();
                if (at(NUMBER)) {
                    expr.expr1 = expression();
                }
                statement.exprs.add(expr);
                if (at(COMMA))
                    next();
                else if (at(SEMICOLON))
                    break;
            }
            if (at(SEMICOLON)) 
                next();
            else
                error(curToken.line, curToken.col, "expected ;, got "+curToken.text);
            statements.add(statement);
            return statement(statements);
        }
        else if (at(EXTRN)) {
            // extrn NAME, NAME ...
            Statement statement = new Statement(StatementType.EXTRN, curToken.line, curToken.col);
            next();
            statement.exprs = new LinkedList<>();
            while (at(NAME) && !at(EOF)) {
                String name = curToken.text;
                statement.exprs.add(new Expr(ExprType.NAME, name, curToken.line, curToken.col));
                next();
                if (at(COMMA))
                    next();
                else
                    break;
            }
            if (at(SEMICOLON))
                next();
            else
                error(curToken.line, curToken.col, "expected ;, got "+curToken.text);
            statements.add(statement);
            return statement(statements);
        }
        else if (at(IF)) {
            // if (expr) statement [else statement]
            Statement statement = new Statement(StatementType.IF, curToken.line, curToken.col);
            next();
            if (at(LPAREN)) {
                next();
                Expr condition = expression();
                if (!at(RPAREN)) {
                    error(curToken.line, curToken.col, "expected ), got "+curToken.text);
                    return false;
                }
                else {
                    next();
                }
                statement.exprs = new LinkedList<>();
                statement.exprs.add(condition);
                statement.statements = new LinkedList<>();
                if (!statement(statement.statements))
                    return false;
                if (at(ELSE)) {
                    next();
                    statement(statement.statements);
                }
                statements.add(statement);
                return true;
            }
            else {
                error(curToken.line, curToken.col, "expected (, got "+curToken.text);
                return false;
            }
        }
        else if (at(WHILE)) {
            // while ( expr ) statement
            Statement statement = new Statement(StatementType.WHILE, curToken.line, curToken.col);
            next();
            if (at(LPAREN)) {
                next();
                Expr condition = expression();
                if (condition == null) return false;
                if (at(RPAREN)) {
                    next();
                }
                else {
                    error(curToken.line, curToken.col, "expected ), got "+curToken.text);
                    return false;
                }
                statement.exprs = new LinkedList<>();
                statement.exprs.add(condition);
                statement.statements = new LinkedList<>();
                statements.add(statement);
                return statement(statement.statements);
            }
            else {
                error(curToken.line, curToken.col, "expected (, got "+curToken.text);
                return false;
            }
        }
        else if (at(SWITCH)) {
            // switch ( expr ) statement
            Statement statement = new Statement(StatementType.SWITCH, curToken.line, curToken.col);
            statement.exprs = new LinkedList<>();
            statement.statements = new LinkedList<>();
            next();
            statement.exprs.add(expression());
            statements.add(statement);            
            return statement(statement.statements);
        }
        else if (at(CASE)) {
            // case constant :
            Statement statement = new Statement(StatementType.CASE, curToken.line, curToken.col);
            statement.exprs = new LinkedList<>();
            next();
            if (at(NUMBER)) {
                statement.exprs.add(new Expr(ExprType.NUMBER, curToken.number, curToken.line, curToken.col));
                next();
            }
            else {
                error(curToken.line, curToken.col, "expected constant, got "+curToken.text);
            }
            if (at(COLON))
                next();
            else
                error(curToken.line, curToken.col, "expected :, got "+curToken.text);
            statements.add(statement);
            return true;
        }
        else if (at(BREAK)) {
            // break
            Statement statement = new Statement(StatementType.BREAK, curToken.line, curToken.col);
            next();
            if (at(SEMICOLON))
                next();
            else
                error(curToken.line, curToken.col, "expected ;, got "+curToken.text);
            statements.add(statement);
            return true;
        }
        else if (at(RETURN)) {
            // return
            Statement statement = new Statement(StatementType.RETURN, curToken.line, curToken.col);
            next();
            if (at(SEMICOLON)) {
                // empty return
                next();
                statements.add(statement);
                return true;
            }
            else {
                // return ( expr )
                if (at(LPAREN))
                    next();
                else
                    error(curToken.line, curToken.col, "expected (, got "+curToken.text);
                Expr expr = expression();
                if (at(RPAREN))
                    next();
                else
                    error(curToken.line, curToken.col, "expected ), got "+curToken.text);
                
                statement.exprs = new LinkedList<>();
                statement.exprs.add(expr);
                if (at(SEMICOLON))
                    next();
                else
                    error(curToken.line, curToken.col, "expected ;, got "+curToken.text);
                statements.add(statement);
                return true;
            }
        }
        else if (at(GOTO)) {
            // goto label
            Statement statement = new Statement(StatementType.GOTO, curToken.line, curToken.col);
            next();
            if (at(NAME)) {
                statement.name = curToken.text;
                next();
            }
            else {
                error(curToken.line, curToken.col, "expected label, got "+curToken.text);
            }
            if (at(SEMICOLON))
                next();
            else
                error(curToken.line, curToken.col, "expected ;, got "+curToken.text);
            statements.add(statement);
            return true;
        }        
        else if (at(NAME)) {
            String name = curToken.text;
            Token nameToken = curToken;
            next();
            if (at(COLON)) {
                // label
                next();
                Statement statement = new Statement(StatementType.LABEL, curToken.line, curToken.col);
                statement.name = name;
                statements.add(statement);
                statement(statements);
                return true;
            }
            else {
                // expression as statement
                lastToken = curToken;
                curToken = nameToken;
                Statement statement = new Statement(StatementType.EXPR, curToken.line, curToken.col);
                statement.exprs = new LinkedList<>();
                statement.exprs.add(expression());
                if (at(SEMICOLON))
                    next();
                else
                    error(curToken.line, curToken.col, "expected ;, got "+curToken.text);
                statements.add(statement);
                return true;
            }
        }
        else {
            // expression as statement
            Statement statement = new Statement(StatementType.EXPR, curToken.line, curToken.col);
            statement.exprs = new LinkedList<>();
            statement.exprs.add(expression());
            if (at(SEMICOLON))
                next();
            else
                error(curToken.line, curToken.col, "expected ;, got "+curToken.text);
            statements.add(statement);
            return true;
        }
    }
    
    private Expr expression() {
        Expr expr = expressionCond();
        // expr = expr, expr =* expr, etc...
        while (TokenType.assignmentTypes.contains(curToken.type)) {
            int l = curToken.line;
            int c = curToken.col;
            TokenType op = curToken.type;
            next();
            expr = new Expr(ExprType.fromTokenType(op), expr, expression(), l, c);
        }
        return expr;
    }
    
    private Expr expressionCond() {
        Expr expr = expressionOr();
        if (at(ASK)) {
            // expr ? expr : expr
            int l = curToken.line;
            int c = curToken.col;
            next();
            Expr expr1 = expressionOr();
            if (at(COLON)) {
                next();
                Expr expr2 = expressionCond();
                expr = new Expr(ExprType.COND, expr, expr1, expr2, l, c);
            }
            else {
                error(curToken.line, curToken.col, "expected :, got "+curToken.text);
            }
        }
        return expr;
    }

    private Expr expressionOr() {
        List<Expr> exprs = new LinkedList<>();
        List<TokenType> ops = new LinkedList<>();
        exprs.add(expressionAnd());
        while (at(OR)) {
            // expr | expr
            ops.add(curToken.type);
            next();
            exprs.add(expressionAnd());
        }
        Expr expr = exprs.remove(0);
        while (!exprs.isEmpty()) {
            expr = new Expr(ExprType.fromTokenType(ops.remove(0)), expr, exprs.remove(0), expr.line, expr.col); 
        }
        return expr;
    }

    private Expr expressionAnd() {
        List<Expr> exprs = new LinkedList<>();
        List<TokenType> ops = new LinkedList<>();
        exprs.add(expressionEquals());
        while (at(AND)) {
            // expr & expr
            ops.add(curToken.type);
            next();
            exprs.add(expressionEquals());
        }
        Expr expr = exprs.remove(0);
        while (!exprs.isEmpty()) {
            expr = new Expr(ExprType.fromTokenType(ops.remove(0)), expr, exprs.remove(0), expr.line, expr.col); 
        }
        return expr;
    }

    private Expr expressionEquals() {
        List<Expr> exprs = new LinkedList<>();
        List<TokenType> ops = new LinkedList<>();
        exprs.add(expressionRel());
        while (at(EQUALS, NEQUALS)) {
            // expr == expr
            // expr != expr
            ops.add(curToken.type);
            next();
            exprs.add(expressionRel());
        }
        Expr expr = exprs.remove(0);
        while (!exprs.isEmpty()) {
            expr = new Expr(ExprType.fromTokenType(ops.remove(0)), expr, exprs.remove(0), expr.line, expr.col); 
        }
        return expr;
    }

    private Expr expressionRel() {
        List<Expr> exprs = new LinkedList<>();
        List<TokenType> ops = new LinkedList<>();
        exprs.add(expressionShift());
        while (at(LT, LE, GT, GE)) {
            // expr < expr
            // expr <= expr
            // expr > expr
            // expr >= expr
            ops.add(curToken.type);
            next();
            exprs.add(expressionShift());
        }
        Expr expr = exprs.remove(0);
        while (!exprs.isEmpty()) {
            expr = new Expr(ExprType.fromTokenType(ops.remove(0)), expr, exprs.remove(0), expr.line, expr.col); 
        }
        return expr;
    }

    private Expr expressionShift() {
        List<Expr> exprs = new LinkedList<>();
        List<TokenType> ops = new LinkedList<>();
        exprs.add(expressionAdd());
        while (at(LSHIFT, RSHIFT)) {
            // expr << expr
            // expr >> expr
            ops.add(curToken.type);
            next();
            exprs.add(expressionAdd());
        }
        Expr expr = exprs.remove(0);
        while (!exprs.isEmpty()) {
            expr = new Expr(ExprType.fromTokenType(ops.remove(0)), expr, exprs.remove(0), expr.line, expr.col); 
        }
        return expr;
    }

    private Expr expressionAdd() {
        List<Expr> exprs = new LinkedList<>();
        List<TokenType> ops = new LinkedList<>();
        exprs.add(expressionMul());
        while (at(PLUS, MINUS)) {
            // expr + expr
            // expr - expr
            ops.add(curToken.type);
            next();
            exprs.add(expressionMul());
        }
        Expr expr = exprs.remove(0);
        while (!exprs.isEmpty()) {
            expr = new Expr(ExprType.fromTokenType(ops.remove(0)), expr, exprs.remove(0), expr.line, expr.col); 
        }
        return expr;
    }

    private Expr expressionMul() {
        List<Expr> exprs = new LinkedList<>();
        List<TokenType> ops = new LinkedList<>();
        exprs.add(expressionUnary());
        while (at(STAR, DIV, MOD)) {
            // expr * expr
            // expr / expr
            // expr % expr
            ops.add(curToken.type);
            next();
            exprs.add(expressionUnary());
        }
        Expr expr = exprs.remove(0);
        while (!exprs.isEmpty()) {
            expr = new Expr(ExprType.fromTokenType(ops.remove(0)), expr, exprs.remove(0), expr.line, expr.col); 
        }
        return expr;
    }

    private Expr expressionUnary() {
        int l = curToken.line;
        int c = curToken.col;
        if (at(STAR)) {
            // * expr
            next();
            return new Expr(ExprType.DEREF, expressionUnary(), l, c);
        }
        else if (at(AND)) {
            // & expr
            next();
            return new Expr(ExprType.REF, expressionUnary(), l, c);
        }
        else if (at(MINUS)) {
            // - expr
            next();
            return new Expr(ExprType.NEG, expressionUnary(), l, c);
        }
        else if (at(NOT)) {
            // ! expr
            next();
            return new Expr(ExprType.NOT, expressionUnary(), l, c);
        }
        else if (at(INC)) {
            // ++ expr
            next();
            return new Expr(ExprType.PREINC, expressionUnary(), l, c);
        }
        else if (at(DEC)) {
            // -- expr
            next();
            return new Expr(ExprType.PREDEC, expressionUnary(), l, c);
        }
        else {
            Expr expr = expressionPrimary();
            l = curToken.line;
            c = curToken.col;
            if (at(INC)) {
                // expr ++
                next();
                return new Expr(ExprType.POSTINC, expr, l, c);
            }
            else if (at(DEC)) {
                // expr --
                next();
                return new Expr(ExprType.POSTDEC, expr, l, c);
            }
            else {
                return expr;
            }
        }
    }

    private Expr expressionPrimary() {
        Expr expr = null;
        int l = curToken.line;
        int c = curToken.col;
        if (at(LPAREN)) {
            // ( expr )
            next();
            expr = new Expr(ExprType.GROUP, expression(), l, c);
            if(at(RPAREN))
                next(); 
            else
                error(curToken.line, curToken.col, "expected ), got "+curToken.text);
        }
        else if (at(NAME)) {
            // name
            expr = new Expr(ExprType.NAME, curToken.text, l, c);
            next();
        }
        else if (at(NUMBER)) {
            // number            
            expr = new Expr(ExprType.NUMBER, curToken.number, l, c);
            next();
        }
        else if (at(STRING)) {
            // string
            expr = defineString(l, c, curToken);
            next();
        }
        else {
            error(curToken.line, curToken.col, "syntax error");
            next();
            return null;
        }

        while (at(LBRACKET, LPAREN)) {
            if (at(LBRACKET)) {
                // expr [ expr ]
                next();
                expr = new Expr(ExprType.INDEX, expr, expression(), l, c);
                if (at(RBRACKET))
                    next();
                else
                    error(curToken.line, curToken.col, "expected ], got "+curToken.text);
            }
            else if (at(LPAREN)) {
                // expr ( expr , expr, ... )
                next();
                expr = new Expr(ExprType.APP, expr, l, c);
                expr.args = new LinkedList<>();
                if (!at(RPAREN)) while (!at(EOF)) {
                    Expr arg = expression();
                    if (arg != null) expr.args.add(arg);
                    if (at(COMMA))
                        next();
                    else
                        break;
                }
                if (at(RPAREN))
                    next();
                else
                    error(curToken.line, curToken.col, "expected ), got "+curToken.text);
            }
        }
        return expr;
    }
    
    private Expr defineString(int line, int col, Token token) {
        label++;
        String name = "$I"+label;
        Expr expr = new Expr(ExprType.STRING, name, line, col);
        Definition definition = new Definition(line, col);
        definition.name = name;
        definition.isString = true;
        definition.ivals = new LinkedList<>();
        for (int i : token.string) {
            definition.ivals.add(new Token(NUMBER, i, token.line, token.col));
        }
        definition.size = definition.ivals.size();
        definitions.add(definition);
        return expr;
    }

    private void next() {
        if (lastToken != null) {
            curToken = lastToken;
            lastToken = null;            
        }
        else {
            curToken = scanner.next();
        }
    }
    
    private boolean at(TokenType ... tokenTypes) {
        for (TokenType tokenType : tokenTypes) {
            if (curToken.type == tokenType)
                return true;
        }
        return false;
    }
    
    private void error(int line, int col, String text) {
        errorHandler.error(line, col, text);
    }
}
