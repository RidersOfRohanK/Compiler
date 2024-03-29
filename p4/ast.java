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

    public void unparse(PrintWriter p, int indent) {
        myDeclList.unparse(p, indent);
    }
    public void nameAnalyze(){
        SymTable symTable = new SymTable();
        myDeclList.nameAnalyze(symTable);
    }
    // one kid
    private DeclListNode myDeclList;
}

class DeclListNode extends ASTnode {
    public DeclListNode(List<DeclNode> S) {
        myDecls = S;
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

    public SymTable nameAnalyzerFnBody(SymTable sTable){
        Hashtable<String,Integer> occ = new Hashtable<>();
        for(DeclNode decNod: myDecls){
            IdNode id = decNod.getMyId();
            String st = id.toString();
            occ.put(st,occ.getOrDefault(st, 0)+1);
            try {
                if(occ.get(st) <= 1 && sTable.lookupLocal(st) == null){
                    decNod.nameAnalyze(sTable);
                }
            } catch (EmptySymTableException e) {
                e.printStackTrace();
            }
            if(occ.get(st)>1){
                ErrMsg.fatal(id.getLineNum(), id.getCharNum(), "Identifier multiply-declared");
            }
        }
        return sTable;
    }
    public SymTable nameAnalyze(SymTable st){
        for(DeclNode dec: myDecls){
            dec.nameAnalyze(st);
        }
        return st;
    }

    public SymTable nameAnalyzeStructDecl(SymTable st, SymTable structSymTab){
        for(DeclNode dcNode: myDecls){
            VarDeclNode n = (VarDeclNode) dcNode;
            if(n.getSize() != VarDeclNode.NOT_STRUCT){
                n.nameAnalyzeStruct(st,structSymTab);
            }else{
                n.nameAnalyze(structSymTab);
            }
        }
        return st;
    }

    // list of kids (DeclNodes)
    private List<DeclNode> myDecls;
}

class FormalsListNode extends ASTnode {
    public FormalsListNode(List<FormalDeclNode> S) {
        myFormals = S;
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
    public List<String> getListOfTypes(){
        List<String> types = new LinkedList<>();
        for(FormalDeclNode fd: myFormals){
            types.add(fd.getType().toString());
        }
        return types;
    }
    public SymTable nameAnalyze(SymTable s){
        for(int i = 0 ;i< myFormals.size();i++){
            myFormals.get(i).nameAnalyze(s);
        }
        return s;
    }
    // list of kids (FormalDeclNodes)
    private List<FormalDeclNode> myFormals;
}

class FnBodyNode extends ASTnode {
    public FnBodyNode(DeclListNode declList, StmtListNode stmtList) {
        myDeclList = declList;
        myStmtList = stmtList;
    }

    public void unparse(PrintWriter p, int indent) {
        myDeclList.unparse(p, indent);
        myStmtList.unparse(p, indent);
    }
    public void nameAnalyze(SymTable s){
        myDeclList.nameAnalyzerFnBody(s);
        myStmtList.nameAnalyze(s);
    }
    // two kids
    private DeclListNode myDeclList;
    private StmtListNode myStmtList;
}

class StmtListNode extends ASTnode {
    public StmtListNode(List<StmtNode> S) {
        myStmts = S;
    }

    public void unparse(PrintWriter p, int indent) {
        Iterator<StmtNode> it = myStmts.iterator();
        while (it.hasNext()) {
            it.next().unparse(p, indent);
        }
    }
    public SymTable nameAnalyze(SymTable s){
        for(int i = 0 ;i< myStmts.size();i++){
            myStmts.get(i).nameAnalyze(s);
        }
        return s;
    }

    // list of kids (StmtNodes)
    private List<StmtNode> myStmts;
}

class ExpListNode extends ASTnode {
    public ExpListNode(List<ExpNode> S) {
        myExps = S;
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
    public SymTable nameAnalyze(SymTable s){
        for(int i = 0 ;i<  myExps.size();i++){
            myExps.get(i).nameAnalyze(s);
        }
        return s;
    }

    // list of kids (ExpNodes)
    private List<ExpNode> myExps;
}

// **********************************************************************
// ******  DeclNode and its subclasses
// **********************************************************************

abstract class DeclNode extends ASTnode {
    abstract IdNode getMyId();
    abstract public SymTable nameAnalyze(SymTable symtable);

}

class VarDeclNode extends DeclNode {
    public VarDeclNode(TypeNode type, IdNode id, int size) {
        myType = type;
        myId = id;
        mySize = size;
    }

    

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myType.unparse(p, 0);
        p.print(" ");
        myId.unparse(p, 0);
        p.println(";");
    }
    public IdNode getMyId(){
        return myId;
    }
    public SymTable nameAnalyze(SymTable symTable){
        if(myType.type() == "void"){
            ErrMsg.fatal(myId.getLineNum(), myId.getCharNum(), "Non-function declared void");
            return symTable;
        }
        if (myType instanceof StructNode) {
            boolean r= this.nameAnalyzeStructName(symTable);
            Sym sSym = null;
            try {
                sSym = symTable.lookupGlobal(((StructNode)myType).getMyId().toString());
            } catch (EmptySymTableException e1) {
                e1.printStackTrace();
            }
            if(sSym == null || r == false){
                return symTable;
            }
            nameAnalyzeVaribleName(symTable);
           Sym mySym = null;
            try {
                mySym = symTable.lookupGlobal(myId.toString());
            } catch (EmptySymTableException e) {
                e.printStackTrace();
            }
            myId.setStruct(sSym.getStruct(), mySym);
            return symTable;
        }
        nameAnalyzeVaribleName(symTable);
        return symTable;
    }

    public void nameAnalyzeVaribleName(SymTable s){
        Sym nS = new Sym(myType.type());
        try{
            s.addDecl(this.myId.getMyStrVal(), nS);
        }catch(DuplicateSymException e){
            ErrMsg.fatal(myId.getLineNum(),myId.getCharNum(),"Identifier multiply-declared");
        }
        catch (EmptySymTableException ex) {
            System.out.println(ex);
        }
    }
    public void nameAnalyzeStruct(SymTable symTable, SymTable symTableStruct) {
        this.nameAnalyzeVaribleName(symTableStruct);
        this.nameAnalyzeStructName(symTable);
        if (myType instanceof StructNode) {
            Sym structSym = null;
            try{
             structSym = symTable.lookupGlobal(((StructNode)myType).getMyId().toString());
            }catch(EmptySymTableException e){
                e.printStackTrace();
            }
            Sym mySym = null;
            try{
             mySym = symTableStruct.lookupGlobal(myId.toString());
            }catch(EmptySymTableException e){
                e.printStackTrace();
            }
            if(structSym!=null && mySym != null){
                myId.setStruct(structSym.getStruct(), mySym); 
            }
        }
    }

    public boolean nameAnalyzeStructName(SymTable s){
        IdNode struct = ((StructNode) myType).getMyId();
       Sym sym = null;
    try {
        sym = s.lookupGlobal(struct.getMyStrVal());
    } catch (EmptySymTableException e) {
        e.printStackTrace();
    }
        if(sym == null|| !sym.getType().equals("struct-decl")){
            ErrMsg.fatal(struct.getLineNum(),struct.getCharNum(),"Name of struct type invalid");
            return false;
        }
        return true;
    }



    public int getSize(){
        return mySize;
    }
    public IdNode getIdNode(){
        return myId;
    }
  
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

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myType.unparse(p, 0);
        p.print(" ");
        myId.unparse(p, 0);
        p.print("(");
        myFormalsList.unparse(p, 0);
        p.println(") {");
        myBody.unparse(p, indent+4);
        p.println("}\n");
    }
    public IdNode getMyId(){
        return myId;
    }
    public TypeNode getType(){
        return myType;
    }
    public SymTable nameAnalyze(SymTable symTable){
        List<String> paramTypes = myFormalsList.getListOfTypes();
        Sym sy = null;
        try{
             sy = new FuncSym(myType.toString(), (LinkedList<String>) paramTypes);
            symTable.addDecl(this.myId.toString(), sy);
        } catch  (DuplicateSymException ex){
            ErrMsg.fatal(this.myId.getLineNum(), this.myId.getCharNum(), 
           "Identifier multiply-declared");
        } catch (EmptySymTableException ex) {
            System.out.println(ex);
        }
        symTable.addScope();
        myFormalsList.nameAnalyze(symTable);
        myBody.nameAnalyze(symTable);
        try{
            symTable.removeScope();
        } catch (EmptySymTableException ex) {
            System.out.println(ex);
        }
        return symTable;
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

    public void unparse(PrintWriter p, int indent) {
        myType.unparse(p, 0);
        p.print(" ");
        myId.unparse(p, 0);
    }

    public SymTable nameAnalyze(SymTable s){
       Sym sym = new Sym(myType.toString());
        try{
            s.addDecl(myId.getMyStrVal(), sym);
        }
        catch(DuplicateSymException ex){
            ErrMsg.fatal(myId.getLineNum(), myId.getCharNum(),"Identifier multiply-declared" );
        }catch (EmptySymTableException ex) {
            System.out.println(ex);
        }
        return s;
    }
    public IdNode getMyId(){
        return myId;
    }
    public TypeNode getType(){
        return myType;
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

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("struct ");
        myId.unparse(p, 0);
        p.println("{");
        myDeclList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("};\n");

    }
    public SymTable nameAnalyze(SymTable s){
       Sym sym = new Sym("struct-decl");
        try{
            s.addDecl(myId.getMyStrVal(), sym);
        }
        catch(DuplicateSymException ex){
            ErrMsg.fatal(myId.getLineNum(), myId.getCharNum(),"Identifier multiply-declared" );
        }catch (EmptySymTableException ex) {
            System.out.println(ex);
        }
        SymTable st = new SymTable();
        myId.setStruct(this, sym);
        myDeclList.nameAnalyzeStructDecl(s,st);
        return s;
    }

    public SymTable getSymTable(){
        return mSymTable;
    }
    public IdNode getMyId(){
        return myId;
    }

    // two kids
    private IdNode myId;
    private DeclListNode myDeclList;
    private SymTable mSymTable;
}

// **********************************************************************
// ******  TypeNode and its Subclasses
// **********************************************************************

abstract class TypeNode extends ASTnode {
    public abstract String type();
    @Override
    public abstract String toString();
}

class IntNode extends TypeNode {
    public IntNode() {
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("int");
    }
    public String type(){
        return "int";
    }
    @Override
    public String toString(){
        return "int";
    }

}

class BoolNode extends TypeNode {
    public BoolNode() {
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("bool");
    }
    public String type(){
        return "bool";
    }
    @Override
    public String toString(){
        return "bool";
    }
}

class VoidNode extends TypeNode {
    public VoidNode() {
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("void");
    }
    public String type(){
        return "void";
    }
    @Override
    public String toString(){
        return "void";
    }
}

class StructNode extends TypeNode {
    public StructNode(IdNode id) {
        myId = id;
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("struct ");
        myId.unparse(p, 0);
    }
    public String type(){
        return myId.toString();
    }
    public IdNode getMyId(){
        return myId;
    }
    @Override
    public String toString(){
        return "Struct";
    }
    // one kid
    private IdNode myId;
}

// **********************************************************************
// ******  StmtNode and its subclasses
// **********************************************************************

abstract class StmtNode extends ASTnode {
    public abstract void nameAnalyze(SymTable s);
}

class AssignStmtNode extends StmtNode {
    public AssignStmtNode(AssignExpNode assign) {
        myAssign = assign;
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myAssign.unparse(p, -1); // no parentheses
        p.println(";");
    }
    public void nameAnalyze(SymTable s){
        myAssign.nameAnalyze(s);
    }
    // one kid
    private AssignExpNode myAssign;
}

class PostIncStmtNode extends StmtNode {
    public PostIncStmtNode(ExpNode exp) {
        myExp = exp;
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myExp.unparse(p, 0);
        p.println("++;");
    }
    public void nameAnalyze(SymTable s){
        myExp.nameAnalyze(s);
    }
    // one kid
    private ExpNode myExp;
}

class PostDecStmtNode extends StmtNode {
    public PostDecStmtNode(ExpNode exp) {
        myExp = exp;
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myExp.unparse(p, 0);
        p.println("--;");
    }
    public void nameAnalyze(SymTable s){
        myExp.nameAnalyze(s);
    }

    // one kid
    private ExpNode myExp;
}

class ReadStmtNode extends StmtNode {
    public ReadStmtNode(ExpNode e) {
        myExp = e;
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("input >> ");
        myExp.unparse(p, 0);
        p.println(";");
    }
    public void nameAnalyze(SymTable s){
        myExp.nameAnalyze(s);
    }

    private ExpNode myExp;
}

class WriteStmtNode extends StmtNode {
    public WriteStmtNode(ExpNode exp) {
        myExp = exp;
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("disp << ");
        myExp.unparse(p, 0);
        p.println(";");
    }
    public void nameAnalyze(SymTable s){
        myExp.nameAnalyze(s);
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
    
    public void nameAnalyze(SymTable symTable){
        myExp.nameAnalyze(symTable);
        symTable.addScope();
        myDeclList.nameAnalyze(symTable);
        myStmtList.nameAnalyze(symTable);
        try{
            symTable.removeScope();
        } catch(EmptySymTableException e){
            System.out.println(e);
        }
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

    public void nameAnalyze(SymTable symTable){
        myExp.nameAnalyze(symTable);
        symTable.addScope();
        myThenDeclList.nameAnalyze(symTable);
        myThenStmtList.nameAnalyze(symTable);
        try{
            symTable.removeScope();
        } catch (EmptySymTableException e) {
            e.printStackTrace();
        }

        symTable.addScope();
        myElseDeclList.nameAnalyze(symTable);
        myElseStmtList.nameAnalyze(symTable);
        try{
            symTable.removeScope();
        } catch (EmptySymTableException e) {
            e.printStackTrace();
        }
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

    public void nameAnalyze(SymTable symTable){
        myExp.nameAnalyze(symTable);
        symTable.addScope();
        myDeclList.nameAnalyze(symTable);
        myStmtList.nameAnalyze(symTable);
        try{
            symTable.removeScope();
        } catch (EmptySymTableException ex) {
            System.out.println(ex);
        }
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

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myCall.unparse(p, indent);
        p.println(";");
    }
    public void nameAnalyze(SymTable symTable){
        myCall.nameAnalyze(symTable);
    }

    // one kid
    private CallExpNode myCall;
}

class ReturnStmtNode extends StmtNode {
    public ReturnStmtNode(ExpNode exp) {
        myExp = exp;
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

    public void nameAnalyze(SymTable symTable){
        if(myExp!=null) {
            myExp.nameAnalyze(symTable);
        }
    }

    // one kid
    private ExpNode myExp; // possibly null
}

// **********************************************************************
// ******  ExpNode and its subclasses
// **********************************************************************

 abstract class ExpNode extends ASTnode {
    public abstract void nameAnalyze(SymTable symTable);
}

class IntLitNode extends ExpNode {
    public IntLitNode(int lineNum, int charNum, int intVal) {
        myLineNum = lineNum;
        myCharNum = charNum;
        myIntVal = intVal;
    }
    public void nameAnalyze(SymTable symTable){}

    public void unparse(PrintWriter p, int indent) {
        p.print(myIntVal);
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
    public void nameAnalyze(SymTable symTable){}

    public void unparse(PrintWriter p, int indent) {
        p.print(myStrVal);
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
    public void nameAnalyze(SymTable symTable){}
    public void unparse(PrintWriter p, int indent) {
        p.print("true");
    }

    private int myLineNum;
    private int myCharNum;
}

class FalseNode extends ExpNode {
    public FalseNode(int lineNum, int charNum) {
        myLineNum = lineNum;
        myCharNum = charNum;
    }
    public void nameAnalyze(SymTable symTable){}

    public void unparse(PrintWriter p, int indent) {
        p.print("false");
    }

    private int myLineNum;
    private int myCharNum;
}



 class IdNode extends ExpNode {//TODO
    public IdNode(int lineNum, int charNum, String strVal) {
        myLineNum = lineNum;
        myCharNum = charNum;
        myStrVal = strVal;
    }

    public void nameAnalyze(SymTable symTable){
        Sym s =null;
        try {
           s = symTable.lookupGlobal(myStrVal);
        } catch (EmptySymTableException e) {
            e.printStackTrace();
        }
        if(s== null){
            ErrMsg.fatal(myLineNum, myCharNum,
                    "Identifier undeclared");
        }else{
            this.myStruct = s.getStruct();
            setSym(s);
        }
        return;
    }

    public void unparse(PrintWriter p, int indent) {
        if(mySym != null){
            // if(mySym instanceof TypeNode){
            //     p.print(myStrVal+"("+mySym +")");
            // }
            p.print(myStrVal+"("+mySym.getType()+")");
        }else{
            p.print(myStrVal);
        }
        

    }

    public String getMyStrVal(){
        return myStrVal;
    }
    public int getCharNum(){
        return myCharNum;
    }
 
    public int getLineNum(){
        return myLineNum;
    }
    
    public Sym getMySym(){
        return mySym;
    }
    public StructDeclNode getStruct(){
        return myStruct;
    }
    public void setSym(Sym mySym){
        this.mySym = mySym;
    }
    public void setStruct(StructDeclNode myStruct, Sym sym){
        this.myStruct = myStruct;
        sym.setStruct(myStruct);
        }
    private int myLineNum;
    private int myCharNum;
    private String myStrVal;
    private Sym mySym;
    private StructDeclNode myStruct;
}



class DotAccessExpNode extends ExpNode {
    public DotAccessExpNode(ExpNode loc, IdNode id) {
        myLoc = loc;
        myId = id;
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myLoc.unparse(p, 0);
        p.print(").");
        myId.unparse(p, 0);
    }
    public void nameAnalyze(SymTable symTable){
        myLoc.nameAnalyze(symTable);
        StructDeclNode lhs = this.getLHSStruct(symTable);
        if(lhs == null){
            return;
        }
        SymTable leftTable = lhs.getSymTable();
        Sym foundItem  = null;
      try{
        foundItem = leftTable.lookupGlobal(myId.toString());
      }catch( EmptySymTableException e){
          e.printStackTrace();
      }
        if(foundItem == null) {
            ErrMsg.fatal(((IdNode)myId).getLineNum(), 
            ((IdNode)myId).getCharNum(),  "Invalid struct field name");
        }else{
            myId.setSym(foundItem);
        }
            
    }

    private StructDeclNode getLHSStruct(SymTable symTable){
        
        if(myLoc instanceof IdNode){
            // get the sym for this id
            Sym lookUpSym  = null;
            try{
                 lookUpSym = symTable.lookupGlobal(((IdNode)myLoc).toString());
            } catch( EmptySymTableException e){
                e.printStackTrace();
            }
            if(lookUpSym == null){
           
                return null;
            }
            if(lookUpSym.getStruct() == null){
              
                ErrMsg.fatal(((IdNode)myLoc).getLineNum(), 
                ((IdNode)myLoc).getCharNum(), 
                "Dot-access of non-struct type");
                return null;
            }
            return ((IdNode)myLoc).getStruct();
        }else{
            StructDeclNode lhs = ((DotAccessExpNode) myLoc).getLHSStruct(symTable);
            if(lhs == null){
                return null;
            }
            SymTable leftTable = lhs.getSymTable();
         
            Sym foundItem = null;
            try{
                foundItem = leftTable.lookupGlobal(((DotAccessExpNode)myLoc).myId.toString());
            }catch( EmptySymTableException e){
                e.printStackTrace();
            }
            if(foundItem==null){
                return null;
            }else{
             
                return foundItem.getStruct();
            }
            
        }
    }
 
    // two kids
    private ExpNode myLoc;
    private IdNode myId;
 
}

class AssignExpNode extends ExpNode {
    public AssignExpNode(ExpNode lhs, ExpNode exp) {
        myLhs = lhs;
        myExp = exp;
    }

    // ** unparse **
    public void unparse(PrintWriter p, int indent) {
        if (indent != -1)  p.print("(");
        myLhs.unparse(p, 0);
        p.print(" = ");
        myExp.unparse(p, 0);
        if (indent != -1)  p.print(")");
    }

    public void nameAnalyze(SymTable symTable){
        myLhs.nameAnalyze(symTable);
        myExp.nameAnalyze(symTable);
    }

    
    private ExpNode myLhs;
    private ExpNode myExp;
   
}

class CallExpNode extends ExpNode {
    public CallExpNode(IdNode name, ExpListNode elist) {
        myId = name;
        myExpList = elist;
    }
    public void nameAnalyze(SymTable symTable){
        myId.nameAnalyze(symTable);
        myExpList.nameAnalyze(symTable);
    }

    public CallExpNode(IdNode name) {
        myId = name;
        myExpList = new ExpListNode(new LinkedList<ExpNode>());
    }

    public void unparse(PrintWriter p, int indent) {
        myId.unparse(p, 0);
        p.print("(");
        if (myExpList != null) {
            myExpList.unparse(p, 0);
        }
        p.print(")");
    }

    // two kids
    private IdNode myId;
    private ExpListNode myExpList;  // possibly null
}

abstract class UnaryExpNode extends ExpNode {
    public UnaryExpNode(ExpNode exp) {
        myExp = exp;
    }

    public void nameAnalyze(SymTable symTable){
        myExp.nameAnalyze(symTable);
    }
    // one kid
    protected ExpNode myExp;
}

abstract class BinaryExpNode extends ExpNode {
    public BinaryExpNode(ExpNode exp1, ExpNode exp2) {
        myExp1 = exp1;
        myExp2 = exp2;
    }

    public void nameAnalyze(SymTable symTable){
        myExp1.nameAnalyze(symTable);
        myExp2.nameAnalyze(symTable);
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

    public void unparse(PrintWriter p, int indent) {
        p.print("(-");
        myExp.unparse(p, 0);
        p.print(")");
    }
}

class NotNode extends UnaryExpNode {
    public NotNode(ExpNode exp) {
        super(exp);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(!");
        myExp.unparse(p, 0);
        p.print(")");
    }
}

// **********************************************************************
// ******  Subclasses of BinaryExpNode
// **********************************************************************

class PlusNode extends BinaryExpNode {
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
}

class MinusNode extends BinaryExpNode {
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
}

class TimesNode extends BinaryExpNode {
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
}

class DivideNode extends BinaryExpNode {
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
}

class AndNode extends BinaryExpNode {
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
}

class OrNode extends BinaryExpNode {
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
}

class EqualsNode extends BinaryExpNode {
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
}

class NotEqualsNode extends BinaryExpNode {
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
}

class LessNode extends BinaryExpNode {
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
}

class GreaterNode extends BinaryExpNode {
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
}

class LessEqNode extends BinaryExpNode {
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
}

class GreaterEqNode extends BinaryExpNode {
    public GreaterEqNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" >= ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
}
