package gemi.bcomp.parser;

public class Binding {

    public enum BindingType {
        AUTO,
        FORMAL,
        INTERNAL,
        EXTERNAL
    }

    public String name;
    public BindingType type;
    public int offset = 0;
    
    public Binding(String name, BindingType type, int offset) {
        this.name = name;
        this.type = type;
        this.offset = offset;
    }
}
