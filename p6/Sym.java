import java.util.*;

/***
 * The Sym class defines a symbol-table entry. 
 * Each Sym contains a type (a Type).
 ***/
public class Sym {
    private Type type;
    private static int localOffset = -8;
    private int globalOffset = 0;
    
    public Sym(Type type) {
        this.type = type;
    }
    
    public Type getType() {
        return type;
    }
    
    public String toString() {
        if(this.globalOffset==1){
            return ("global, "+type.toString());
        }else{
            return (this.globalOffset+", "+type.toString());
        }
    }

    public static int getLocalOffset() {
        return localOffset;
    }

    public static void setLocalOffset(int localOffset) {
        Sym.localOffset = localOffset;
    }

    public int getGlobalOffset() {
        return globalOffset;
    }

    public void setGlobalOffset(int globalOffset) {
        this.globalOffset = globalOffset;
    }
}

/***
 * The FnSym class is a subclass of the Sym class just for functions.
 * The returnType field holds the return type and there are fields to hold
 * information about the parameters.
 ***/
class FnSym extends Sym {
    // new fields
    private Type returnType;
    private int numParams;
    private List<Type> paramTypes;
    private int paramsSize = 0;
    private int localSize = 0;
    
    public FnSym(Type type, int numparams) {
        super(new FnType());
        returnType = type;
        numParams = numparams;
    }

    public void addFormals(List<Type> L) {
        paramTypes = L;
    }
    
    public Type getReturnType() {
        return returnType;
    }

    public int getNumParams() {
        return numParams;
    }

    public List<Type> getParamTypes() {
        return paramTypes;
    }

    public String toString() {
        // make list of formals
        String str = "";
        boolean notfirst = false;
        str+="("+this.paramsSize +","+this.localSize+"),";
        for (Type type : paramTypes) {
            if (notfirst)
                str += ",";
            else
                notfirst = true;
            str += type.toString();
        }

        str += "->" + returnType.toString();
        return str;
    }

    public int getLocalsSize() {
        return localSize;
    }

    public void setLocalsSize(int localsSize) {
        this.localSize = localsSize;
    }

    public int getParamsSize() {
        return paramsSize;
    }

    public void setParamsSize(int paramSize) {
        this.paramsSize = paramSize;
    }
}

/***
 * The StructSym class is a subclass of the Sym class just for variables 
 * declared to be a struct type. 
 * Each StructSym contains a symbol table to hold information about its 
 * fields.
 ***/
class StructSym extends Sym {
    // new fields
    private IdNode structType;  // name of the struct type
    
    public StructSym(IdNode id) {
        super(new StructType(id));
        structType = id;
    }

    public IdNode getStructType() {
        return structType;
    }    
}

/***
 * The StructDefSym class is a subclass of the Sym class just for the 
 * definition of a struct type. 
 * Each StructDefSym contains a symbol table to hold information about its 
 * fields.
 ***/
class StructDefSym extends Sym {
    // new fields
    private SymTable symTab;
    
    public StructDefSym(SymTable table) {
        super(new StructDefType());
        symTab = table;
    }

    public SymTable getSymTable() {
        return symTab;
    }
}
