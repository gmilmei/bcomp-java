package gemi.bcomp.parser;

import java.util.TreeMap;

import gemi.bcomp.parser.Binding.BindingType;

public class Bindings extends TreeMap<String,Binding> {

    private static final long serialVersionUID = 2654570102006967785L;

    public int offset = -1; 
    public String breakLabel = null;
    public Bindings next = null;
    
    public Bindings(Bindings next) {
        this.next = next;
        if (next != null) {
            this.offset = next.offset;
            this.breakLabel = next.breakLabel;
        }
    }

    public boolean bind(String name, BindingType type, int offset) {
        Binding binding = lookupShallow(name);
        if (binding != null && binding.type == type)
            return false;
        binding = new Binding(name, type, offset);
        put(name, binding);
        return true;
    }

    public Binding lookupShallow(String name) {
        return get(name);
    }

    public Binding lookupDeep(String name) {
        Binding binding = get(name);
        if (binding == null && next != null)
            return next.lookupDeep(name);
        else
            return binding;
    }
    
    public String getBreakLabel() {
        if (breakLabel == null && next != null)
            return next.getBreakLabel();
        else
            return breakLabel;
    }
}
