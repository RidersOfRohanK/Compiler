import java.io.*;
import java.util.*;

// **********************************************************************
// The ASTnode class defines the nodes of the abstract-syntax tree that
// represents a minim program.
//
// Internal nodes of the tree contain pointers to children, organized
// either in a list (for nodes that may have a variable number of 
// children) or as a fixed set of fields.
//
// The nodes for literals and ids contain line and character number
// information; for string literals and identifiers, they also contain a
// string; for integer literals, they also contain an integer value.
//
// Here are all the different kinds of AST nodes and what kinds of children
// they have.  All of these kinds of AST nodes are subclasses of "ASTnode".
// Indentation indicates further subclassing:
//
//     Subclass            Kids
//     --------            ----
//     ProgramNode         DeclListNode
//     DeclListNode        linked list of DeclNode
//     DeclNode:
//       VarDeclNode       TypeNode, IdNode, int
//       FnDeclNode        TypeNode, IdNode, FormalsListNode, FnBodyNode
//       FormalDeclNode    TypeNode, IdNode
//       StructDeclNode    IdNode, DeclListNode
//
//     FormalsListNode     linked list of FormalDeclNode
//     FnBodyNode          DeclListNode, StmtListNode
//     StmtListNode        linked list of StmtNode
//     ExpListNode         linked list of ExpNode
//
//     TypeNode:
//       IntNode           -- none --
//       BoolNode          -- none --
//       VoidNode          -- none --
//       StructNode        IdNode
//
//     StmtNode:
//       AssignStmtNode      AssignExpNode
//       PostIncStmtNode     ExpNode
//       PostDecStmtNode     ExpNode
//       ReadStmtNode        ExpNode
//       WriteStmtNode       ExpNode
//       IfStmtNode          ExpNode, DeclListNode, StmtListNode
//       IfElseStmtNode      ExpNode, DeclListNode, StmtListNode,
//                                    DeclListNode, StmtListNode
//       WhileStmtNode       ExpNode, DeclListNode, StmtListNode
//       CallStmtNode        CallExpNode
//       ReturnStmtNode      ExpNode
//
//     ExpNode:
//       IntLitNode          -- none --
//       StrLitNode          -- none --
//       TrueNode            -- none --
//       FalseNode           -- none --
//       IdNode              -- none --
//       DotAccessNode       ExpNode, IdNode
//       AssignExpNode       ExpNode, ExpNode
//       CallExpNode         IdNode, ExpListNode
//       UnaryExpNode        ExpNode
//         UnaryMinusNode
//         NotNode
//       BinaryExpNode       ExpNode ExpNode
//         PlusNode     
//         MinusNode
//         TimesNode
//         DivideNode
//         AndNode
//         OrNode
//         EqualsNode
//         NotEqualsNode
//         LessNode
//         GreaterNode
//         LessEqNode
//         GreaterEqNode
//
// Here are the different kinds of AST nodes again, organized according to
// whether they are leaves, internal nodes with linked lists of kids, or
// internal nodes with a fixed number of kids:
//
// (1) Leaf nodes:
//        IntNode,   BoolNode,  VoidNode,  IntLitNode,  StrLitNode,
//        TrueNode,  FalseNode, IdNode
//
// (2) Internal nodes with (possibly empty) linked lists of children:
//        DeclListNode, FormalsListNode, StmtListNode, ExpListNode
//
// (3) Internal nodes with fixed numbers of kids:
//        ProgramNode,     VarDeclNode,     FnDeclNode,     FormalDeclNode,
//        StructDeclNode,  FnBodyNode,      StructNode,     AssignStmtNode,
//        PostIncStmtNode, PostDecStmtNode, ReadStmtNode,   WriteStmtNode   
//        IfStmtNode,      IfElseStmtNode,  WhileStmtNode,  CallStmtNode
//        ReturnStmtNode,  DotAccessNode,   AssignExpNode,  CallExpNode,
//        UnaryExpNode,    BinaryExpNode,   UnaryMinusNode, NotNode,
//        PlusNode,        MinusNode,       TimesNode,      DivideNode,
//        AndNode,         OrNode,          EqualsNode,     NotEqualsNode,
//        LessNode,        GreaterNode,     LessEqNode,     GreaterEqNode
//
// **********************************************************************

// **********************************************************************
//   ASTnode class (base class for all other kinds of nodes)
// **********************************************************************

abstract class ASTnode { 
    // every subclass must provide an unparse operation
    abstract public void unparse(PrintWriter p, int indent);

    // this method can be used by the unparse methods to do indenting
    protected void doIndent(PrintWriter p, int indent) {
        for (int k=0; k<indent; k++) p.print(" ");
    }
}

// **********************************************************************
//   ProgramNode,  DeclListNode, FormalsListNode, FnBodyNode,
//   StmtListNode, ExpListNode
// **********************************************************************

class ProgramNode extends ASTnode {
    public ProgramNode(DeclListNode L) {
        myDeclList = L;
    }

    /***
     * nameAnalysis
     * Creates an empty symbol table for the outermost scope, then processes
     * all of the globals, struct defintions, and functions in the program.
     ***/
    public void nameAnalysis() {
        SymTable symTab = new SymTable();
        myDeclList.nameAnalysis(symTab);

        //check for main func
        Sym mainFunc = null;
        try {
             mainFunc = symTab.lookupLocal("main");
        }catch(EmptySymTableException e){
            System.out.println(e);
        }
        if(mainFunc == null||!mainFunc.getType().isFnType()){
            ErrMsg.fatal(0,0,"There is no function named main");
        }



    }
    
    /***
     * typeCheck
     ***/
    public void typeCheck() {
        myDeclList.typeCheck();
    }
    
    public void unparse(PrintWriter p, int indent) {
        myDeclList.unparse(p, indent);
    }

    public void codeGen(PrintWriter p){
        Codegen.p = p;
        Codegen.stringMap = new HashMap<>();
        myDeclList.codeGen();
    }

    // one kid
    private DeclListNode myDeclList;
}

class DeclListNode extends ASTnode {
    public DeclListNode(List<DeclNode> S) {
        myDecls = S;
    }

    /***
     * nameAnalysis
     * Given a symbol table symTab, process all of the decls in the list.
     ***/
    public void nameAnalysis(SymTable symTab) {
        nameAnalysis(symTab, symTab);
    }
    
    /***
     * nameAnalysis
     * Given a symbol table symTab and a global symbol table globalTab
     * (for processing struct names in variable decls), process all of the 
     * decls in the list.
     ***/    
    public void nameAnalysis(SymTable symTab, SymTable globalTab) {
        int currentOffset = Sym.getLocalOffset();
        for (DeclNode node : myDecls) {
            if (node instanceof VarDeclNode) {
                ((VarDeclNode)node).nameAnalysis(symTab, globalTab);
                ((VarDeclNode)node).getSym().setGlobalOffset(currentOffset);
            } else {
                node.nameAnalysis(symTab);
            }
        }
        Sym.setLocalOffset(currentOffset);
    }    
    
    /***
     * typeCheck
     ***/
    public void typeCheck() {
        for (DeclNode node : myDecls) {
            node.typeCheck();
        }
    }
    
    public void unparse(PrintWriter p, int indent) {
        Iterator it = myDecls.iterator();
        try {
            while (it.hasNext()) {
                ((DeclNode)it.next()).unparse(p, indent);
            }
        } catch (NoSuchElementException ex) {
            System.err.println("unexpected NoSuchElementException in DeclListNode.print");
            System.exit(-1);
        }
    }

    public void codeGen(){
        for(DeclNode node : myDecls){
            if(node instanceof VarDeclNode || node instanceof FnDeclNode){
                node.codeGen();
            }
        }
    }


    // list of kids (DeclNodes)
    private List<DeclNode> myDecls;
}

class FormalsListNode extends ASTnode {
    public FormalsListNode(List<FormalDeclNode> S) {
        myFormals = S;
    }

    /***
     * nameAnalysis
     * Given a symbol table symTab, do:
     * for each formal decl in the list
     *     process the formal decl
     *     if there was no error, add type of formal decl to list
     ***/
    public List<Type> nameAnalysis(SymTable symTab) {
        List<Type> typeList = new LinkedList<Type>();
        int currentOffset = 4;
        for (FormalDeclNode node : myFormals) {
            Sym sym = node.nameAnalysis(symTab);
            if (sym != null) {
                typeList.add(sym.getType());
                sym.setGlobalOffset(currentOffset);
                currentOffset+=4;
            }
        }
        this.paramsSize = currentOffset - 4;
        return typeList;
    }    
    
    /***
     * Return the number of formals in this list.
     ***/
    public int length() {
        return myFormals.size();
    }
    
    public void unparse(PrintWriter p, int indent) {
        Iterator<FormalDeclNode> it = myFormals.iterator();
        if (it.hasNext()) { // if there is at least one element
            it.next().unparse(p, indent);
            while (it.hasNext()) {  // print the rest of the list
                p.print(", ");
                it.next().unparse(p, indent);
            }
        } 
    }
    public int getParamsSize(){
        return paramsSize;
    }

    // list of kids (FormalDeclNodes)
    private List<FormalDeclNode> myFormals;
    private int paramsSize;
}

class FnBodyNode extends ASTnode {
    public FnBodyNode(DeclListNode declList, StmtListNode stmtList) {
        myDeclList = declList;
        myStmtList = stmtList;
    }

    /***
     * nameAnalysis
     * Given a symbol table symTab, do:
     * - process the declaration list
     * - process the statement list
     ***/
    public void nameAnalysis(SymTable symTab) {
        Sym.setLocalOffset(-8);
        myDeclList.nameAnalysis(symTab);
        myStmtList.nameAnalysis(symTab);
        int currentOffset = Sym.getLocalOffset();
        this.localsSize = (currentOffset+8)*(-1);
    }    
 
    /***
     * typeCheck
     ***/
    public void typeCheck(Type retType) {
        myStmtList.typeCheck(retType);
    }    
          
    public void unparse(PrintWriter p, int indent) {
        myDeclList.unparse(p, indent);
        myStmtList.unparse(p, indent);
    }
    public void codeGen(String funcEndLabel){
        myStmtList.codeGen(funcEndLabel);
    }
    public int getLocalsSize(){
        return this.localsSize;
    }

    // two kids
    private DeclListNode myDeclList;
    private StmtListNode myStmtList;
    private int localsSize;
}

class StmtListNode extends ASTnode {
    public StmtListNode(List<StmtNode> S) {
        myStmts = S;
    }

    /***
     * nameAnalysis
     * Given a symbol table symTab, process each statement in the list.
     ***/
    public void nameAnalysis(SymTable symTab) {
        for (StmtNode node : myStmts) {
            node.nameAnalysis(symTab);
        }
    }    
    
    /***
     * typeCheck
     ***/
    public void typeCheck(Type retType) {
        for(StmtNode node : myStmts) {
            node.typeCheck(retType);
        }
    }
    
    public void unparse(PrintWriter p, int indent) {
        Iterator<StmtNode> it = myStmts.iterator();
        while (it.hasNext()) {
            it.next().unparse(p, indent);
        }
    }
    public void codeGen(String funcEndLabel){
      for(StmtNode node : myStmts){
        node.codeGen(funcEndLabel);//TODO fix this
      }
    }

    // list of kids (StmtNodes)
    private List<StmtNode> myStmts;
}

class ExpListNode extends ASTnode {
    public ExpListNode(List<ExpNode> S) {
        myExps = S;
        myExpsReversed = new LinkedList<ExpNode>(S);
        Collections.reverse(myExpsReversed);
    }
    
    public int size() {
        return myExps.size();
    }
    
    /***
     * nameAnalysis
     * Given a symbol table symTab, process each exp in the list.
     ***/
    public void nameAnalysis(SymTable symTab) {
        for (ExpNode node : myExps) {
            node.nameAnalysis(symTab);
        }
    }
    
    /***
     * typeCheck
     ***/
    public void typeCheck(List<Type> typeList) {
        int k = 0;
        try {
            for (ExpNode node : myExps) {
                Type actualType = node.typeCheck();     // actual type of arg
                
                if (!actualType.isErrorType()) {        // if this is not an error
                    Type formalType = typeList.get(k);  // get the formal type
                    if (!formalType.equals(actualType)) {
                        ErrMsg.fatal(node.lineNum(), node.charNum(),
                                     "Actual type and formal type doe not match");
                    }
                }
                k++;
            }
        } catch (NoSuchElementException e) {
            System.err.println("unexpected NoSuchElementException in ExpListNode.typeCheck");
            System.exit(-1);
        }
    }
    
    public void unparse(PrintWriter p, int indent) {
        Iterator<ExpNode> it = myExps.iterator();
        if (it.hasNext()) { // if there is at least one element
            it.next().unparse(p, indent);
            while (it.hasNext()) {  // print the rest of the list
                p.print(", ");
                it.next().unparse(p, indent);
            }
        } 
    }
    public void codeGen(){

      for(ExpNode node : myExpsReversed){
        node.codeGenCommon();
        Codegen.genPush(Codegen.T0);
      }
    }

    // list of kids (ExpNodes)
    private List<ExpNode> myExps;
    private List<ExpNode> myExpsReversed;
}

// **********************************************************************
// ******  DeclNode and its subclasses
// **********************************************************************

abstract class DeclNode extends ASTnode {
    /***
     * Note: a formal decl needs to return a sym
     ***/
    abstract public Sym nameAnalysis(SymTable symTab);

    // default version of typeCheck for non-function decls
    public void typeCheck() { }
  public void codeGen(){}
}

class VarDeclNode extends DeclNode {
    public VarDeclNode(TypeNode type, IdNode id, int size) {
        myType = type;
        myId = id;
        mySize = size;
    }

    /***
     * nameAnalysis (overloaded)
     * Given a symbol table symTab, do:
     * if this name is declared void, then error
     * else if the declaration is of a struct type, 
     *     lookup type name (globally)
     *     if type name doesn't exist, then error
     * if no errors so far,
     *     if name has already been declared in this scope, then error
     *     else add name to local symbol table     
     *
     * symTab is local symbol table (say, for struct field decls)
     * globalTab is global symbol table (for struct type names)
     * symTab and globalTab can be the same
     ***/
    public Sym nameAnalysis(SymTable symTab) {
        return nameAnalysis(symTab, symTab);
    }
    
    public Sym nameAnalysis(SymTable symTab, SymTable globalTab) {
        boolean badDecl = false;
        String name = myId.name();
        Sym sym = null;
        IdNode structId = null;

        if (myType instanceof VoidNode) {  // check for void type
            ErrMsg.fatal(myId.lineNum(), myId.charNum(), 
                         "Non-function declared void");
            badDecl = true;        
        }
        
        else if (myType instanceof StructNode) {
            structId = ((StructNode)myType).idNode();
			try {
				sym = globalTab.lookupGlobal(structId.name());
            
				// if the name for the struct type is not found, 
				// or is not a struct type
				if (sym == null || !(sym instanceof StructDefSym)) {
					ErrMsg.fatal(structId.lineNum(), structId.charNum(), 
								"Name of struct type invalid");
					badDecl = true;
				}
				else {
					structId.link(sym);
				}
			} catch (EmptySymTableException ex) {
				System.err.println("Unexpected EmptySymTableException " +
								    " in VarDeclNode.nameAnalysis");
				System.exit(-1);
			} 
        }
        
		try {
			if (symTab.lookupLocal(name) != null) {
				ErrMsg.fatal(myId.lineNum(), myId.charNum(), 
							"Identifier multiply-declared");
				badDecl = true;            
			}
		} catch (EmptySymTableException ex) {
            System.err.println("Unexpected EmptySymTableException " +
                               " in VarDeclNode.nameAnalysis");
            System.exit(-1);
        } 
        
        if (!badDecl) {  // insert into symbol table
            try {
                if (myType instanceof StructNode) {
                    sym = new StructSym(structId);
                }
                else {
                    sym = new Sym(myType.type());
                }
                symTab.addDecl(name, sym);
                myId.link(sym);
            } catch (DuplicateSymException ex) {
                System.err.println("Unexpected DuplicateSymException " +
                                   " in VarDeclNode.nameAnalysis");
                System.exit(-1);
            } catch (EmptySymTableException ex) {
                System.err.println("Unexpected EmptySymTableException " +
                                   " in VarDeclNode.nameAnalysis");
                System.exit(-1);
            }
        }
        
        return sym;
    }    
    
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myType.unparse(p, 0);
        p.print(" ");
        p.print(myId.name());
        p.println(";");
    }
    public Sym getSym(){
      return myId.sym();
    }
    public void codeGen(){
      if(this.myId.sym().getGlobalOffset() == 1){
        Codegen.p.print(Codegen.addGlobalVar(this.myId.name()));
      }
    }

    // three kids
    private TypeNode myType;
    private IdNode myId;
    private int mySize;  // use value NOT_STRUCT if this is not a struct type

    public static int NOT_STRUCT = -1;
}

class FnDeclNode extends DeclNode {
    public FnDeclNode(TypeNode type,
                      IdNode id,
                      FormalsListNode formalList,
                      FnBodyNode body) {
        myType = type;
        myId = id;
        myFormalsList = formalList;
        myBody = body;
    }

    /***
     * nameAnalysis
     * Given a symbol table symTab, do:
     * if this name has already been declared in this scope, then error
     * else add name to local symbol table
     * in any case, do the following:
     *     enter new scope
     *     process the formals
     *     if this function is not multiply declared,
     *         update symbol table entry with types of formals
     *     process the body of the function
     *     exit scope
     ***/
    public Sym nameAnalysis(SymTable symTab) {
        String name = myId.name();
        FnSym sym = null;
        try {
			if (symTab.lookupLocal(name) != null) {
				ErrMsg.fatal(myId.lineNum(), myId.charNum(),
							"Identifier multiply-declared");
			}
        
			else { // add function name to local symbol table
				try {
					sym = new FnSym(myType.type(), myFormalsList.length());
					symTab.addDecl(name, sym);
					myId.link(sym);
				} catch (DuplicateSymException ex) {
					System.err.println("Unexpected DuplicateSymException " +
									" in FnDeclNode.nameAnalysis");
					System.exit(-1);
				} catch (EmptySymTableException ex) {
					System.err.println("Unexpected EmptySymTableException " +
									" in FnDeclNode.nameAnalysis");
					System.exit(-1);
				}
			}
		} catch (EmptySymTableException ex) {
            System.err.println("Unexpected EmptySymTableException " +
                               " in FnDeclNode.nameAnalysis");
            System.exit(-1);
        } 
        
        symTab.addScope();  // add a new scope for locals and params
        
        // process the formals
        List<Type> typeList = myFormalsList.nameAnalysis(symTab);
        sym.setParamsSize(myFormalsList.getParamsSize());
        if (sym != null) {
            sym.addFormals(typeList);
        }
        
        myBody.nameAnalysis(symTab); // process the function body
        sym.setLocalsSize(myBody.getLocalsSize());
        try {
            symTab.removeScope();  // exit scope
        } catch (EmptySymTableException ex) {
            System.err.println("Unexpected EmptySymTableException " +
                               " in FnDeclNode.nameAnalysis");
            System.exit(-1);
        }
        
        return null;
    } 
       
    /***
     * typeCheck
     ***/
    public void typeCheck() {
        myBody.typeCheck(myType.type());
    }
        
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myType.unparse(p, 0);
        p.print(" ");
        p.print(myId.name());
        p.print("(");
        myFormalsList.unparse(p, 0);
        p.println(") {");
        myBody.unparse(p, indent+4);
        p.println("}\n");
    }
    public void codeGen(){
      String funcName = this.myId.name();
      String funcEndLabel = Codegen.nextLabel();
      if(funcName.equals("main")){
        Codegen.p.print("\t.text\n\t.globl main\nmain:\t# FUNCTION ENTRY\n");
      } else {
        Codegen.p.print("\t.text\n_"+funcName+":\t#FUNCTION ENTRY\n");
      }
      Codegen.genPush(Codegen.RA);
      Codegen.genPush(Codegen.FP);
      Codegen.generate("addu",Codegen.FP,Codegen.SP,8);
      Codegen.generate("subu",Codegen.SP,Codegen.FP,((FnSym)this.myId.sym()).getLocalsSize());
      this.myBody.codeGen(funcEndLabel);
      Codegen.genLabel(funcEndLabel);
      Codegen.p.print("\t\t#FUNCTION EXIT\n");
      Codegen.generateIndexed("lw",Codegen.RA,Codegen.FP,0);
      Codegen.generate("move",Codegen.T0,Codegen.FP);
      Codegen.generateIndexed("lw",Codegen.FP,Codegen.FP,-4);
      Codegen.generate("move",Codegen.SP,Codegen.T0);
      Codegen.generate("jr",Codegen.RA);
    }

    // 4 kids
    private TypeNode myType;
    private IdNode myId;
    private FormalsListNode myFormalsList;
    private FnBodyNode myBody;
}

class FormalDeclNode extends DeclNode {
    public FormalDeclNode(TypeNode type, IdNode id) {
        myType = type;
        myId = id;
    }

    /***
     * nameAnalysis
     * Given a symbol table symTab, do:
     * if this formal is declared void, then error
     * else if this formal is already in the local symble table,
     *     then issue multiply declared error message and return null
     * else add a new entry to the symbol table and return that Sym
     ***/
    public Sym nameAnalysis(SymTable symTab) {
        String name = myId.name();
        boolean badDecl = false;
        Sym sym = null;
        
        if (myType instanceof VoidNode) {
            ErrMsg.fatal(myId.lineNum(), myId.charNum(), 
                         "Non-function declared void");
            badDecl = true;        
        }
        
        try { 
			if (symTab.lookupLocal(name) != null) {
				ErrMsg.fatal(myId.lineNum(), myId.charNum(), 
							"Identifier multiply-declared");
				badDecl = true;
			}
        } catch (EmptySymTableException ex) {
            System.err.println("Unexpected EmptySymTableException " +
                               " in FormalDeclNode.nameAnalysis");
            System.exit(-1);
        } 
        
        if (!badDecl) {  // insert into symbol table
            try {
                sym = new Sym(myType.type());
                symTab.addDecl(name, sym);
                myId.link(sym);
            } catch (DuplicateSymException ex) {
                System.err.println("Unexpected DuplicateSymException " +
                                   " in FormalDeclNode.nameAnalysis");
                System.exit(-1);
            } catch (EmptySymTableException ex) {
                System.err.println("Unexpected EmptySymTableException " +
                                   " in FormalDeclNode.nameAnalysis");
                System.exit(-1);
            }
        }  
        
        return sym;
    } 		
    
    public void unparse(PrintWriter p, int indent) {
        myType.unparse(p, 0);
        p.print(" ");
        p.print(myId.name());
    }

    // two kids
    private TypeNode myType;
    private IdNode myId;
}

class StructDeclNode extends DeclNode {
    public StructDeclNode(IdNode id, DeclListNode declList) {
        myId = id;
        myDeclList = declList;
    }

    /***
     * nameAnalysis
     * Given a symbol table symTab, do:
     * if this name is already in the symbol table,
     *     then multiply declared error (don't add to symbol table)
     * create a new symbol table for this struct definition
     * process the decl list
     * if no errors
     *     add a new entry to symbol table for this struct
     ***/
    public Sym nameAnalysis(SymTable symTab) {
        String name = myId.name();
        boolean badDecl = false;
        try {
			if (symTab.lookupLocal(name) != null) {
				ErrMsg.fatal(myId.lineNum(), myId.charNum(), 
							"Identifier multiply-declared");
				badDecl = true;            
			}
		} catch (EmptySymTableException ex) {
            System.err.println("Unexpected EmptySymTableException " +
                               " in StructDeclNode.nameAnalysis");
            System.exit(-1);
        } 

        SymTable structSymTab = new SymTable();
        
        // process the fields of the struct
        myDeclList.nameAnalysis(structSymTab, symTab);
        
        if (!badDecl) {
            try {   // add entry to symbol table
                StructDefSym sym = new StructDefSym(structSymTab);
                symTab.addDecl(name, sym);
                myId.link(sym);
            } catch (DuplicateSymException ex) {
                System.err.println("Unexpected DuplicateSymException " +
                                   " in StructDeclNode.nameAnalysis");
                System.exit(-1);
            } catch (EmptySymTableException ex) {
                System.err.println("Unexpected EmptySymTableException " +
                                   " in StructDeclNode.nameAnalysis");
                System.exit(-1);
            }
        }
        
        return null;
    }    
    
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("struct ");
        p.print(myId.name());
        p.println("{");
        myDeclList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("};\n");

    }

    // two kids
    private IdNode myId;
    private DeclListNode myDeclList;
}

// **********************************************************************
// ******  TypeNode and its Subclasses
// **********************************************************************

abstract class TypeNode extends ASTnode {
    /* all subclasses must provide a type method ***/
    abstract public Type type();
}

class IntNode extends TypeNode {
    public IntNode() {
    }

    /***
     * type
     ***/
    public Type type() {
        return new IntType();
    }
    
    public void unparse(PrintWriter p, int indent) {
        p.print("int");
    }
}

class BoolNode extends TypeNode {
    public BoolNode() {
    }

    /***
     * type
     ***/
    public Type type() {
        return new BoolType();
    }
    
    public void unparse(PrintWriter p, int indent) {
        p.print("bool");
    }
}

class VoidNode extends TypeNode {
    public VoidNode() {
    }
    
    /***
     * type
     ***/
    public Type type() {
        return new VoidType();
    }
    
    public void unparse(PrintWriter p, int indent) {
        p.print("void");
    }
}

class StructNode extends TypeNode {
    public StructNode(IdNode id) {
        myId = id;
    }

    public IdNode idNode() {
        return myId;
    }
    
    /***
     * type
     ***/
    public Type type() {
        return new StructType(myId);
    }
    
    public void unparse(PrintWriter p, int indent) {
        p.print("struct ");
        p.print(myId.name());
    }
    
    // one kid
    private IdNode myId;
}

// **********************************************************************
// ******  StmtNode and its subclasses
// **********************************************************************

abstract class StmtNode extends ASTnode {
    abstract public void nameAnalysis(SymTable symTab);
    abstract public void typeCheck(Type retType);
    abstract public void codeGen(String funcExitLabel);

}

class AssignStmtNode extends StmtNode {
    public AssignStmtNode(AssignExpNode assign) {
        myAssign = assign;
    }

    /***
     * nameAnalysis
     * Given a symbol table symTab, perform name analysis on this node's child
     ***/
    public void nameAnalysis(SymTable symTab) {
        myAssign.nameAnalysis(symTab);
    }
    
    /***
     * typeCheck
     ***/
    public void typeCheck(Type retType) {
        myAssign.typeCheck();
    }
        
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myAssign.unparse(p, -1); // no parentheses
        p.println(";");
    }
    public void codeGen(String fnExitLabel){
      myAssign.codeGenExp();
    }

    // one kid
    private AssignExpNode myAssign;
}

class PostIncStmtNode extends StmtNode {
    public PostIncStmtNode(ExpNode exp) {
        myExp = exp;
    }
    
    /***
     * nameAnalysis
     * Given a symbol table symTab, perform name analysis on this node's child
     ***/
    public void nameAnalysis(SymTable symTab) {
        myExp.nameAnalysis(symTab);
    }
    
    /***
     * typeCheck
     ***/
    public void typeCheck(Type retType) {
        Type type = myExp.typeCheck();
        
        if (!type.isErrorType() && !type.isIntType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                         "Arithmetic operator with non-numeric operand");
        }
    }
        
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myExp.unparse(p, 0);
        p.println("++;");
    }
  public void codeGen(String funcExitLabel){
    assert (myExp instanceof IdNode);
    IdNode node = (IdNode) myExp;
    node.codeGenLoc();
    node.codeGenExp();
    Codegen.genPop(Codegen.T0);
    Codegen.genPop(Codegen.T1);
    Codegen.generate("add",Codegen.T0,Codegen.T0,1);
    Codegen.generateIndexed("sw",Codegen.T0,Codegen.T1,0);
  }

    // one kid
    private ExpNode myExp;
}

class PostDecStmtNode extends StmtNode {
    public PostDecStmtNode(ExpNode exp) {
        myExp = exp;
    }

    /***
     * nameAnalysis
     * Given a symbol table symTab, perform name analysis on this node's child
     ***/
    public void nameAnalysis(SymTable symTab) {
        myExp.nameAnalysis(symTab);
    }
    
    /***
     * typeCheck
     ***/
    public void typeCheck(Type retType) {
        Type type = myExp.typeCheck();
        
        if (!type.isErrorType() && !type.isIntType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                         "Arithmetic operator with non-numeric operand");
        }
    }
        
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myExp.unparse(p, 0);
        p.println("--;");
    }
  public void codeGen(String funcExitLabel){
    assert (myExp instanceof IdNode);
    IdNode node = (IdNode) myExp;
    node.codeGenLoc();
    node.codeGenExp();
    Codegen.genPop(Codegen.T0);
    Codegen.genPop(Codegen.T1);
    Codegen.generate("sub",Codegen.T0,Codegen.T0,1);
    Codegen.generateIndexed("sw",Codegen.T0,Codegen.T1,0);
  }
    
    // one kid
    private ExpNode myExp;
}

class ReadStmtNode extends StmtNode {
    public ReadStmtNode(ExpNode e) {
        myExp = e;
    }

    /***
     * nameAnalysis
     * Given a symbol table symTab, perform name analysis on this node's child
     ***/
    public void nameAnalysis(SymTable symTab) {
        myExp.nameAnalysis(symTab);
    }    
 
    /***
     * typeCheck
     ***/
    public void typeCheck(Type retType) {
        Type type = myExp.typeCheck();
        
        if (type.isFnType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                         "Read attempt of function");
        }
        
        if (type.isStructDefType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                         "Read attempt of struct name");
        }
        
        if (type.isStructType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                         "Read attempt of struct variable");
        }
    }
    public void codeGen(String funcExitLabel){
      assert (myExp instanceof IdNode);
      Codegen.generate("li",Codegen.V0,5);
      Codegen.generate("syscall");
      IdNode node = (IdNode) myExp;
      node.codeGenLoc();
      Codegen.genPop(Codegen.T0);
      if(node.sym().getType().isBoolType()){
        Codegen.generateIndexed("sne",Codegen.T1,Codegen.V0,0);
      } else{
        Codegen.generateIndexed("sw",Codegen.V0,Codegen.T0,0);
      }
    }
    
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("input >> ");
        myExp.unparse(p, 0);
        p.println(";");
    }

    // one kid (actually can only be an IdNode)
    private ExpNode myExp;
}

class WriteStmtNode extends StmtNode {
    public WriteStmtNode(ExpNode exp) {
        myExp = exp;
    }

    /***
     * nameAnalysis
     * Given a symbol table symTab, perform name analysis on this node's child
     ***/
    public void nameAnalysis(SymTable symTab) {
        myExp.nameAnalysis(symTab);
    }

    /***
     * typeCheck
     ***/
    public void typeCheck(Type retType) {
        Type type = myExp.typeCheck();
        
        if (type.isFnType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                         "Write attempt of function");
        }
        
        if (type.isStructDefType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                         "Write attempt of struct name");
        }
        
        if (type.isStructType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                         "Write attempt of struct variable");
        }
        
        if (type.isVoidType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                         "Write attempt of void");
        }
    }
        
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("disp << ");
        myExp.unparse(p, 0);
        p.println(";");
    }
    public void codeGen(String funcExitLabel){
      myExp.codeGenExp();
      Type expType = myExp.typeCheck();
      Codegen.genPop(Codegen.T0);
      Codegen.generate("move",Codegen.A0,Codegen.T0);
      int sysCallCode = 1;
      if(expType.isStringType()){
        sysCallCode = 4;
      }else if(expType.isIntType() || expType.isBoolType()){
        sysCallCode = 1;
      }
      Codegen.generate("li",Codegen.V0,sysCallCode);
      Codegen.generate("syscall");
    }

    // one kid
    private ExpNode myExp;
}

class IfStmtNode extends StmtNode {
    public IfStmtNode(ExpNode exp, DeclListNode dlist, StmtListNode slist) {
        myDeclList = dlist;
        myExp = exp;
        myStmtList = slist;
    }
    
    /***
     * nameAnalysis
     * Given a symbol table symTab, do:
     * - process the condition
     * - enter a new scope
     * - process the decls and stmts
     * - exit the scope
     ***/
    public void nameAnalysis(SymTable symTab) {
        myExp.nameAnalysis(symTab);
        symTab.addScope();
        myDeclList.nameAnalysis(symTab);
        myStmtList.nameAnalysis(symTab);
        try {
            symTab.removeScope();
        } catch (EmptySymTableException ex) {
            System.err.println("Unexpected EmptySymTableException " +
                               " in IfStmtNode.nameAnalysis");
            System.exit(-1);        
        }
    }
    
     /***
     * typeCheck
     ***/
    public void typeCheck(Type retType) {
        Type type = myExp.typeCheck();
        
        if (!type.isErrorType() && !type.isBoolType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                         "Non-bool expression in condition of if");        
        }
        
        myStmtList.typeCheck(retType);
    }
       
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("if (");
        myExp.unparse(p, 0);
        p.println(") {");
        myDeclList.unparse(p, indent+4);
        myStmtList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("}");
    }

    public void codeGen(String funcExitLabel){
      String trueLabel = Codegen.nextLabel();
      String doneLabel = Codegen.nextLabel();
      myExp.codeGenJump(trueLabel,doneLabel);
      Codegen.genLabel(trueLabel);
      myStmtList.codeGen(funcExitLabel);
      Codegen.genLabel(doneLabel);
    }

    // three kids
    private ExpNode myExp;
    private DeclListNode myDeclList;
    private StmtListNode myStmtList;
}

class IfElseStmtNode extends StmtNode {
    public IfElseStmtNode(ExpNode exp, DeclListNode dlist1,
                          StmtListNode slist1, DeclListNode dlist2,
                          StmtListNode slist2) {
        myExp = exp;
        myThenDeclList = dlist1;
        myThenStmtList = slist1;
        myElseDeclList = dlist2;
        myElseStmtList = slist2;
    }
    
    /***
     * nameAnalysis
     * Given a symbol table symTab, do:
     * - process the condition
     * - enter a new scope
     * - process the decls and stmts of then
     * - exit the scope
     * - enter a new scope
     * - process the decls and stmts of else
     * - exit the scope
     ***/
    public void nameAnalysis(SymTable symTab) {
        myExp.nameAnalysis(symTab);
        symTab.addScope();
        int entryOffset = Sym.getLocalOffset();
        myThenDeclList.nameAnalysis(symTab);
        myThenStmtList.nameAnalysis(symTab);
        int exitOffset1 = Sym.getLocalOffset();
        try {
            symTab.removeScope();
        } catch (EmptySymTableException ex) {
            System.err.println("Unexpected EmptySymTableException " +
                               " in IfStmtNode.nameAnalysis");
            System.exit(-1);        
        }
        symTab.addScope();
        Sym.setLocalOffset(entryOffset);
        myElseDeclList.nameAnalysis(symTab);
        myElseStmtList.nameAnalysis(symTab);
        int exitOffset2 = Sym.getLocalOffset();
        int exitOffset = exitOffset1< exitOffset2 ? exitOffset1:exitOffset2;//TODO no clue what this does but possibly expand?
        Sym.setLocalOffset(exitOffset);

        try {
            symTab.removeScope();
        } catch (EmptySymTableException ex) {
            System.err.println("Unexpected EmptySymTableException " +
                               " in IfStmtNode.nameAnalysis");
            System.exit(-1);        
        }
    }
    
    /***
     * typeCheck
     ***/
    public void typeCheck(Type retType) {
        Type type = myExp.typeCheck();
        
        if (!type.isErrorType() && !type.isBoolType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                         "Non-bool expression in condition of if");        
        }
        
        myThenStmtList.typeCheck(retType);
        myElseStmtList.typeCheck(retType);
    }
        
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("if (");
        myExp.unparse(p, 0);
        p.println(") {");
        myThenDeclList.unparse(p, indent+4);
        myThenStmtList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("}");
        doIndent(p, indent);
        p.println("else {");
        myElseDeclList.unparse(p, indent+4);
        myElseStmtList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("}");        
    }
  public void codeGen(String funcExitLabel){
    String trueLabel = Codegen.nextLabel();
    String falseLabel = Codegen.nextLabel();
    String doneLabel = Codegen.nextLabel();
    myExp.codeGenJump(trueLabel,falseLabel);
    Codegen.genLabel(trueLabel);
    myThenStmtList.codeGen(funcExitLabel);
    Codegen.generate("b",doneLabel);
    Codegen.genLabel(falseLabel);
    myElseStmtList.codeGen(funcExitLabel);
    Codegen.genLabel(doneLabel);
  }

    // 5 kids
    private ExpNode myExp;
    private DeclListNode myThenDeclList;
    private StmtListNode myThenStmtList;
    private StmtListNode myElseStmtList;
    private DeclListNode myElseDeclList;
}

class WhileStmtNode extends StmtNode {
    public WhileStmtNode(ExpNode exp, DeclListNode dlist, StmtListNode slist) {
        myExp = exp;
        myDeclList = dlist;
        myStmtList = slist;
    }
    
    /***
     * nameAnalysis
     * Given a symbol table symTab, do:
     * - process the condition
     * - enter a new scope
     * - process the decls and stmts
     * - exit the scope
     ***/
    public void nameAnalysis(SymTable symTab) {
        myExp.nameAnalysis(symTab);
        symTab.addScope();
        myDeclList.nameAnalysis(symTab);
        myStmtList.nameAnalysis(symTab);
        try {
            symTab.removeScope();
        } catch (EmptySymTableException ex) {
            System.err.println("Unexpected EmptySymTableException " +
                               " in IfStmtNode.nameAnalysis");
            System.exit(-1);        
        }
    }
    
    /***
     * typeCheck
     ***/
    public void typeCheck(Type retType) {
        Type type = myExp.typeCheck();
        
        if (!type.isErrorType() && !type.isBoolType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                         "Non-bool expression in condition of while");        
        }
        
        myStmtList.typeCheck(retType);
    }
        
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("while (");
        myExp.unparse(p, 0);
        p.println(") {");
        myDeclList.unparse(p, indent+4);
        myStmtList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("}");
    }
    public void codeGen(String funcExitLabel){
     String condLabel = Codegen.nextLabel();
     String bodyLabel = Codegen.nextLabel();
     String doneLabel = Codegen.nextLabel();
     Codegen.genLabel(condLabel);
     myExp.codeGenJump(bodyLabel,doneLabel);
     Codegen.genLabel(bodyLabel);
     myStmtList.codeGen(funcExitLabel);
     Codegen.generate("b",condLabel);
     Codegen.genLabel(doneLabel);
    }


    // three kids
    private ExpNode myExp;
    private DeclListNode myDeclList;
    private StmtListNode myStmtList;
}

class CallStmtNode extends StmtNode {
    public CallStmtNode(CallExpNode call) {
        myCall = call;
    }
    
    /***
     * nameAnalysis
     * Given a symbol table symTab, perform name analysis on this node's child
     ***/
    public void nameAnalysis(SymTable symTab) {
        myCall.nameAnalysis(symTab);
    }
    
    /***
     * typeCheck
     ***/
    public void typeCheck(Type retType) {
        myCall.typeCheck();
    }
    
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myCall.unparse(p, indent);
        p.println(";");
    }
    public void codeGen(String funcExitLabel){
      myCall.codeGenCommon();
    }

    // one kid
    private CallExpNode myCall;
}

class ReturnStmtNode extends StmtNode {
    public ReturnStmtNode(ExpNode exp) {
        myExp = exp;
    }
    
    /***
     * nameAnalysis
     * Given a symbol table symTab, perform name analysis on this node's child,
     * if it has one
     ***/
    public void nameAnalysis(SymTable symTab) {
        if (myExp != null) {
            myExp.nameAnalysis(symTab);
        }
    }

    /***
     * typeCheck
     ***/
    public void typeCheck(Type retType) {
        if (myExp != null) {  // return value given
            Type type = myExp.typeCheck();
            
            if (retType.isVoidType()) {
                ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                             "Return with value in a void function");                
            }
            
            else if (!retType.isErrorType() && !type.isErrorType() && !retType.equals(type)){
                ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                             "Bad return value");
            }
        }
        
        else {  // no return value given -- ok if this is a void function
            if (!retType.isVoidType()) {
                ErrMsg.fatal(0, 0, "Return value missing");                
            }
        }
        
    }
    
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("return");
        if (myExp != null) {
            p.print(" ");
            myExp.unparse(p, 0);
        }
        p.println(";");
    }

    public void codeGen(String funcEndLabel){
      if(myExp!=null && !myExp.typeCheck().isVoidType()){
        myExp.codeGenExp();
        Codegen.genPop(Codegen.V0);
      }
      Codegen.generate("b",funcEndLabel);
    }

    // one kid
    private ExpNode myExp; // possibly null
}

// **********************************************************************
// ******  ExpNode and its subclasses
// **********************************************************************

abstract class ExpNode extends ASTnode {
    /***
     * Default version for nodes with no names
     ***/
    public void nameAnalysis(SymTable symTab) { }
    
    abstract public Type typeCheck();
    abstract public int lineNum();
    abstract public int charNum();
  public void codeGenExp(){
    this.codeGenCommon();
    Codegen.genPush(Codegen.T0);
  }
  abstract protected void codeGenCommon();
  public void codeGenJump(String trueLabel,String falseLabel){}

}

class IntLitNode extends ExpNode {
    public IntLitNode(int lineNum, int charNum, int intVal) {
        myLineNum = lineNum;
        myCharNum = charNum;
        myIntVal = intVal;
    }


    /***
     * Return the line number for this literal.
     ***/
    public int lineNum() {
        return myLineNum;
    }
    
    /***
     * Return the char number for this literal.
     ***/
    public int charNum() {
        return myCharNum;
    }
        
    /***
     * typeCheck
     ***/
    public Type typeCheck() {
        return new IntType();
    }
    
    public void unparse(PrintWriter p, int indent) {
        p.print(myIntVal);
    }

    public void codeGenCommon(){
      Codegen.generate("li","$t0",String.valueOf(this.myIntVal));
    }

    private int myLineNum;
    private int myCharNum;
    private int myIntVal;
}

class StringLitNode extends ExpNode {
    public StringLitNode(int lineNum, int charNum, String strVal) {
        myLineNum = lineNum;
        myCharNum = charNum;
        myStrVal = strVal;
    }
    
    /***
     * Return the line number for this literal.
     ***/
    public int lineNum() {
        return myLineNum;
    }
    
    /***
     * Return the char number for this literal.
     ***/
    public int charNum() {
        return myCharNum;
    }
    
    /***
     * typeCheck
     ***/
    public Type typeCheck() {
        return new StringType();
    }
        
    public void unparse(PrintWriter p, int indent) {
        p.print(myStrVal);
    }

    protected void codeGenCommon(){
      String newLabel = null;
      if(!Codegen.stringMap.containsKey(myStrVal)){
        Codegen.p.print("\t.data\n");
        newLabel = Codegen.nextLabel();
        Codegen.p.print(newLabel+":\t.asciiz "+myStrVal);
        Codegen.p.print("\t.text\n");
        Codegen.stringMap.put(myStrVal,newLabel);
      }
      else{
        newLabel = Codegen.stringMap.get(myStrVal);
      }
      Codegen.generate("la",Codegen.T0,newLabel);
    }

    private int myLineNum;
    private int myCharNum;
    private String myStrVal;
}

class TrueNode extends ExpNode {
    public TrueNode(int lineNum, int charNum) {
        myLineNum = lineNum;
        myCharNum = charNum;
    }

    /***
     * Return the line number for this literal.
     ***/
    public int lineNum() {
        return myLineNum;
    }
    
    /***
     * Return the char number for this literal.
     ***/
    public int charNum() {
        return myCharNum;
    }
    
    /***
     * typeCheck
     ***/
    public Type typeCheck() {
        return new BoolType();
    }
        
    public void unparse(PrintWriter p, int indent) {
        p.print("true");
    }

    protected void codeGenCommon(){
      Codegen.generate("li",Codegen.T0,Codegen.TRUE);
    }
    public void codeGenJump(String trueLabel,String falseLabel){
      this.codeGenCommon();
      Codegen.generate("b",trueLabel);
    }

    private int myLineNum;
    private int myCharNum;
}

class FalseNode extends ExpNode {
    public FalseNode(int lineNum, int charNum) {
        myLineNum = lineNum;
        myCharNum = charNum;
    }

    /***
     * Return the line number for this literal.
     ***/
    public int lineNum() {
        return myLineNum;
    }
    
    /***
     * Return the char number for this literal.
     ***/
    public int charNum() {
        return myCharNum;
    }

    /***
     * typeCheck
     ***/
    public Type typeCheck() {
        return new BoolType();
    }
        
    public void unparse(PrintWriter p, int indent) {
        p.print("false");
    }

    protected void codeGenCommon(){
      Codegen.generate("li",Codegen.T0,Codegen.FALSE);
    }
    public void codeGenJump(String trueLabel, String falseLabel){
      this.codeGenCommon();
      Codegen.generate("b",falseLabel);
    }

    private int myLineNum;
    private int myCharNum;
}

class IdNode extends ExpNode {
    public IdNode(int lineNum, int charNum, String strVal) {
        myLineNum = lineNum;
        myCharNum = charNum;
        myStrVal = strVal;
    }

    /***
     * Link the given symbol to this ID.
     ***/
    public void link(Sym sym) {
        mySym = sym;
    }
    
    /***
     * Return the name of this ID.
     ***/
    public String name() {
        return myStrVal;
    }
    
    /***
     * Return the symbol associated with this ID.
     ***/
    public Sym sym() {
        return mySym;
    }
    
    /***
     * Return the line number for this ID.
     ***/
    public int lineNum() {
        return myLineNum;
    }
    
    /***
     * Return the char number for this ID.
     ***/
    public int charNum() {
        return myCharNum;
    }    
    
    /***
     * nameAnalysis
     * Given a symbol table symTab, do:
     * - check for use of undeclared name
     * - if ok, link to symbol table entry
     ***/
    public void nameAnalysis(SymTable symTab) {
		try {
            Sym sym = symTab.lookupGlobal(myStrVal);
            if (sym == null) {
                ErrMsg.fatal(myLineNum, myCharNum, "Identifier undeclared");
            } else {
                link(sym);
            }
        } catch (EmptySymTableException ex) {
            System.err.println("Unexpected EmptySymTableException " +
                               " in IdNode.nameAnalysis");
            System.exit(-1);
        } 
    }
 
    /***
     * typeCheck
     ***/
    public Type typeCheck() {
        if (mySym != null) {
            return mySym.getType();
        } 
        else {
            System.err.println("ID with null sym field in IdNode.typeCheck");
            System.exit(-1);
        }
        return null;
    }
           
    public void unparse(PrintWriter p, int indent) {
        p.print(myStrVal);
        if (mySym != null) {
            p.print("(" + mySym + ")");
        }
    }
    private void codeGenLoad(String command){
      if(mySym.getGlobalOffset()!=1){
        Codegen.generateIndexed(command,Codegen.T0,Codegen.FP,this.mySym.getGlobalOffset());
      } else {
        Codegen.generate(command,Codegen.T0,"_"+myStrVal);
      }
    }
    protected void codeGenCommon(){
      this.codeGenLoad("lw");
    }
    public void codeGenLoc(){
      this.codeGenLoad("la");
      Codegen.genPush(Codegen.T0);
    }
    public void codeGenJump(String trueLabel, String falseLabel){
      this.codeGenLoad("lw");
      Codegen.generate("beq",Codegen.T0,Codegen.FALSE,falseLabel);
      Codegen.generate("b",trueLabel);
    }

    private int myLineNum;
    private int myCharNum;
    private String myStrVal;
    private Sym mySym;
}

class DotAccessExpNode extends ExpNode {
    public DotAccessExpNode(ExpNode loc, IdNode id) {
        myLoc = loc;    
        myId = id;
        mySym = null;
    }

    /***
     * Return the symbol associated with this dot-access node.
     ***/
    public Sym sym() {
        return mySym;
    }    
    
    /***
     * Return the line number for this dot-access node. 
     * The line number is the one corresponding to the RHS of the dot-access.
     ***/
    public int lineNum() {
        return myId.lineNum();
    }
    
    /***
     * Return the char number for this dot-access node.
     * The char number is the one corresponding to the RHS of the dot-access.
     ***/
    public int charNum() {
        return myId.charNum();
    }
    
    /***
     * nameAnalysis
     * Given a symbol table symTab, do:
     * - process the LHS of the dot-access
     * - process the RHS of the dot-access
     * - if the RHS is of a struct type, set the sym for this node so that
     *   a dot-access "higher up" in the AST can get access to the symbol
     *   table for the appropriate struct definition
     ***/
    public void nameAnalysis(SymTable symTab) {
        badAccess = false;
        SymTable structSymTab = null; // to lookup RHS of dot-access
        Sym sym = null;
        
        myLoc.nameAnalysis(symTab);  // do name analysis on LHS
        
        // if myLoc is really an ID, then sym will be a link to the ID's symbol
        if (myLoc instanceof IdNode) {
            IdNode id = (IdNode)myLoc;
            sym = id.sym();
            
            // check ID has been declared to be of a struct type
            
            if (sym == null) { // ID was undeclared
                badAccess = true;
            }
            else if (sym instanceof StructSym) { 
                // get symbol table for struct type
                Sym tempSym = ((StructSym)sym).getStructType().sym();
                structSymTab = ((StructDefSym)tempSym).getSymTable();
            } 
            else {  // LHS is not a struct type
                ErrMsg.fatal(id.lineNum(), id.charNum(), 
                             "Dot-access of non-struct type");
                badAccess = true;
            }
        }
        
        // if myLoc is really a dot-access (i.e., myLoc was of the form
        // LHSloc.RHSid), then sym will either be
        // null - indicating RHSid is not of a struct type, or
        // a link to the Sym for the struct type RHSid was declared to be
        else if (myLoc instanceof DotAccessExpNode) {
            DotAccessExpNode loc = (DotAccessExpNode)myLoc;
            
            if (loc.badAccess) {  // if errors in processing myLoc
                badAccess = true; // don't continue proccessing this dot-access
            }
            else { //  no errors in processing myLoc
                sym = loc.sym();

                if (sym == null) {  // no struct in which to look up RHS
                    ErrMsg.fatal(loc.lineNum(), loc.charNum(), 
                                 "Dot-access of non-struct type");
                    badAccess = true;
                }
                else {  // get the struct's symbol table in which to lookup RHS
                    if (sym instanceof StructDefSym) {
                        structSymTab = ((StructDefSym)sym).getSymTable();
                    }
                    else {
                        System.err.println("Unexpected Sym type in DotAccessExpNode");
                        System.exit(-1);
                    }
                }
            }

        }
        
        else { // don't know what kind of thing myLoc is
            System.err.println("Unexpected node type in LHS of dot-access");
            System.exit(-1);
        }
        
        // do name analysis on RHS of dot-access in the struct's symbol table
        if (!badAccess) {
			try {
				sym = structSymTab.lookupGlobal(myId.name()); // lookup
				if (sym == null) { // not found - RHS is not a valid field name
					ErrMsg.fatal(myId.lineNum(), myId.charNum(), 
								"Struct field name invalid");
					badAccess = true;
				}
            
				else {
					myId.link(sym);  // link the symbol
					// if RHS is itself as struct type, link the symbol for its struct 
					// type to this dot-access node (to allow chained dot-access)
					if (sym instanceof StructSym) {
						mySym = ((StructSym)sym).getStructType().sym();
					}
				}
			} catch (EmptySymTableException ex) {
				System.err.println("Unexpected EmptySymTableException " +
								" in DotAccessExpNode.nameAnalysis");
				System.exit(-1);
			} 
        }
    }    
 
    /***
     * typeCheck
     ***/
    public Type typeCheck() {
        return myId.typeCheck();
    }
    
    public void unparse(PrintWriter p, int indent) {
        myLoc.unparse(p, 0);
        p.print(".");
        myId.unparse(p, 0);
    }
    protected void codeGenCommon(){
      //TODO - look at deleting
    }

    // two kids
    private ExpNode myLoc;    
    private IdNode myId;
    private Sym mySym;          // link to Sym for struct type
    private boolean badAccess;  // to prevent multiple, cascading errors
}

class AssignExpNode extends ExpNode {
    public AssignExpNode(ExpNode lhs, ExpNode exp) {
        myLhs = lhs;
        myExp = exp;
    }
    
    /***
     * Return the line number for this assignment node. 
     * The line number is the one corresponding to the left operand.
     ***/
    public int lineNum() {
        return myLhs.lineNum();
    }
    
    /***
     * Return the char number for this assignment node.
     * The char number is the one corresponding to the left operand.
     ***/
    public int charNum() {
        return myLhs.charNum();
    }
    
    /***
     * nameAnalysis
     * Given a symbol table symTab, perform name analysis on this node's 
     * two children
     ***/
    public void nameAnalysis(SymTable symTab) {
        myLhs.nameAnalysis(symTab);
        myExp.nameAnalysis(symTab);
    }
 
    /***
     * typeCheck
     ***/
    public Type typeCheck() {
        Type typeLhs = myLhs.typeCheck();
        Type typeExp = myExp.typeCheck();
        Type retType = typeLhs;
        
        if (typeLhs.isFnType() && typeExp.isFnType()) {
            ErrMsg.fatal(lineNum(), charNum(), "Function assignment");
            retType = new ErrorType();
        }
        
        if (typeLhs.isStructDefType() && typeExp.isStructDefType()) {
            ErrMsg.fatal(lineNum(), charNum(), "Struct name assignment");
            retType = new ErrorType();
        }
        
        if (typeLhs.isStructType() && typeExp.isStructType()) {
            ErrMsg.fatal(lineNum(), charNum(), "Struct variable assignment");
            retType = new ErrorType();
        }        
        
        if (!typeLhs.equals(typeExp) && !typeLhs.isErrorType() && !typeExp.isErrorType()) {
            ErrMsg.fatal(lineNum(), charNum(), "Type mismatch");
            retType = new ErrorType();
        }
        
        if (typeLhs.isErrorType() || typeExp.isErrorType()) {
            retType = new ErrorType();
        }
        
        return retType;
    }
    
	// *** unparse ***
    public void unparse(PrintWriter p, int indent) {
        if (indent != -1)  p.print("(");
        myLhs.unparse(p, 0);
        p.print(" = ");
        myExp.unparse(p, 0);
        if (indent != -1)  p.print(")");
    }
    protected void codeGenCommon(){
      myExp.codeGenExp();
      if(myLhs instanceof IdNode){
        ((IdNode)myLhs).codeGenLoc();
      } else{
        return;
      }
      Codegen.genPop(Codegen.T1);
      Codegen.genPop(Codegen.T0);
      Codegen.generateIndexed("sw",Codegen.T0,Codegen.T1,0);
    }

    // two kids
    private ExpNode myLhs;
    private ExpNode myExp;
}

class CallExpNode extends ExpNode {
    public CallExpNode(IdNode name, ExpListNode elist) {
        myId = name;
        myExpList = elist;
    }

    public CallExpNode(IdNode name) {
        myId = name;
        myExpList = new ExpListNode(new LinkedList<ExpNode>());
    }

    /***
     * Return the line number for this call node. 
     * The line number is the one corresponding to the function name.
     ***/
    public int lineNum() {
        return myId.lineNum();
    }
    
    /***
     * Return the char number for this call node.
     * The char number is the one corresponding to the function name.
     ***/
    public int charNum() {
        return myId.charNum();
    }
    
    /***
     * nameAnalysis
     * Given a symbol table symTab, perform name analysis on this node's 
     * two children
     ***/
    public void nameAnalysis(SymTable symTab) {
        myId.nameAnalysis(symTab);
        myExpList.nameAnalysis(symTab);
    }  
      
    /***
     * typeCheck
     ***/
    public Type typeCheck() {
        if (!myId.typeCheck().isFnType()) {  
            ErrMsg.fatal(myId.lineNum(), myId.charNum(), 
                         "Call attempt on non-function");
            return new ErrorType();
        }
        
        FnSym fnSym = (FnSym)(myId.sym());
        
        if (fnSym == null) {
            System.err.println("null sym for Id in CallExpNode.typeCheck");
            System.exit(-1);
        }
        
        if (myExpList.size() != fnSym.getNumParams()) {
            ErrMsg.fatal(myId.lineNum(), myId.charNum(), 
                         "Function call with wrong # of args");
            return fnSym.getReturnType();
        }
        
        myExpList.typeCheck(fnSym.getParamTypes());
        return fnSym.getReturnType();
    }
        
    // *** unparse ***
    public void unparse(PrintWriter p, int indent) {
        myId.unparse(p, 0);
        p.print("(");
        if (myExpList != null) {
            myExpList.unparse(p, 0);
        }
        p.print(")");
    }
    protected void codeGenCommon(){
      myExpList.codeGen();
      String jmpLabel;
      if(this.myId.name().equals("main")){
        jmpLabel = "main";
      } else{
        jmpLabel = "_"+this.myId.name();
      }
      Codegen.generate("jal",jmpLabel);
      Codegen.generate("add",Codegen.SP,((FnSym)this.myId.sym()).getParamsSize());
      if(!((FnSym)this.myId.sym()).getReturnType().isVoidType()){
        Codegen.genPush(Codegen.V0);
      }
    }
    public void codeGenExp(){
      this.codeGenCommon();
    }

    // two kids
    private IdNode myId;
    private ExpListNode myExpList;  // possibly null
}

abstract class UnaryExpNode extends ExpNode {
    public UnaryExpNode(ExpNode exp) {
        myExp = exp;
    }
    
    /***
     * Return the line number for this unary expression node. 
     * The line number is the one corresponding to the  operand.
     ***/
    public int lineNum() {
        return myExp.lineNum();
    }
    
    /***
     * Return the char number for this unary expression node.
     * The char number is the one corresponding to the  operand.
     ***/
    public int charNum() {
        return myExp.charNum();
    }
    
    /***
     * nameAnalysis
     * Given a symbol table symTab, perform name analysis on this node's child
     ***/
    public void nameAnalysis(SymTable symTab) {
        myExp.nameAnalysis(symTab);
    }
    
    // one child
    protected ExpNode myExp;
}

abstract class BinaryExpNode extends ExpNode {
    public BinaryExpNode(ExpNode exp1, ExpNode exp2) {
        myExp1 = exp1;
        myExp2 = exp2;
    }

    
    /***
     * Return the line number for this binary expression node. 
     * The line number is the one corresponding to the left operand.
     ***/
    public int lineNum() {
        return myExp1.lineNum();
    }
    
    /***
     * Return the char number for this binary expression node.
     * The char number is the one corresponding to the left operand.
     ***/
    public int charNum() {
        return myExp1.charNum();
    }
    
    /***
     * nameAnalysis
     * Given a symbol table symTab, perform name analysis on this node's 
     * two children
     ***/
    public void nameAnalysis(SymTable symTab) {
        myExp1.nameAnalysis(symTab);
        myExp2.nameAnalysis(symTab);
    }

    public void genCodeSubExp(){
      myExp1.codeGenExp();
      myExp2.codeGenExp();
      Codegen.genPop(Codegen.T1);
      Codegen.genPop(Codegen.T0);
    }
    
    // two kids
    protected ExpNode myExp1;
    protected ExpNode myExp2;
}

// **********************************************************************
// ******  Subclasses of UnaryExpNode
// **********************************************************************

class UnaryMinusNode extends UnaryExpNode {
    public UnaryMinusNode(ExpNode exp) {
        super(exp);
    }

    /***
     * typeCheck
     ***/
    public Type typeCheck() {
        Type type = myExp.typeCheck();
        Type retType = new IntType();
        
        if (!type.isErrorType() && !type.isIntType()) {
            ErrMsg.fatal(lineNum(), charNum(),
                         "Arithmetic operator with non-numeric operand");
            retType = new ErrorType();
        }
        
        if (type.isErrorType()) {
            retType = new ErrorType();
        }
        
        return retType;
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(-");
        myExp.unparse(p, 0);
        p.print(")");
    }
    protected void codeGenCommon(){
      myExp.codeGenExp();
      Codegen.genPop(Codegen.T0);
      Codegen.generate("li",Codegen.T1,-1);
      Codegen.generate("mult",Codegen.T0,Codegen.T1);
      Codegen.generate("mflo",Codegen.T0);
    }
}

class NotNode extends UnaryExpNode {
    public NotNode(ExpNode exp) {
        super(exp);
    }

    /***
     * typeCheck
     ***/
    public Type typeCheck() {
        Type type = myExp.typeCheck();
        Type retType = new BoolType();
        
        if (!type.isErrorType() && !type.isBoolType()) {
            ErrMsg.fatal(lineNum(), charNum(),
                         "Logical operator with non-bool operand");
            retType = new ErrorType();
        }
        
        if (type.isErrorType()) {
            retType = new ErrorType();
        }
        
        return retType;
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(!");
        myExp.unparse(p, 0);
        p.print(")");
    }
    protected void codeGenCommon(){
      myExp.codeGenExp();
      Codegen.genPop(Codegen.T0);
      Codegen.generate("seq",Codegen.T0,Codegen.T0,Codegen.FALSE);
    }
    public void codeGenJump(String trueLabel,String falseLabel){
      //TODO - look at deleting
    }
}

// **********************************************************************
// ******  Subclasses of BinaryExpNode
// **********************************************************************

abstract class ArithmeticExpNode extends BinaryExpNode {
    public ArithmeticExpNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }
    
    /***
     * typeCheck
     ***/
    public Type typeCheck() {
        Type type1 = myExp1.typeCheck();
        Type type2 = myExp2.typeCheck();
        Type retType = new IntType();
        
        if (!type1.isErrorType() && !type1.isIntType()) {
            ErrMsg.fatal(myExp1.lineNum(), myExp1.charNum(),
                         "Arithmetic operator with non-numeric operand");
            retType = new ErrorType();
        }
        
        if (!type2.isErrorType() && !type2.isIntType()) {
            ErrMsg.fatal(myExp2.lineNum(), myExp2.charNum(),
                         "Arithmetic operator with non-numeric operand");
            retType = new ErrorType();
        }
        
        if (type1.isErrorType() || type2.isErrorType()) {
            retType = new ErrorType();
        }
        
        return retType;
    }
}

abstract class LogicalExpNode extends BinaryExpNode {
    public LogicalExpNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }
    
    /***
     * typeCheck
     ***/
    public Type typeCheck() {
        Type type1 = myExp1.typeCheck();
        Type type2 = myExp2.typeCheck();
        Type retType = new BoolType();
        
        if (!type1.isErrorType() && !type1.isBoolType()) {
            ErrMsg.fatal(myExp1.lineNum(), myExp1.charNum(),
                         "Logical operator with non-bool operand");
            retType = new ErrorType();
        }
        
        if (!type2.isErrorType() && !type2.isBoolType()) {
            ErrMsg.fatal(myExp2.lineNum(), myExp2.charNum(),
                         "Logical operator with non-bool operand");
            retType = new ErrorType();
        }
        
        if (type1.isErrorType() || type2.isErrorType()) {
            retType = new ErrorType();
        }
        
        return retType;
    }
}

abstract class EqualityExpNode extends BinaryExpNode {
    public EqualityExpNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }
    
    /***
     * typeCheck
     ***/
    public Type typeCheck() {
        Type type1 = myExp1.typeCheck();
        Type type2 = myExp2.typeCheck();
        Type retType = new BoolType();
        
        if (type1.isVoidType() && type2.isVoidType()) {
            ErrMsg.fatal(lineNum(), charNum(),
                         "Equality operator used with void functions");
            retType = new ErrorType();
        }
        
        if (type1.isFnType() && type2.isFnType()) {
            ErrMsg.fatal(lineNum(), charNum(),
                         "Equality operator used with functions");
            retType = new ErrorType();
        }
        
        if (type1.isStructDefType() && type2.isStructDefType()) {
            ErrMsg.fatal(lineNum(), charNum(),
                         "Equality operator used with struct names");
            retType = new ErrorType();
        }
        
        if (type1.isStructType() && type2.isStructType()) {
            ErrMsg.fatal(lineNum(), charNum(),
                         "Equality operator used with struct variables");
            retType = new ErrorType();
        }        
        
        if (!type1.equals(type2) && !type1.isErrorType() && !type2.isErrorType()) {
            ErrMsg.fatal(lineNum(), charNum(),
                         "Type mismatch");
            retType = new ErrorType();
        }
        
        if (type1.isErrorType() || type2.isErrorType()) {
            retType = new ErrorType();
        }
        
        return retType;
    }
}

abstract class RelationalExpNode extends BinaryExpNode {
    public RelationalExpNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }
    
    /***
     * typeCheck
     ***/
    public Type typeCheck() {
        Type type1 = myExp1.typeCheck();
        Type type2 = myExp2.typeCheck();
        Type retType = new BoolType();
        
        if (!type1.isErrorType() && !type1.isIntType()) {
            ErrMsg.fatal(myExp1.lineNum(), myExp1.charNum(),
                         "Relational operator with non-numeric operand");
            retType = new ErrorType();
        }
        
        if (!type2.isErrorType() && !type2.isIntType()) {
            ErrMsg.fatal(myExp2.lineNum(), myExp2.charNum(),
                         "Relational operator with non-numeric operand");
            retType = new ErrorType();
        }
        
        if (type1.isErrorType() || type2.isErrorType()) {
            retType = new ErrorType();
        }
        
        return retType;
    }
}

class PlusNode extends ArithmeticExpNode {
    public PlusNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }
    
    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" + ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
  protected void codeGenCommon(){
    this.genCodeSubExp();
    Codegen.generate("add",Codegen.T0,Codegen.T1);
    Codegen.generate("mflo",Codegen.T0);
  }
}

class MinusNode extends ArithmeticExpNode {
    public MinusNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }
    
    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" - ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
  protected void codeGenCommon(){
    this.genCodeSubExp();
    Codegen.generate("sub",Codegen.T0,Codegen.T1);
    Codegen.generate("mflo",Codegen.T0);
  }
}

class TimesNode extends ArithmeticExpNode {
    public TimesNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    
    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" * ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
  protected void codeGenCommon(){
    this.genCodeSubExp();
    Codegen.generate("mult",Codegen.T0,Codegen.T1);
    Codegen.generate("mflo",Codegen.T0);
  }

}

class DivideNode extends ArithmeticExpNode {
    public DivideNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }
    
    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" / ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
    protected void codeGenCommon(){
      this.genCodeSubExp();
      Codegen.generate("div",Codegen.T0,Codegen.T1);
      Codegen.generate("mflo",Codegen.T0);
    }
}

class AndNode extends LogicalExpNode {
    public AndNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }
    
    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" && ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
    protected void codeGenCommon(){
      String rightExprLabel = Codegen.nextLabel();
      String endLabel = Codegen.nextLabel();
      myExp1.codeGenJump(rightExprLabel,endLabel);
      Codegen.genLabel(rightExprLabel);
      myExp2.codeGenCommon();
      Codegen.genLabel(endLabel);
    }
    public void codeGenJump(String trueLabel,String falseLabel){
      String rightExprLabel = Codegen.nextLabel();
      myExp1.codeGenJump(trueLabel,falseLabel);
      Codegen.genLabel(rightExprLabel);
      myExp2.codeGenJump(trueLabel,falseLabel);
    }
}

class OrNode extends LogicalExpNode {
    public OrNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }
    
    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" || ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
    protected void codeGenCommon(){
      String rightExprLabel = Codegen.nextLabel();
      String endLabel = Codegen.nextLabel();
      myExp1.codeGenJump(endLabel,rightExprLabel);
      Codegen.genLabel(rightExprLabel);
      myExp2.codeGenCommon();
      Codegen.genLabel(endLabel);
    }
    public void codeGenJump(String trueLabel,String falseLabel){
      String rightExprLabel = Codegen.nextLabel();
      myExp1.codeGenJump(trueLabel,falseLabel);
      Codegen.genLabel(rightExprLabel);
      myExp2.codeGenJump(trueLabel,falseLabel);
    }
}

class EqualsNode extends EqualityExpNode {
    public EqualsNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }
    
    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" == ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
  protected void codeGenCommon(){
    this.genCodeSubExp();
    Codegen.generate("seq",Codegen.T0,Codegen.T0,Codegen.T1);
  }
  public void codeGenJump(String trueLabel,String falseLabel){
    this.codeGenCommon();
    Codegen.generate("beq",Codegen.T0,Codegen.FALSE,falseLabel);
    Codegen.generate("b",trueLabel);
  }
}

class NotEqualsNode extends EqualityExpNode {
    public NotEqualsNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }
    
    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" != ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
  protected void codeGenCommon(){
    this.genCodeSubExp();
    Codegen.generate("sne",Codegen.T0,Codegen.T0,Codegen.T1);
  }
  public void codeGenJump(String trueLabel,String falseLabel){
    this.codeGenCommon();
    Codegen.generate("beq",Codegen.T0,Codegen.FALSE,falseLabel);
    Codegen.generate("b",trueLabel);
  }
}

class LessNode extends RelationalExpNode {
    public LessNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }
    
    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" < ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
  protected void codeGenCommon(){
    this.genCodeSubExp();
    Codegen.generate("sgt",Codegen.T0,Codegen.T0,Codegen.T1);
  }
  public void codeGenJump(String trueLabel,String falseLabel){
    this.codeGenCommon();
    Codegen.generate("beq",Codegen.T0,Codegen.FALSE,falseLabel);
    Codegen.generate("b",trueLabel);
  }
}

class GreaterNode extends RelationalExpNode {
    public GreaterNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" > ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
  protected void codeGenCommon(){
    this.genCodeSubExp();
    Codegen.generate("sgt",Codegen.T0,Codegen.T0,Codegen.T1);
  }
  public void codeGenJump(String trueLabel,String falseLabel){
    this.codeGenCommon();
    Codegen.generate("beq",Codegen.T0,Codegen.FALSE,falseLabel);
    Codegen.generate("b",trueLabel);
  }

}

class LessEqNode extends RelationalExpNode {
    public LessEqNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" <= ");
        myExp2.unparse(p, 0);
        p.print(")");
    }

  protected void codeGenCommon(){
    this.genCodeSubExp();
    Codegen.generate("sle",Codegen.T0,Codegen.T0,Codegen.T1);
  }
  public void codeGenJump(String trueLabel,String falseLabel){
    this.codeGenCommon();
    Codegen.generate("beq",Codegen.T0,Codegen.FALSE,falseLabel);
    Codegen.generate("b",trueLabel);
  }
}

class GreaterEqNode extends RelationalExpNode {
    public GreaterEqNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    protected void codeGenCommon(){
      this.genCodeSubExp();
      Codegen.generate("sge",Codegen.T0,Codegen.T0,Codegen.T1);
    }
    public void codeGenJump(String trueLabel,String falseLabel){
      this.codeGenCommon();
      Codegen.generate("beq",Codegen.T0,Codegen.FALSE,falseLabel);
      Codegen.generate("b",trueLabel);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" >= ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
}
