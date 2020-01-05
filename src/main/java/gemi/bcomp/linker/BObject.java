package gemi.bcomp.linker;

import static gemi.bcomp.utilities.Utilities.hex2int;
import static gemi.bcomp.utilities.Utilities.string2int;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class BObject {

    public Map<String,List<Integer>> externalRef = new HashMap<>();
    public Map<String,List<Integer>> internalRef = new HashMap<>();
    public Map<String,Integer> exports = new HashMap<>();
    public Map<String,Integer> data = new HashMap<>();
    public Map<String,List<Integer>> dataRef = new HashMap<>();
    public Map<String,Integer> internalSymbols = new HashMap<>();
    public Integer init = null;
    public int offset = 0;
    public int[] text = null;
    
    public static BObject load(InputStream in) {
        BObject obj = new BObject();
        String line;
        int n = 0;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith(".external ")) {
                    line = line.substring(".external ".length());
                    int i = line.indexOf(':');
                    if (i > 0) {
                        String name = line.substring(0, i).trim();
                        List<Integer> list = new LinkedList<>();
                        obj.externalRef.put(name, list);
                        for (String ref : line.substring(i+1).trim().split("\\s+")) {
                            list.add(hex2int(ref));
                        }
                    }
                }
                else if (line.startsWith(".internal ")) {
                    line = line.substring(".external ".length());
                    int i = line.indexOf(':');
                    if (i > 0) {
                        String name = line.substring(0, i).trim();
                        List<Integer> list = new LinkedList<>();
                        obj.internalRef.put(name, list);
                        for (String ref : line.substring(i+1).trim().split("\\s+")) {
                            list.add(hex2int(ref));
                        }
                    }
                }
                else if (line.startsWith(".export ")) {
                    line = line.substring(".export ".length());
                    int i = line.indexOf(':');
                    if (i > 0) {
                        String name = line.substring(0, i).trim();
                        String ref = line.substring(i+1).trim();
                        int adr = hex2int(ref);
                        if (name.equals("$init"))
                            obj.init = adr;
                        else
                            obj.exports.put(name, adr);
                    }
                    else {
                        obj.exports.put(line.trim(), 0);
                    }
                }
                else if (line.startsWith(".data ")) {
                    line = line.substring(".data ".length());
                    int i = line.indexOf(':');
                    if (i > 0) {
                        String name = line.substring(0, i).trim();
                        int size = string2int(line.substring(i+1).trim());
                        obj.data.put(name, size);
                    }
                }
                else if (line.startsWith(".text ")) {
                    int textsize = Integer.parseInt(line.substring(".text ".length()).trim());
                    obj.text = new int[textsize];
                }
                else if (line.length() == 8) {
                    if (n < obj.text.length) {
                        int word = hex2int(line);
                        obj.text[n] = word;
                        n++;
                    }
                }
            }
            return obj;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
