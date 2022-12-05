
abstract public class Type {
   
    public Type() {}

    abstract public String toString();
    public String Type;
}

class ErrorType extends Type {
    public String Type = "error";
    public String toString() {
        return "Error";
    }
}

class IntType extends Type {
    public String Type = "int";
    public String toString() {
        return "int";
    }
}


class BoolType extends Type {
    public String Type = "bool";
    public String toString() {
        return "bool";
    }
}

class VoidType extends Type {
    public String Type = "void";
    public String toString() {
        return "void";
    }
}

class StringType extends Type {
    public String Type = "String";

    public String toString() {
        return "String";
    }
}
class FnType extends Type {
    public String Type = "function";
    public String toString() {
        return "function";
    }
}


class StructType extends Type {
    private IdNode myId;
    public String Type = "Struct";
    public StructType(IdNode id) {
        myId = id;
    }

    public String toString() {
        return myId.name();
    }
}

class StructDefType extends Type {
    public String Type = "struct";
    public String toString() {
        return "struct";
    }
}