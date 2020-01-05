package gemi.bcomp.codegen;

import static gemi.bcomp.codegen.CodeUtilities.collectCases;
import static gemi.bcomp.parser.ExprType.*;
import static gemi.bcomp.utilities.Utilities.isNotEmpty;

import java.io.PrintStream;
import java.util.*;
import java.util.Map.Entry;

import gemi.bcomp.parser.*;
import gemi.bcomp.parser.Binding.BindingType;
import gemi.bcomp.scanner.Token;
import gemi.bcomp.scanner.TokenType;
import gemi.bcomp.utilities.ErrorHandler;

public class CodeGenerator {
    
    private StringBuilder code = new StringBuilder();
    private Set<String> exports = new TreeSet<>();
    private Set<String> externals = new TreeSet<>();
    private Set<String> internals = new TreeSet<>();
    private Map<String,Integer> data = new TreeMap<>();
    private Map<String,Label> labels = new HashMap<>();
    private int label = 1;

    private String sp   = "SP";
    private String fp   = "FP";
    private String acc  = "R0";
    private String reg1 = "R1";
    
    private ErrorHandler errorHandler;
    
    public CodeGenerator(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }
    
    public void outputCode(PrintStream out) {
        out.println(".text");
        out.print(code);
    }

    public void outputSymbols(PrintStream out) {
        for (String external : externals) {
            out.println(".external "+external);
        }
        for (String export : exports) {
            out.println(".export "+export);
        }
        for (String internal : internals) {
            out.println(".internal "+internal);
        }
        for (Entry<String,Integer> entry : data.entrySet()) {
            out.println(".data "+entry.getKey()+" "+entry.getValue());
        }
    }
    
    public void definitions(List<Definition> definitions) {
        Set<String> names = new HashSet<>();
        label("$init");
        exports.add("$init");
        for (Definition definition : definitions) {
            if (!definition.isFunction) {
                if (names.contains(definition.name))
                    error(definition.line, definition.col, "duplicate definition: "+definition.name);
                names.add(definition.name);
                init(definition);
            }
        }
        instr("RET");
        for (Definition definition : definitions) {
            if (definition.isFunction) {
                if (names.contains(definition.name))
                    error(definition.line, definition.col, "duplicate definition: "+definition.name);
                names.add(definition.name);
                function(definition);
            }
        }
    }
    
    private void init(Definition definition) {
        if (definition.isVector) {
            int alloc = definition.size+1;
            data.put(definition.name, (int)alloc);
            comment("external variable "+definition.name);
            // initialize pointer
            instr("LAD", definition.name, acc);
            instr("MOV", acc, reg1);
            instr("ADD", lit(1), reg1);
            instr("MOV", reg1, ind(acc));
            instr("MOV", acc, reg1);
            if (definition.ivals != null && definition.ivals.size() > 0) {
                // initialize vector values
                for (Token ival : definition.ivals) {
                    instr("ADD", lit(1), reg1);
                    if (ival.type == TokenType.NUMBER) {
                        movNumber(ival.number, acc);
                        instr("MOV", acc, ind(reg1));
                    }
                    else if (ival.type == TokenType.NAME) {
                        instr("LAD", ival.text, acc);
                        instr("MOV", acc, ind(reg1));
                    }
                    else if (ival.type == TokenType.STRING) {
                        instr("LAD", ival.text, acc);
                        instr("MOV", acc, ind(reg1));
                    }
                }
            }
        }
        else if (definition.isString) {
            comment("string "+definition.name);
            instr("LAD", definition.name, acc);
            instr("MOV", acc, reg1);
            // initialize string
            int n = 0;
            for (Token ival : definition.ivals) {
                movNumber(ival.number, acc);
                instr("MOV", acc, ind(reg1, n));
                n++;
            }
            data.put(definition.name, definition.ivals.size());
        }
        else {
            int alloc = 1;
            if (definition.ivals != null && definition.ivals.size() > 0) {
                alloc = definition.ivals.size();
                comment("external variable "+definition.name);
                instr("LAD", definition.name, acc);
                instr("MOV", acc, reg1);
                int n = 0;
                for (Token ival : definition.ivals) {
                    if (ival.type == TokenType.NUMBER) {
                        movNumber(ival.number, acc);
                        instr("MOV", acc, ind(reg1, n));
                    }
                    else if (ival.type == TokenType.NAME) {
                        instr("LAD", ival.text, acc);
                        instr("MOV", acc, ind(reg1));
                    }
                    else if (ival.type == TokenType.STRING) {
                        instr("LAD", ival.text, acc);
                        instr("MOV", acc, ind(reg1));
                    }
                    n++;
                }
            }
            data.put(definition.name, alloc);
        }
        if (definition.name.startsWith("$")) {
            internals.add(definition.name);
        }
        else {
            if (exports.contains(definition.name)) {
                error(definition.line, definition.col, "duplicate definition: "+definition.name);
            }
            exports.add(definition.name);
            externals.add(definition.name);
        }
    }

    private void function(Definition definition) {
        labels.clear();
        exports.add(definition.name);
        label(definition.name);
        Bindings bindings = new Bindings(null);
        int offset = 3;
        for (String formal : definition.formals) {
            if (!bindings.bind(formal, BindingType.FORMAL, offset))
                error(definition.line, definition.col, "duplicate declaration: "+formal);
            offset++;
        }
        int s = Statement.allocateStack(definition.statements);
        if (s > 0) instr("SUB", lit(s), sp);
        for (Statement statement : definition.statements) {
            statement(statement, bindings);
        }
        bindings = bindings.next;
        instr("RET");
        for (Label l : labels.values()) {
            if (!l.defined) {
                error(0, 0, "label "+l.name+" not defined");
            }
        }
    }

    private void statement(Statement statement, Bindings bindings) {
        switch (statement.type) {
        case AUTO:
            // auto name [ival], ... ;
            for (Expr expr : statement.exprs) {
                if(!bindings.bind(expr.name, BindingType.AUTO, bindings.offset))
                    error(expr.line, expr.col, "duplicate declaration: "+expr.name);
                bindings.offset--;
                if (expr.expr1 != null) {
                    rvalue(expr.expr1, bindings);
                    instr("MOV", acc, ind(fp+bindings.lookupShallow(expr.name).offset));
                }
            }
            break;
        case EXTRN:
            // extrn name, ... ;
            for (Expr expr : statement.exprs) {
                if (!bindings.bind(expr.name, BindingType.EXTERNAL, 0))
                    error(expr.line, expr.col, "duplicate declaration: "+expr.name);
                externals.add(expr.name);
            }
            break;
        case COMPOUND:
            // { ... }
            bindings = new Bindings(bindings);
            for (Statement st : statement.statements) {
                statement(st, bindings);
            }
            break;
        case EXPR:
            // expr ;
            rvalue(statement.exprs.get(0), bindings, null, null);
            break;
        case IF:
            // if ( expr ) statement [ else statement ]
            boolean withElse = statement.statements.size() == 2;
            if (withElse) {
                String endLabel = L(label++);
                String falseLabel = L(label++);
                comment("if");
                rvalue(statement.exprs.get(0), bindings, null, falseLabel);
                comment("then");
                statement(statement.statements.get(0), bindings);
                instr("JRL", endLabel);
                label(falseLabel);
                comment("else");
                statement(statement.statements.get(1), bindings);
                comment("end if");
                label(endLabel);
            }
            else {
                String falseLabel = L(label++);
                comment("if");
                rvalue(statement.exprs.get(0), bindings, null, falseLabel);
                comment("then");
                statement(statement.statements.get(0), bindings);
                label(falseLabel);
                comment("end if");
            }
            break;
        case RETURN:
            // return [ expr ]
            if (statement.exprs != null) rvalue(statement.exprs.get(0), bindings);
            instr("RET");
            break;
        case WHILE:
            // while ( expr ) statement
            String startLabel = L(label++);
            String endLabel = L(label++);
            Expr condition = statement.exprs.get(0);
            label(startLabel);
            rvalue(condition, bindings, null, endLabel);
            String oldBreakLabel = bindings.breakLabel;
            bindings.breakLabel = endLabel;
            if (statement.statements != null) for (Statement st: statement.statements) {
                statement(st, bindings);
            }
            instr("JRL", startLabel);
            label(endLabel);
            bindings.breakLabel = oldBreakLabel;
            break;
        case BREAK:
            // break
            String breakLabel = bindings.getBreakLabel();
            if (breakLabel != null)
                instr("JRL", breakLabel);
            else
                error(statement.line, statement.col, "break only allowed in switch and while statements");
            break;
        case SWITCH:
            // switch ( expr ) statement
            comment("select value of switch statement, in R1");
            rvalue(statement.exprs.get(0), bindings);
            instr("MOV", acc, reg1);
            List<Statement> cases = new LinkedList<>();
            oldBreakLabel = bindings.breakLabel;
            bindings.breakLabel = L(label++); 
            if (isNotEmpty(statement.statements)) {
                for (Statement st : statement.statements) {
                    collectCases(st, cases);
                }
            }
            for (Statement st : cases) {
                st.name = L(label++);
                int constant = st.exprs.get(0).number;
                comment("compare with case "+constant);
                instr("MOV", lit(st.exprs.get(0).number), acc);
                instr("SUB", reg1, acc);
                instr("BEQ", st.name);
            }
            instr("JRL", bindings.breakLabel);
            if (isNotEmpty(statement.statements)) {
                for (Statement st : statement.statements) {
                    statement(st, bindings);
                }
            }
            label(bindings.breakLabel);
            comment("end switch");
            bindings.breakLabel = oldBreakLabel;
            break;
        case CASE:            
            // case constant : 
            label(statement.name);
            comment("case "+statement.exprs.get(0).number);
            break;
        case LABEL:
            // label :
            Label l = labels.get(statement.name);
            if (l == null) {
                l = new Label();
                l.name = statement.name;
                l.internal = L(label++);
                labels.put(statement.name, l);
            }
            l.defined = true;
            label(l.internal);
            break;
        case GOTO:
            // goto label ;
            Binding binding = bindings.lookupDeep(statement.name);
            if (binding != null) {
                rvalue(new Expr(ExprType.NAME, statement.name, statement.line, statement.col), bindings);
                instr("JMP", acc);
            }
            else {
                l = labels.get(statement.name);
                if (l == null) {
                    label++;
                    l = new Label();
                    l.name = statement.name;
                    l.internal = L(label);
                    labels.put(statement.name, l);
                }
                instr("JRL", l.internal);
            }
            break;
        case NULL:
            // ;
            break;
        default:
            error(statement.line, statement.col, statement.type+" not yet implemented");
        }
    }

    /**
     * Leaves the result in R0 (acc).
     */
    private void rvalue(Expr expr, Bindings bindings) {
        rvalue(expr, bindings, null, null);
    }
    
    /**
     * Leaves the result in R0 (acc).
     * If trueLabel != null, branches to trueLabel, if the result != 0.
     * If falseLabel != null, branches to falseLabel, if the result == 0.
     */
    private void rvalue(Expr expr, Bindings bindings, String trueLabel, String falseLabel) {
        String l0, l1;
        switch (expr.op) {
        case AND:
            // expr & expr
            if (trueLabel != null && falseLabel != null) {
                rvalue(expr.expr1, bindings, null, falseLabel);
                rvalue(expr.expr2, bindings, trueLabel, falseLabel);
            }
            else if (trueLabel != null) {
                rvalue(expr.expr1, bindings);
                instr("PUSH", acc);
                rvalue(expr.expr2, bindings);
                instr("POP", reg1);
                instr("AND", reg1, acc);
                branchTrueFalse(trueLabel, null);
            }
            else if (falseLabel != null) {
                rvalue(expr.expr1, bindings, null, falseLabel);
                rvalue(expr.expr2, bindings, null, falseLabel);
            }
            else {
                rvalue(expr.expr1, bindings);
                instr("PUSH", acc);
                rvalue(expr.expr2, bindings);
                instr("POP", reg1);
                instr("AND", reg1, acc);
                l0 = L(label++);
                l1 = L(label++);
                branchTrueFalse(null, l0);
                instr("MOV", lit(1), acc);
                instr("JRL", l1);
                label(l0);
                instr("MOV", lit(0), acc);
                label(l1);
            }
            break;
        case APP:
            // expr ( expr , ... )
            comment("function call");
            if (expr.expr1.op == NAME && bindings.lookupDeep(expr.expr1.name) == null) {
                // consider undefined function name as external
                externals.add(expr.expr1.name);
                bindings.bind(expr.expr1.name, BindingType.EXTERNAL, 0);
            }            
            int nargs = expr.args.size();
            while (!expr.args.isEmpty()) {
                comment("push argument");
                rvalue(expr.args.remove(expr.args.size()-1), bindings);
                instr("PUSH", acc);
            }
            comment("push number of arguments");
            instr("MOV", lit(nargs), acc);
            instr("PUSH", acc);
            
//            if (expr.expr1.op == NAME && bindings.lookupDeep(expr.expr1.name).type == BindingType.EXTERNAL) {
//                instr("CALL", expr.expr1.name);
//            }
//            else {
                rvalue(expr.expr1, bindings, null, null);
                instr("CALL", acc);
//            }
            comment("adjust stack");
            instr("ADD", lit(nargs+1), sp);
            comment("end function call");
            branchTrueFalse(trueLabel, falseLabel);
            break;
        case ASS:
            // expr = expr
            lvalue(expr.expr1, bindings);
            instr("PUSH", acc);
            rvalue(expr.expr2, bindings);
            instr("POP", reg1);
            instr("MOV", acc, ind(reg1));
            branchTrueFalse(trueLabel, falseLabel); 
            break;
        case ASAND:
            // expr =+ expr
            rvalue(expandAssignment(AND, expr.expr1, expr.expr2), bindings, trueLabel, falseLabel);
            break;
        case ASEQUALS:
            // expr === expr
            rvalue(expandAssignment(EQUALS, expr.expr1, expr.expr2), bindings, trueLabel, falseLabel);
            break;
        case ASDIV:
            // expr =/ expr
            rvalue(expandAssignment(DIV, expr.expr1, expr.expr2), bindings, trueLabel, falseLabel);
            break;
        case ASGE:
            // expr =>= expr
            rvalue(expandAssignment(GE, expr.expr1, expr.expr2), bindings, trueLabel, falseLabel);
            break;
        case ASGT:
            // expr => expr
            rvalue(expandAssignment(GT, expr.expr1, expr.expr2), bindings, trueLabel, falseLabel);
            break;
        case ASLE:
            // expr =<= expr
            rvalue(expandAssignment(LE, expr.expr1, expr.expr2), bindings, trueLabel, falseLabel);
            break;
        case ASLSHIFT:
            // expr =<< expr
            rvalue(expandAssignment(LSHIFT, expr.expr1, expr.expr2), bindings, trueLabel, falseLabel);
            break;
        case ASLT:
            // expr =< expr
            rvalue(expandAssignment(LT, expr.expr1, expr.expr2), bindings, trueLabel, falseLabel);
            break;
        case ASMINUS:
            // expr =- expr
            rvalue(expandAssignment(MINUS, expr.expr1, expr.expr2), bindings, trueLabel, falseLabel);
            break;
        case ASMOD:
            // expr =% expr
            rvalue(expandAssignment(MOD, expr.expr1, expr.expr2), bindings, trueLabel, falseLabel);
            break;
        case ASMUL:
            // expr =* expr
            rvalue(expandAssignment(MUL, expr.expr1, expr.expr2), bindings, trueLabel, falseLabel);
            break;
        case ASNEQUALS:
            // expr =!= expr
            rvalue(expandAssignment(NEQUALS, expr.expr1, expr.expr2), bindings, trueLabel, falseLabel);
            break;
        case ASOR:
            // expr =| expr
            rvalue(expandAssignment(OR, expr.expr1, expr.expr2), bindings, trueLabel, falseLabel);
            break;
        case ASPLUS:
            // expr =+ expr
            rvalue(expandAssignment(PLUS, expr.expr1, expr.expr2), bindings, trueLabel, falseLabel);
            break;
        case ASRSHIFT:
            // expr =>> expr
            rvalue(expandAssignment(RSHIFT, expr.expr1, expr.expr2), bindings, trueLabel, falseLabel);
            break;
        case ASXOR:
            // expr =^ expr
            rvalue(expandAssignment(XOR, expr.expr1, expr.expr2), bindings, trueLabel, falseLabel);
            break;
        case COMPL:
            // ~ expr
            rvalue(expr.expr1, bindings);
            instr("CPL", acc);
            branchTrueFalse(trueLabel, falseLabel); 
            break;
        case DEREF:
            // * expr (dereference);
            rvalue(expr.expr1, bindings);
            instr("MOV", ind(acc), acc);
            branchTrueFalse(trueLabel, falseLabel); 
            break;
        case DIV:
            // expr / expr
            rvalue(expr.expr1, bindings);
            instr("PUSH", acc);
            rvalue(expr.expr2, bindings);
            instr("MOV", acc, reg1);
            instr("POP", acc);
            instr("DIV", reg1, acc);
            branchTrueFalse(trueLabel, falseLabel); 
            break;
        case LSHIFT:
            // expr << expr
            rvalue(expr.expr1, bindings);
            instr("PUSH", acc);
            rvalue(expr.expr2, bindings);
            instr("MOV", acc, reg1);
            instr("POP", acc);
            instr("LSH", reg1, acc); // acc <- acc << reg1
            branchTrueFalse(trueLabel, falseLabel); 
            break;
        case MINUS:
            // expr - expr
            rvalue(expr.expr1, bindings);
            instr("PUSH", acc);
            rvalue(expr.expr2, bindings);
            instr("POP", reg1);
            instr("EXCH", reg1, acc);
            instr("SUB", reg1, acc); // acc <- acc - reg1
            branchTrueFalse(trueLabel, falseLabel); 
            break;
        case MOD:
            // expr % expr
            rvalue(expr.expr1, bindings);
            instr("PUSH", acc);
            rvalue(expr.expr2, bindings);
            instr("MOV", acc, reg1);
            instr("POP", acc);
            instr("MOD", reg1, acc); // acc <- acc % reg1
            branchTrueFalse(trueLabel, falseLabel); 
            break;
        case MUL:            
            // expr * expr
            rvalue(expr.expr1, bindings);
            instr("PUSH", acc);
            rvalue(expr.expr2, bindings);
            instr("POP", reg1);
            instr("MUL", reg1, acc); // acc <- acc * reg1
            branchTrueFalse(trueLabel, falseLabel); 
            break;
        case NAME:
            // name
            Binding b = bindings.lookupDeep(expr.name);
            if (b == null)
                error(expr.line, expr.col, "name not declared: "+expr.name);                
            else if (b.type == BindingType.AUTO)
                instr("MOV", ind(fp, b.offset), acc);
            else if (b.type == BindingType.EXTERNAL)
                instr("MOV", b.name, acc);
            else if (b.type == BindingType.FORMAL)
                instr("MOV", ind(fp, b.offset), acc);
            else if (b.type == BindingType.INTERNAL)
                error(expr.line, expr.col, "not allowed");
            branchTrueFalse(trueLabel, falseLabel);
            break;
        case NEG:
            // - expr            
            rvalue(expr.expr1, bindings);
            instr("NEG", acc, acc); // acc <- -acc
            branchTrueFalse(trueLabel, falseLabel); 
            break;
        case NOT:
            // ! expr
            rvalue(expr.expr1, bindings);
            instr("NOT", acc);
            branchTrueFalse(trueLabel, falseLabel); // acc <- !acc 
            break;
        case NUMBER:
            // number
            int n = expr.number;
            movNumber(n, acc);
            branchTrueFalse(trueLabel, falseLabel);
            break;
        case STRING:
            // string
            instr("LAD", expr.name, acc);
            branchTrueFalse(trueLabel, falseLabel);
            break;
        case OR:
            // expr | expr
            if (trueLabel != null && falseLabel != null) {
                rvalue(expr.expr1, bindings, trueLabel, null);
                rvalue(expr.expr2, bindings, trueLabel, falseLabel);
            }
            else if (falseLabel != null) {
                rvalue(expr.expr1, bindings);
                instr("PUSH", acc);
                rvalue(expr.expr2, bindings);
                instr("POP", reg1);
                instr("OR", reg1, acc);
                branchTrueFalse(null, falseLabel);
            }
            else if (trueLabel != null) {
                rvalue(expr.expr1, bindings, trueLabel, null);
                rvalue(expr.expr2, bindings, trueLabel, null);
            }
            else {
                rvalue(expr.expr1, bindings);
                instr("PUSH", acc);
                rvalue(expr.expr2, bindings);
                instr("POP", reg1);
                instr("OR", reg1, acc);
                l0 = L(label++);
                l1 = L(label++);
                branchTrueFalse(null, l0);
                instr("MOV", lit(1), acc);
                instr("JRL", l1);
                label(l0);
                instr("MOV", lit(0), acc);
                label(l1);
            }
            break;
        case PLUS:
            // expr + expr
            rvalue(expr.expr1, bindings);
            instr("PUSH", acc);
            rvalue(expr.expr2, bindings);
            instr("POP", reg1);
            instr("ADD", reg1, acc); // acc <- acc + reg1
            branchTrueFalse(trueLabel, falseLabel); 
            break;
        case REF:
            // & expr (reference);
            lvalue(expr.expr1, bindings);
            branchTrueFalse(trueLabel, falseLabel); 
            break;
        case RSHIFT:
            // expr >> expr
            rvalue(expr.expr1, bindings);
            instr("PUSH", acc);
            rvalue(expr.expr2, bindings);
            instr("MOV", acc, reg1);
            instr("POP", acc);
            instr("RSH", reg1, acc); // acc <- acc >> reg1 
            branchTrueFalse(trueLabel, falseLabel); 
            break;
        case XOR:
            // expr ^ expr
            rvalue(expr.expr1, bindings);
            instr("PUSH", acc);
            rvalue(expr.expr2, bindings);
            instr("POP", reg1);
            instr("XOR", reg1, acc); // acc <- acc ^ reg1
            branchTrueFalse(trueLabel, falseLabel); 
            break;
        case EQUALS:
            // expr == expr
            rvalue(expr.expr1, bindings);
            instr("PUSH", acc);
            rvalue(expr.expr2, bindings);
            instr("POP", reg1);
            instr("XOR", reg1, acc);
            if (!branchTrueFalse(falseLabel, trueLabel)) {
                l0 = L(label++);
                l1 = L(label++);
                instr("BNE", l0);
                instr("MOV", lit(0), acc);
                instr("JRL", l1);
                label(l0);
                instr("MOV", lit(1), acc);
                label(l1);
            }
            break;
        case NEQUALS:
            // expr != expr
            rvalue(expr.expr1, bindings);
            instr("PUSH", acc);
            rvalue(expr.expr2, bindings);
            instr("POP", reg1);
            instr("XOR", reg1, acc);
            if (!branchTrueFalse(trueLabel, falseLabel)) {
                l0 = L(label++);
                l1 = L(label++);
                instr("BEQ", l0);
                instr("MOV", lit(0), acc);
                instr("JRL", l1);
                label(l0);
                instr("MOV", lit(1), acc);
                label(l1);
            }
            break;
        case LT:
            // expr < expr
            rvalue(expr.expr1, bindings);
            instr("PUSH", acc);
            rvalue(expr.expr2, bindings);
            instr("POP", reg1);
            instr("SUB", reg1, acc);
            if (trueLabel != null && falseLabel != null) {
                instr("BPS", trueLabel);
                instr("JRL", falseLabel);
            }
            else if (trueLabel != null) {
                instr("BPS", trueLabel);
            }
            else if (falseLabel != null) {
                instr("BNG", falseLabel);
                instr("BNE", falseLabel);
            }
            else {
                l0 = L(label++);
                l1 = L(label++);
                instr("BPS", l0);
                instr("MOV", lit(0), acc);
                instr("JRL", l1);
                label(l0);
                instr("MOV", lit(1), acc);
                label(l1);
            }
            break;
        case LE:
            // expr <= expr
            rvalue(expr.expr1, bindings);
            instr("PUSH", acc);
            rvalue(expr.expr2, bindings);
            instr("POP", reg1);
            instr("SUB", reg1, acc);
            if (trueLabel != null && falseLabel != null) {
                instr("BPS", trueLabel);
                instr("BNE", trueLabel);
                instr("JRL", falseLabel);
            }
            else if (trueLabel != null) {
                instr("BPS", trueLabel);
                instr("BNE", trueLabel);
            }
            else if (falseLabel != null) {
                instr("BNG", falseLabel);
            }
            else {
                l0 = L(label++);
                l1 = L(label++);
                instr("BPS", l0);
                instr("BNE", l0);
                instr("MOV", lit(0), acc);
                instr("JRL", l1);
                label(l0);
                instr("MOV", lit(1), acc);
                label(l1);
            }
            break;
        case GT:
            // expr > expr
            rvalue(expr.expr1, bindings);
            instr("PUSH", acc);
            rvalue(expr.expr2, bindings);
            instr("POP", reg1);
            instr("SUB", reg1, acc);
            if (trueLabel != null && falseLabel != null) {
                instr("BNG", trueLabel);
                instr("JRL", falseLabel);
            }
            else if (trueLabel != null) {
                instr("BNG", trueLabel);
            }
            else if (falseLabel != null) {
                instr("BPS", falseLabel);
                instr("BNE", falseLabel);
            }
            else {
                l0 = L(label++);
                l1 = L(label++);
                instr("BNG", l0);
                instr("MOV", lit(0), acc);
                instr("JRL", l1);
                label(l0);
                instr("MOV", lit(1), acc);
                label(l1);
            }
            break;
        case GE:
            // expr >= expr
            rvalue(expr.expr1, bindings);
            instr("PUSH", acc);
            rvalue(expr.expr2, bindings);
            instr("POP", reg1);
            instr("SUB", reg1, acc);
            if (trueLabel != null && falseLabel != null) {
                instr("BNG", trueLabel);
                instr("BNE", trueLabel);
                instr("JRL", falseLabel);
            }
            else if (trueLabel != null) {
                instr("BNG", trueLabel);
                instr("BNE", trueLabel);
            }
            else if (falseLabel != null) {
                instr("BPS", falseLabel);
            }
            else {
                l0 = L(label++);
                l1 = L(label++);
                instr("BNG", l0);
                instr("BNE", l0);
                instr("MOV", lit(0), acc);
                instr("JRL", l1);
                label(l0);
                instr("MOV", lit(1), acc);
                label(l1);
            }
            break;
        case COND:
            // expr ? expr : expr
            comment("begin conditional: "+expr);
            l0 = L(label++);
            l1 = L(label++);
            rvalue(expr.expr1, bindings, null, l0);
            rvalue(expr.expr2, bindings);
            instr("JRL", l1);
            label(l0);
            rvalue(expr.expr3, bindings);
            label(l1);
            branchTrueFalse(trueLabel, falseLabel);
            comment("end conditional: "+expr);
            break;
        case GROUP:
            // ( expr ) 
            rvalue(expr.expr1, bindings);
            branchTrueFalse(trueLabel, falseLabel);
            break;
        case INDEX:
            // expr [ expr ]
            comment("indexing: "+expr); 
            rvalue(expr.expr1, bindings);
            instr("PUSH", acc);
            rvalue(expr.expr2, bindings);
            instr("POP", reg1);
            instr("ADD", reg1, acc);
            instr("MOV", ind(acc), acc);
            branchTrueFalse(trueLabel, falseLabel);
            break;
        case POSTDEC:
            lvalue(expr.expr1, bindings); // acc contains lvalue(expr)
            instr("MOV", ind(acc), reg1); // reg1 contains rvalue(expr)
            instr("PUSH", reg1);
            instr("SUB", lit(1), reg1); // reg1 contains rvalue(expr)-1
            instr("MOV", reg1, ind(acc));
            instr("POP", acc);
            branchTrueFalse(trueLabel, falseLabel);
            break;
        case POSTINC:            
            lvalue(expr.expr1, bindings); // acc contains lvalue(expr)
            instr("MOV", ind(acc), reg1); // reg1 contains rvalue(expr)
            instr("PUSH", reg1);
            instr("ADD", lit(1), reg1); // reg1 contains rvalue(expr)+1
            instr("MOV", reg1, ind(acc));
            instr("POP", acc);
            branchTrueFalse(trueLabel, falseLabel);
            break;
        case PREDEC:
            // -- expr
            lvalue(expr.expr1, bindings);
            instr("MOV", ind(acc), reg1);
            instr("SUB", lit(1), reg1);
            instr("MOV", reg1, ind(acc));
            instr("MOV", reg1, acc);
            branchTrueFalse(trueLabel, falseLabel);
            break;
        case PREINC:
            // ++ expr
            lvalue(expr.expr1, bindings);
            instr("MOV", ind(acc), reg1);
            instr("ADD", lit(1), reg1);
            instr("MOV", reg1, ind(acc));
            instr("MOV", reg1, acc);
            branchTrueFalse(trueLabel, falseLabel);
            break;
        default:
            error(expr.line, expr.col, "not yet implemented: "+expr.op);
            break;
        }
    }
    
    /**
     * Leaves the address of expr in R0 (acc).
     */
    private void lvalue(Expr expr, Bindings bindings) {
        switch (expr.op) {
        case NAME:
            Binding b = bindings.lookupDeep(expr.name);
            if (b == null) {
                error(expr.line, expr.col, "name not declared: "+expr.name);                
            }
            else if (b.type == BindingType.AUTO) {
                comment("auto lvalue: "+expr);
                instr("MOV", fp, acc);
                if (b.offset > 0)
                    instr("ADD", lit(b.offset), acc);
                else
                    instr("SUB", lit(-b.offset), acc);
            }
            else if (b.type == BindingType.EXTERNAL) {
                comment("external lvalue: "+expr);
                instr("LAD", b.name, acc);
            }
            else if (b.type == BindingType.FORMAL) {
                comment("formal lvalue: "+expr);
                instr("MOV", fp, acc);
                if (b.offset > 0)
                    instr("ADD", lit(b.offset), acc);
                else
                    instr("SUB", lit(-b.offset), acc);
            }
            else if (b.type == BindingType.INTERNAL) {
                error(expr.line, expr.col, "not yet implemented: "+expr.op);
            }
            break;
        case DEREF:
            rvalue(expr.expr1, bindings);
            break;
        case INDEX:
            comment("indexing: "+expr); 
            rvalue(expr.expr1, bindings);
            instr("PUSH", acc);
            rvalue(expr.expr2, bindings);
            instr("POP", reg1);
            instr("ADD", reg1, acc);
            break;
        default:
            error(expr.line, expr.col, "no lvalue");
            break;
        }
    }
    
    private void movNumber(int n, String reg) {
        if ((n & 0xFF000000) != 0) {
            // number does not fit into 24 bits available
            instr("MOV", lit((n&0xFFFFFF00)>>8), reg);
            instr("LSH", lit(8), reg);
            instr("OR", lit(n&0xFF), reg);
        }
        else {
            instr("MOV", lit(n), reg);
        }
    }
    
    private boolean branchTrueFalse(String trueLabel, String falseLabel) {
        if (trueLabel != null) instr("BEQ", trueLabel);
        if (falseLabel != null) instr("BNE", falseLabel);
        return trueLabel != null || falseLabel != null;
    }
    
    private String ind(String arg) {
        return "["+arg+"]";
    }

    private String ind(String arg, int offset) {
        if (offset == 0)
            return "["+arg+"]";
        else if (offset < 0)
            return "["+arg+"-"+(-offset)+"]";
        else
            return "["+arg+"+"+offset+"]";
    }

    private String lit(int n) {
        return "#"+n;
    }

    private void comment(Object text) {
        code.append(String.format("%16s// %s\n", "", text.toString()));
    }
    
    private void instr(String ins, String ... args) {
        code.append(String.format("%16s%-5s", "", ins));
        if (args.length > 0) {
            code.append(args[0]);
            if (args.length > 1) {
                code.append(",");
                code.append(args[1]);
            }
        }
        code.append("\n");
    }

    private void label(String label) {
        code.append(String.format("%s:\n", label));
    }

    private String L(int labelno) {
        return "$L"+labelno;
    }
    
    /**
     * Convert expr1 =OP expr2 to expr1 = expr1 OP expr2.
     * This is lazy, =OP should be implemented more directly. 
     */
    private Expr expandAssignment(ExprType op, Expr expr1, Expr expr2) {
        return new Expr(ASS, expr1, new Expr(op, expr1, expr2, expr1.line, expr1.col), expr1.line, expr1.col);
    }
    
    private void error(int line, int col, String text) {
        errorHandler.error(line, col, text);
    }
    
    public static class Label {
        public String  name     = null;
        public String  internal = null;
        public boolean defined  = false;
    }
}
