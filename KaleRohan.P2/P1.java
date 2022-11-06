public class P1 {
  public static void main(String[] args) {


    System.out.println("Working correctly: " + runTests());
  }

  /*
    runs all my tests
   */
  public static boolean runTests() {
    return (testSym() && testSymTable());
  }

  /*
    test all cases for the Sym class
   */
  private static boolean testSym() {

    Sym sym = new Sym("");//test the empty string
    Sym sym1 = new Sym("" + 5);//test an int as a string
    Sym sym2 = new Sym("string");//normal input
    Sym sym3 = new Sym(null);//test null case - I'm thinking I'm just going to have it be an empty
    // string if this is passed in

    //test the toString method
    if (!sym.toString().equals("")) return false;
    if (!sym1.toString().equals("5")) return false;
    if (!sym2.toString().equals("string")) return false;
    if (!sym3.toString().equals("")) return false;

    //test the getType method
    if (!sym.getType().equals("")) return false;
    if (!sym1.getType().equals("5")) return false;
    if (!sym2.getType().equals("string")) return false;
    if (!sym3.getType().equals("")) return false;

    return true;
  }

  /*
  run all tests for the SymTable class
   */
  private static boolean testSymTable() {
    return testAddDecl() && testAddScope() && testLocalLookup() && testRemoveScope() && testPrint();
  }

  /*
  test print functionality
   */
  private static boolean testPrint() {
    SymTable table = new SymTable();

    //test when there is nothing in the scope
    table.print();

    //test when there are no maps
    try {
      table.removeScope();
    } catch (EmptySymTableException e) {
      e.printStackTrace();
      return false;
    }
    table.print();

    //regular case
    table.addScope();
    Sym sym1 = new Sym("Integer");
    Sym sym2 = new Sym("String");

    try {
      table.addDecl("int", sym1);
      table.addDecl("string", sym2);
    } catch (DuplicateSymException e) {
      e.printStackTrace();
      return false;
    } catch (EmptySymTableException e) {
      e.printStackTrace();
      return false;
    }
    table.print();


    return true;
  }

  /*
  test the removeScope method for the SymTable class
   */
  private static boolean testRemoveScope() {
    SymTable table = new SymTable();
    //usual case
    try {
      table.removeScope();
    } catch (EmptySymTableException e) {
      e.printStackTrace();
      return false;
    }

    boolean works = false;
    //should throw exception
    try {
      table.removeScope();
    } catch (EmptySymTableException e) {
      works = true;
    }
    if (!works) return works;

    //test multiple removes
    table.addScope();
    table.addScope();

    try {
      table.removeScope();
      table.removeScope();
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }


    return true;

  }

  /*
  test the localLookup method
   */
  private static boolean testLocalLookup() {
    SymTable table = new SymTable();

    try {
      table.removeScope();
    } catch (EmptySymTableException e) {
      return false;
    }
    //test that empty table excep is thrown
    boolean works = false;
    try {
      table.lookupLocal("int");
    } catch (EmptySymTableException e) {
      works = true;
    }
    if (!works) return works;


    table.addScope();
    Sym sym1 = new Sym("int");
    try {
      table.addDecl("int", sym1);
    } catch (DuplicateSymException e) {
      e.printStackTrace();
      return false;
    } catch (EmptySymTableException e) {
      e.printStackTrace();
      return false;
    }


    //check regular case
    Sym test;
    try {
      test = table.lookupLocal("int");
    } catch (EmptySymTableException e) {
      e.printStackTrace();
      return false;
    }

    if (test == null) return false;
    else if (!test.getType().equals(sym1.getType())) return false;
    else if (!test.getClass().equals(sym1.getClass())) return false;


    //test for when the name isn't in the table
    try {
      test = table.lookupLocal("wiggity");
    } catch (EmptySymTableException e) {
      e.printStackTrace();
      return false;
    }
    if (test != null) return false;


    //test for null case
    try {
      test = table.lookupLocal(null);
    } catch (EmptySymTableException e) {
      e.printStackTrace();
      return false;
    }
    if (test != null) return false;


    return true;
  }

  private static boolean testGlobalLookup() {
    SymTable table = new SymTable();

    try {
      table.removeScope();
    } catch (EmptySymTableException e) {
      return false;
    }
    //test that empty table excep is thrown
    boolean works = false;
    try {
      table.lookupGlobal("int");
    } catch (EmptySymTableException e) {
      works = true;
    }
    if (!works) return works;


    table.addScope();
    Sym sym1 = new Sym("int");
    try {
      table.addDecl("int", sym1);
    } catch (DuplicateSymException e) {
      e.printStackTrace();
      return false;
    } catch (EmptySymTableException e) {
      e.printStackTrace();
      return false;
    }

    table.addScope();
    Sym sym2 = new Sym("bit");
    try {
      table.addDecl("bit", sym2); //should be fine to add
    } catch (DuplicateSymException e) {
      e.printStackTrace();
      return false;
    } catch (EmptySymTableException e) {
      e.printStackTrace();
      return false;
    }


    //check regular case
    Sym test;
    try {
      test = table.lookupGlobal("int");
    } catch (EmptySymTableException e) {
      e.printStackTrace();
      return false;
    }

    if (test == null) return false;
    else if (!test.getType().equals(sym1.getType())) return false;
    else if (!test.getClass().equals(sym1.getClass())) return false;

    try {
      test = table.lookupGlobal("bit");
    } catch (EmptySymTableException e) {
      e.printStackTrace();
      return false;
    }

    if (test == null) return false;
    else if (!test.getType().equals(sym2.getType())) return false;
    else if (!test.getClass().equals(sym2.getClass())) return false;


    //test for when the name isn't in the table
    try {
      test = table.lookupGlobal("wiggity");
    } catch (EmptySymTableException e) {
      e.printStackTrace();
      return false;
    }
    if (test != null) return false;


    //test for null case
    try {
      test = table.lookupGlobal(null);
    } catch (EmptySymTableException e) {
      e.printStackTrace();
      return false;
    }
    if (test != null) return false;


    return true;
  }

  /*
  tests the addScope function
   */
  private static boolean testAddScope() {
    SymTable table = new SymTable();
    //if it throws an exception then its failed
    try {
      table.addScope();
    } catch (Exception e) {
      return false;
    }
    try {
      table.removeScope();
    } catch (EmptySymTableException e) {
      e.printStackTrace();
      return false;
    }
    try {
      table.removeScope();
    } catch (EmptySymTableException e) {
      e.printStackTrace();
      return false;
    }
    try {
      table.removeScope();
    } catch (EmptySymTableException e) {//there should be no added scopes left
      return true;
    }
    return false;
  }

  /*
  tests the addDecl method functionality
   */
  private static boolean testAddDecl() {
    SymTable table = new SymTable();//create new table
    Sym sym1 = new Sym("Integer");//new Sym

    //normal case
    try {
      table.addDecl("int", sym1);
    } catch (DuplicateSymException e) {//if any expections thrown return false
      e.printStackTrace();
      return false;
    } catch (EmptySymTableException e) {
      e.printStackTrace();
      return false;
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
      return false;
    }


    //test if the string passed in is null
    boolean works = false;
    try {
      table.addDecl(null, sym1);
    } catch (DuplicateSymException e) {
      e.printStackTrace();
      return false;
    } catch (EmptySymTableException e) {
      return false;
    } catch (IllegalArgumentException e) {//this should be thrown
      works = true;
    }
    if (!works) return works;


    works = false;
    try {
      table.addDecl("int", null);//tests the other case IllegalArgException should be thrown
    } catch (DuplicateSymException e) {
      e.printStackTrace();
      return false;
    } catch (EmptySymTableException e) {
      return false;
    } catch (IllegalArgumentException e) {
      works = true;
    }
    if (!works) return works;


    try {//removes scope to check for empty table
      table.removeScope();
    } catch (EmptySymTableException e) {
      e.printStackTrace();
      return false;
    }

    works = false;
    try {
      table.addDecl("int", sym1);
    } catch (DuplicateSymException e) {
      e.printStackTrace();
      return false;
    } catch (EmptySymTableException e) { //should be thrown
      works = true;
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
      return false;
    }
    if (!works) return works;


    //checks the order in which exceptions are thrown - IllegalArg should be after EmptySymTable
    works = false;
    try {
      table.addDecl(null, sym1);
    } catch (DuplicateSymException e) {
      e.printStackTrace();
      return false;
    } catch (IllegalArgumentException e) {
      return false;
    } catch (EmptySymTableException e) {
      works = true;
    }
    if (!works) return works;


    table.addScope();

    try {
      table.addDecl("int", sym1);
    } catch (DuplicateSymException e) {
      e.printStackTrace();
      return false;
    } catch (EmptySymTableException e) {
      e.printStackTrace();
      return false;
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
      return false;
    }
    //tests for Duplicate Sym Expection
    try {
      table.addDecl("int", sym1);
    } catch (DuplicateSymException e) {
      works = true;
    } catch (EmptySymTableException e) {
      e.printStackTrace();
      return false;
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
      return false;
    }
    if (!works) return works;


    //tests that the table contains the correct things
    Sym sym2;
    try {
      sym2 = table.lookupLocal("int");
    } catch (EmptySymTableException e) {
      e.printStackTrace();
      return false;
    }
    if (sym2 == null) return false;
    else if (!sym2.getType().equals(sym1.getType())) return false;
    else if (!sym2.getClass().equals(sym1.getClass())) return false;


    return true;
  }
}
