import java.util.*;
import java.io.*;

import java_cup.runtime.*;  // defines Symbol

/**
 * This program is to be used to test the minim scanner.
 * This version is set up to test all tokens, but more code is needed to test
 * other aspects of the scanner (e.g., input that causes errors, character
 * numbers, values associated with tokens)
 */
public class P2 {
  public static void main(String[] args) throws IOException {
    // exception may be thrown by yylex
    // test all tokens
    testAllTokens();
    CharNum.num = 1;

    // ADD CALLS TO OTHER TEST METHODS HERE

    testErrors();
    CharNum.num = 1;

    testComments();
    CharNum.num = 1;

    testCharAndLineNum();
    CharNum.num = 1;


  }

  /*
    Tester for line and char counts
    To test for lines we had the scanner go through and as it looped to a new line character increased the count then
    checked if the lineNum variable was incremented as well
    To check that the charCount was the same we took the stream and had it count chars as it went through
    We made sure that the final totals were both the same to make sure that the tokens had the same length
   */
  private static void testCharAndLineNum() throws IOException{
    // open input and output files
    FileReader inFile = null;
    try {
      inFile = new FileReader("nums.in");
        } catch (FileNotFoundException ex) {
      System.err.println("File comment.in not found.");
      System.exit(-1);
    } 
    // create and call the scanner
    Yylex scanner = new Yylex(inFile);
    Symbol token = scanner.next_token();
    int lineNum = 1;
    int characterNum = inFile.toString().length();
    int charComp =-1;
    while (token.sym != sym.EOF) {
       
        if(((TokenVal)token.value).linenum!=lineNum){
          System.out.println("Error with line numbering");
        }
        else{
          lineNum++;
          charComp += CharNum.num;
        }
       token = scanner.next_token();
    }
    if(charComp != characterNum){//check for final char count
      System.out.println(charComp);
      System.out.println(characterNum);
      System.out.println("Error with char Numbering");
    }
    inFile.close();

  }
  /*
  Just chekcing that the comment symbols are correctly parsed so that anything behind them aren't read.
  "In the end we should have 2 empty files since nothing should be read"

   */
  private static void testComments() throws IOException {
    // open input and output files
    FileReader inFile = null;
    PrintWriter outFile = null;
    try {
      inFile = new FileReader("comment.in");
      outFile = new PrintWriter(new FileWriter("comment.out"));
    } catch (FileNotFoundException ex) {
      System.err.println("File comment.in not found.");
      System.exit(-1);
    } catch (IOException ex) {
      System.err.println("comment.out cannot be opened.");
      System.exit(-1);
    }

    // create and call the scanner
    Yylex scanner = new Yylex(inFile);
    Symbol token = scanner.next_token();
    while (token.sym != sym.EOF) {
      switch (token.sym) {
        case sym.BOOL:
          outFile.println("bool");
          break;
        case sym.INT:
          outFile.println("int");
          break;
        case sym.VOID:
          outFile.println("void");
          break;
        case sym.TRUE:
          outFile.println("true");
          break;
        case sym.FALSE:
          outFile.println("false");
          break;
        case sym.STRUCT:
          outFile.println("struct");
          break;
        case sym.INPUT:
          outFile.println("input");
          break;
        case sym.DISP:
          outFile.println("disp");
          break;
        case sym.IF:
          outFile.println("if");
          break;
        case sym.ELSE:
          outFile.println("else");
          break;
        case sym.WHILE:
          outFile.println("while");
          break;
        case sym.RETURN:
          outFile.println("return");
          break;
        case sym.ID:
          outFile.println(((IdTokenVal) token.value).idVal);
          break;
        case sym.INTLITERAL:
          outFile.println(((IntLitTokenVal) token.value).intVal);
          break;
        case sym.STRINGLITERAL:
          outFile.println(((StrLitTokenVal) token.value).strVal);
          break;
        case sym.LCURLY:
          outFile.println("{");
          break;
        case sym.RCURLY:
          outFile.println("}");
          break;
        case sym.LPAREN:
          outFile.println("(");
          break;
        case sym.RPAREN:
          outFile.println(")");
          break;
        case sym.SEMICOLON:
          outFile.println(";");
          break;
        case sym.COMMA:
          outFile.println(",");
          break;
        case sym.DOT:
          outFile.println(".");
          break;
        case sym.WRITE:
          outFile.println("<<");
          break;
        case sym.READ:
          outFile.println(">>");
          break;
        case sym.PLUSPLUS:
          outFile.println("++");
          break;
        case sym.MINUSMINUS:
          outFile.println("--");
          break;
        case sym.PLUS:
          outFile.println("+");
          break;
        case sym.MINUS:
          outFile.println("-");
          break;
        case sym.TIMES:
          outFile.println("*");
          break;
        case sym.DIVIDE:
          outFile.println("/");
          break;
        case sym.NOT:
          outFile.println("!");
          break;
        case sym.AND:
          outFile.println("&&");
          break;
        case sym.OR:
          outFile.println("||");
          break;
        case sym.EQUALS:
          outFile.println("==");
          break;
        case sym.NOTEQUALS:
          outFile.println("!=");
          break;
        case sym.LESS:
          outFile.println("<");
          break;
        case sym.GREATER:
          outFile.println(">");
          break;
        case sym.LESSEQ:
          outFile.println("<=");
          break;
        case sym.GREATEREQ:
          outFile.println(">=");
          break;
        case sym.ASSIGN:
          outFile.println("=");
          break;
        default:
          outFile.println("Nothing there");
      } // end switch

      token = scanner.next_token();
    } // end while
    outFile.close();
  }
  /*
    to test the errors because we had to take the error messages from the error messages class we needed to access the
    Err stream to write it to a file. We did this by using a print stream and setting the err stream to outs and reading
    outs. From there we put it into a file and checked that the files are the same with the error messages
   */
  private static void testErrors() throws IOException{
    PrintStream originalErr = System.err;
    FileReader inFile = null;
    PrintStream outFile = null;
    PrintStream console = System.err;
    File out = null ;
    try {
      out = new File("./errTokens.out");
      inFile = new FileReader("./errTokens.in");
    } catch (FileNotFoundException ex) {
      System.err.println("File errTokens.in not found.");
      System.exit(-1);
    } 
    FileOutputStream outFileS = new FileOutputStream(out);//to write to a file
    PrintStream outs=  new PrintStream(outFileS);
    System.setErr(outs);//setting the error stream to outs
    Yylex scanner = new Yylex(inFile);
    Symbol token = scanner.next_token();
    while (token == null) {
      token = scanner.next_token();
    }
    System.setErr(console);//reset it so that we can read messages in the error console for the other error messages


  }

  /**
   * testAllTokens
   * <p>
   * Open and read from file allTokens.txt
   * For each token read, write the corresponding string to allTokens.out
   * If the input file contains all tokens, one per line, we can verify
   * correctness of the scanner by comparing the input and output files
   * (e.g., using a 'diff' command).
   */
  private static void testAllTokens() throws IOException {
    // open input and output files
    FileReader inFile = null;
    PrintWriter outFile = null;
    try {
      inFile = new FileReader("allTokens.in");
      outFile = new PrintWriter(new FileWriter("allTokens.out"));
    } catch (FileNotFoundException ex) {
      System.err.println("File allTokens.in not found.");
      System.exit(-1);
    } catch (IOException ex) {
      System.err.println("allTokens.out cannot be opened.");
      System.exit(-1);
    }

    // create and call the scanner
    Yylex scanner = new Yylex(inFile);
    Symbol token = scanner.next_token();
    while (token.sym != sym.EOF) {
      switch (token.sym) {
        case sym.BOOL:
          outFile.println("bool");
          break;
        case sym.INT:
          outFile.println("int");
          break;
        case sym.VOID:
          outFile.println("void");
          break;
        case sym.TRUE:
          outFile.println("true");
          break;
        case sym.FALSE:
          outFile.println("false");
          break;
        case sym.STRUCT:
          outFile.println("struct");
          break;
        case sym.INPUT:
          outFile.println("input");
          break;
        case sym.DISP:
          outFile.println("disp");
          break;
        case sym.IF:
          outFile.println("if");
          break;
        case sym.ELSE:
          outFile.println("else");
          break;
        case sym.WHILE:
          outFile.println("while");
          break;
        case sym.RETURN:
          outFile.println("return");
          break;
        case sym.ID:
          outFile.println(((IdTokenVal) token.value).idVal);
          break;
        case sym.INTLITERAL:
          outFile.println(((IntLitTokenVal) token.value).intVal);
          break;
        case sym.STRINGLITERAL:
          outFile.println(((StrLitTokenVal) token.value).strVal);
          break;
        case sym.LCURLY:
          outFile.println("{");
          break;
        case sym.RCURLY:
          outFile.println("}");
          break;
        case sym.LPAREN:
          outFile.println("(");
          break;
        case sym.RPAREN:
          outFile.println(")");
          break;
        case sym.SEMICOLON:
          outFile.println(";");
          break;
        case sym.COMMA:
          outFile.println(",");
          break;
        case sym.DOT:
          outFile.println(".");
          break;
        case sym.WRITE:
          outFile.println("<<");
          break;
        case sym.READ:
          outFile.println(">>");
          break;
        case sym.PLUSPLUS:
          outFile.println("++");
          break;
        case sym.MINUSMINUS:
          outFile.println("--");
          break;
        case sym.PLUS:
          outFile.println("+");
          break;
        case sym.MINUS:
          outFile.println("-");
          break;
        case sym.TIMES:
          outFile.println("*");
          break;
        case sym.DIVIDE:
          outFile.println("/");
          break;
        case sym.NOT:
          outFile.println("!");
          break;
        case sym.AND:
          outFile.println("&&");
          break;
        case sym.OR:
          outFile.println("||");
          break;
        case sym.EQUALS:
          outFile.println("==");
          break;
        case sym.NOTEQUALS:
          outFile.println("!=");
          break;
        case sym.LESS:
          outFile.println("<");
          break;
        case sym.GREATER:
          outFile.println(">");
          break;
        case sym.LESSEQ:
          outFile.println("<=");
          break;
        case sym.GREATEREQ:
          outFile.println(">=");
          break;
        case sym.ASSIGN:
          outFile.println("=");
          break;
        default:
          outFile.println("UNKNOWN TOKEN");
      } // end switch
      token = scanner.next_token();
    } // end while
    outFile.close();
  }
}
