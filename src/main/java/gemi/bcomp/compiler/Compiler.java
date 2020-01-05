package gemi.bcomp.compiler;

import java.io.FileInputStream;
import java.io.PrintStream;
import java.util.List;

import gemi.bcomp.codegen.CodeGenerator;
import gemi.bcomp.parser.Definition;
import gemi.bcomp.parser.Parser;
import gemi.bcomp.scanner.Scanner;
import gemi.bcomp.utilities.ErrorHandler;

public class Compiler {

    private PrintStream out = null;
    private ErrorHandler errorHandler;
    
    public Compiler(PrintStream out, ErrorHandler errorHandler) {
        this.out = out;
        this.errorHandler = errorHandler;
    }
    
    public void compile(List<Definition> definitions) {
        CodeGenerator codeGenerator = new CodeGenerator(errorHandler);
        codeGenerator.definitions(definitions);
        codeGenerator.outputSymbols(out);
        codeGenerator.outputCode(out);
    }
    
    public static void warning(String msg) {
        System.err.println("b-comp: warning: "+msg);
    }

    public static void error(String msg) {
        System.err.println("b-comp: error: "+msg);
        System.exit(1);
    }

    public static void main(String[] args) throws Exception {
        String filename = null;
        String outname = null;

        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-o")) {
                if (args[i].length() > 2)
                    outname = args[i].substring(2); 
                else if (i < args.length-1)
                    outname = args[++i];
                else
                    error("missing filename after -o");
            }
            else if (args[i].equals("--help") || args[i].equals("-h")) {
                System.out.println("Usage: b-comp [-o FILE] FILE");
                System.exit(0);
            }
            else if (args[i].startsWith("-")) {
                warning("unknown option '"+args[i]+"'");
            }
            else if (filename == null) {
                filename = args[i];
            }
            else {
                warning("unknown option '"+args[i]+"'");
            }
        }
        
        if (filename == null) error("no input file");
        
        if (!filename.endsWith(".b")) error("invalid input file '"+filename+"'");
        
        try {
            ErrorHandler errorHandler = new ErrorHandler(filename, System.err);
            FileInputStream in = new FileInputStream(filename);
            Scanner scanner = new Scanner(in, errorHandler);
            Parser parser = new Parser(scanner, errorHandler);
            List<Definition> definitions = parser.parse();
            if (errorHandler.errorCount > 0) System.exit(1);
            if (outname == null) outname = filename.substring(0, filename.length()-"b".length())+"bs";
            PrintStream out = new PrintStream(outname);
            Compiler compiler = new Compiler(out, errorHandler);
            compiler.compile(definitions);
            out.close();
            if (errorHandler.errorCount > 0) System.exit(1);
        }
        catch (Exception e) {
            error("cannot open input file '"+filename+"'");
        }
    }
}
