package gemi.bcomp.linker;

import static gemi.bcomp.assembler.Opcodes.CALL_A;
import static gemi.bcomp.utilities.Utilities.addAdr;
import static gemi.bcomp.utilities.Utilities.addOpcode;
import static gemi.bcomp.utilities.Utilities.int2hex;
import static gemi.bcomp.utilities.Utilities.string2int;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.Map.Entry;

public class Linker {

    private PrintStream out;

    public List<BObject> objects = new LinkedList<>();
    public int memsize = 0;
    public int offset = 0;
    public int[] code = new int[1024];
    public int dataoffset = 0;
    public String vmexe = "/usr/bin/b-vm";
    
    public Linker(PrintStream out) {
        this.out = out;
    }
    
    public boolean link() {
        if (!precheck()) return false;
        
        // allocation locations for calls to init functions
        for (BObject obj : objects) {
            if (obj.init != null) {
                addcode(addOpcode(0, CALL_A));
            }
        }
        int init_function = 0;
        for (BObject obj : objects) {
            obj.offset = offset;
            if (obj.init != null) {
                // patch init function
                code[init_function] = addAdr(code[init_function], obj.init+offset);
                init_function++;
            }
            for (int c : obj.text) {
                addcode(c);
            }
        }

        // allocation and resolve references 
        if (!resolve())
            return false;

        return true;
    }
    
    public void output() {
        out.println("#!"+vmexe);
        out.println(".memsize "+int2hex(memsize));
        out.println(".data "+int2hex(dataoffset));
        for (int i = 0; i < offset; i++) {
            out.println(int2hex(code[i]));
        }
    }
    
    /**
     * Adds a word to the code.
     * Returns the location of the word.
     */
    private int addcode(int c) {
        if (offset >= code.length) {
            code = Arrays.copyOf(code, code.length+1024);
        }
        code[offset] = c;
        offset++;
        return offset-1;
    }
    
    private boolean precheck() {
        // check for duplicate exports
        Set<String> names = new HashSet<>();
        for (BObject obj : objects) {
            for (String name : obj.exports.keySet()) {
                if (names.contains(name)) {
                    error("duplicate name: "+name);
                    return false;
                }
                else {
                    names.add(name);
                }
            }
        }
        return true;
    }
    
    private boolean resolve() {
        Map<String,Integer> dataSymbols  = new HashMap<>();
        Map<String,Integer> functionSymbols = new HashMap<String,Integer>();

        // get offsets to function functions symbols
        for (BObject obj : objects) {
            for (Entry<String,Integer> export : obj.exports.entrySet()) {
                String name = export.getKey();
                if (obj.data.containsKey(name)) {
                    // this is a data symbol
                    dataSymbols.put(name, 0);
                }
                else {
                    int adr = export.getValue()+obj.offset;
                    // allocate location containing pointer to function
                    adr = addcode(adr);
                    functionSymbols.put(name, adr);
                }
            }
        }
        
        // patch references to function symbols
        for (BObject obj : objects) {
            for (Entry<String,List<Integer>> externalRefs : obj.externalRef.entrySet()) {
                String name = externalRefs.getKey();
                Integer adr = functionSymbols.get(name);
                if (adr == null) {
                    if (!dataSymbols.containsKey(name))
                        error("cannot resolve function symbol "+name);
                }
                else if (externalRefs.getValue() != null) {
                    for (int loc : externalRefs.getValue()) {
                        loc = loc+obj.offset;
                        code[loc] = addAdr(code[loc], adr);
                    }
                }
            }
        }

        dataoffset = offset;
        for (BObject obj : objects) {
            for (Entry<String,Integer> data : obj.data.entrySet()) {
                String name = data.getKey();
                Integer size = obj.data.get(name);
                if (size != null) {
                    if (obj.internalRef.containsKey(name))
                        obj.internalSymbols.put(name, dataoffset);
                    else
                        dataSymbols.put(name, dataoffset);
                    dataoffset += size;
                }
            }
        }

        // patch references to data symbols
        for (BObject obj : objects) {
            // external data symbols
            for (Entry<String,List<Integer>> externalRefs : obj.externalRef.entrySet()) {
                String name = externalRefs.getKey();
                Integer adr = dataSymbols.get(name);
                if (adr == null) {
                    if (!functionSymbols.containsKey(name))
                        error("cannot resolve data symbol "+name);
                }
                else if (externalRefs.getValue() != null) {
                    for (int loc : externalRefs.getValue()) {
                        loc = loc+obj.offset;
                        code[loc] = addAdr(code[loc], adr);
                    }
                }
            }
            // internal data symbols
            for (Entry<String,List<Integer>> internalRefs : obj.internalRef.entrySet()) {
                String name = internalRefs.getKey();
                Integer adr = obj.internalSymbols.get(name);
                if (adr == null) {
                    if (!functionSymbols.containsKey(name))
                        error("cannot resolve internal data symbol "+name);
                }
                else if (internalRefs.getValue() != null) {
                    for (int loc : internalRefs.getValue()) {
                        loc = loc+obj.offset;
                        code[loc] = addAdr(code[loc], adr);
                    }
                }
            }
        }
        
        return true;
    }
    
    public static void error(String msg) {
        System.err.println("b-link: error: "+msg);
        System.exit(1);
    }
    
    public static void main(String[] args) {
        List<String> objnames = new LinkedList<>();
        String exefilename = "b.out";
        int memsize = 1024*1024;
        String bcompLib = System.getProperty("bcomp.lib");
        String vmexe = System.getProperty("bcomp.vmexe");
        if (bcompLib != null) objnames.add(bcompLib);
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-o")) {
                String arg = args[i].substring("-o".length());
                if (arg.length() > 0) {
                    exefilename = arg;
                }
                else if (i < args.length-1) {
                    exefilename = args[i+1];
                    i++;
                }
                else {
                    error("missing argument to option -o");
                }
            }
            else if (args[i].startsWith("-s")) {
                String arg = args[i].substring("-s".length());
                Integer s = null;
                if (arg.length() > 0) {
                    s = string2int(arg);
                    if (s == null || s < 1)
                        error("argument to -s must be a positive integer");
                    else
                        memsize = s*1024*1024;
                }
                else if (i < args.length-1) {
                    s = string2int(args[i+1]);
                    if (s == null || s < 1)
                        error("argument to -s must be a positive integer");
                    else
                        memsize = s*1024*1024;
                    i++;
                }
                else {
                    error("missing argument to option -s");
                }
            }
            else {
                objnames.add(args[i]);
            }
        }
        
        if (objnames.isEmpty()) {
            error("no files");
        }
 
        File exefile = new File(exefilename);
        try (PrintStream out = new PrintStream(exefile)) {
            Linker linker = new Linker(out);
            linker.memsize = memsize;
            if (vmexe != null) linker.vmexe = vmexe;
            for (String objname : objnames) {
                try {
                    BObject obj = BObject.load(new FileInputStream(objname));
                    if (obj == null) error("cannot open file "+objname);
                    linker.objects.add(obj);
                } catch (Exception e) {
                    error("error in reading file "+objname);
                }
            }
            if (linker.link()) {
                linker.output();
                exefile.setExecutable(true);
            }
            else {
                exefile.delete();
            }
        } catch (Exception e) {
            exefile.delete();
        }
    }
}
