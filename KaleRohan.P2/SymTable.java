import java.util.*;

public class SymTable {
  //private List of HashMaps of String Symbol
  private List<HashMap<String, Sym>> symbolTable = new ArrayList<>();

  //empty constructor adds a new HashMap
  public SymTable() {
    symbolTable.add(new HashMap<>());
  }


  /**
   * @param name name for the symbol
   * @param sym  the symbol being passed in
   * @throws DuplicateSymException  thrown if the name is already in the list
   * @throws EmptySymTableException thrown if size is 0
   * Adds a new Declaration for a name to a symbol
   */
  public void addDecl(String name, Sym sym) throws DuplicateSymException, EmptySymTableException {
    if (symbolTable.size() <= 0) {
      throw new EmptySymTableException();
    }//if either are null throw an Illegal Arg Exception
    if (name == null || sym == null) {
      throw new IllegalArgumentException();
    }
    if (symbolTable.get(0).containsKey(name)) {
      throw new DuplicateSymException();
    }//if all parameters are met then add
    symbolTable.get(0).put(name, sym);
  }

  /**
   * adds a new scope to the list
   */
  public void addScope() {
    symbolTable.add(0, new HashMap<>());
  }

  /**
   * @param name the name to check for
   * @return the symbol if it is in the table, null if not
   * @throws EmptySymTableException throw if there is no table to lookup in
   * Looks to see if the name is in the first hashmap
   */
  public Sym lookupLocal(String name) throws EmptySymTableException {
    if (symbolTable.size() <= 0) {
      throw new EmptySymTableException();
    }
    if (symbolTable.get(0).containsKey(name)) {//just use the hash function search
      return symbolTable.get(0).get(name);
    } else {
      return null;
    }
  }

  /**
   * @param name name being searched for
   * @return null if not found otherwise the Symbol in the first hashmap it is found in
   * @throws EmptySymTableException if the list size is 0
   */
  public Sym lookupGlobal(String name) throws EmptySymTableException {
    if(symbolTable.size()<=0){
      throw new EmptySymTableException();
    }
    for (int i = 0; i < symbolTable.size(); i++) {
       if(symbolTable.get(i).containsKey(name)){//get the hashmap then check if it has it
         return symbolTable.get(i).get(name);
       }
    }
    return null;
  }

  /**
   * @throws EmptySymTableException if there is no scope to be removed throws an exception
   * removes a hash table from the list if there are 1 or more scopes
   */
  public void removeScope() throws EmptySymTableException {
    if (symbolTable.size() <= 0) {
      throw new EmptySymTableException();
    }
    symbolTable.remove(0);
  }

  /**
   * prints the Symbol Table by going through each hashmap and calling it's toString function
   */
  public void print() {
    System.out.print("\n** Sym Table **\n");
    for (int i = 0; i < symbolTable.size(); i++) {//iterates through all the hashmaps
      System.out.print(symbolTable.get(i).toString());
      System.out.println();
    }
    System.out.println();
  }
}
