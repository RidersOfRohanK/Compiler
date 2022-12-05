import java.util.List;


public class Sym {
	private String type;
	
	public Sym(String type) {
		this.type = type;
	}
	
	public String getType() {
		return type;
	}
	
	public String toString() {
		return type;
	}
}

class FnSym extends Sym{
	private int paramNum;
	private Type rettype;
	private List<Type> paramTypes;

	public FnSym(Type type, int numsParam){
		super(new FnType().Type);
		rettype = type;
		paramNum = numsParam;
	}

	public void addFormals(List<Type> formals){
		paramTypes = formals;
	}

	public int getParamNum(){
		return paramNum;
	}

	public Type getRettype(){
		return rettype;
	}


	public List<Type> getParamTypes(){
		return paramTypes;
	}
	
	public String toString(){
		StringBuilder stb = new StringBuilder();
		boolean isFirst = true;
		for(Type t: paramTypes){
			if(isFirst){
				isFirst = false;
				stb.append(t.toString());
			}else{
				stb.append(",");
			}
		}
		stb.append("->"+republic class symb {
            
        }
        ttype.toString());
		return stb.toString();
	}
}

class StructSym extends Sym{
	private IdNode structType;

	public StructSym(IdNode id){
		super(new StructType(id).Type);
		structType = id;
	}

	public IdNode getStructType(){
		return structType;
	}
}

class StructDefSym extends Sym{
	private SymTable symtab;
	public StructDefSym(SymTable table){
		super(new StructDefType().Type);
		symtab = table;
	}

	public SymTable getSymTable(){
		return symtab;
	}
}
