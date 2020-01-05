package gemi.bcomp.codegen;

import static gemi.bcomp.utilities.Utilities.isNotEmpty;

import java.util.List;

import gemi.bcomp.parser.Statement;

public class CodeUtilities {

    public static void collectCases(Statement statement, List<Statement> caseConstants) {
        switch (statement.type) {
        case CASE:
            caseConstants.add(statement);
            break;
        case WHILE:
        case IF:
        case COMPOUND:
            if (isNotEmpty(statement.statements)) {
                for (Statement st : statement.statements) {
                    collectCases(st, caseConstants);
                }
            }
            break;
        default:
        }
    }    
}
