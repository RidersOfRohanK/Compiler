public class Sym {
  private String type;

  /**
   * @param type the type that we want this symbol to be
   */
  public Sym(String type) {
    if (type == null) type = "";//if there is no type then I'm just having an empty string
    this.type = type;
  }

  public String getType() {
    return type;
  }//return method to get type

  public String toString() {//toString returns the info for the Symbol which is the type
    return type;
  }
}
