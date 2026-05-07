package fatorial;
import fatorial.lexer.*;
import fatorial.node.*;
import java.io.*;


public class Main {
	
	public static void main(String[] args) {
		
		try
		{
			String arquivo = "teste/fatorial.dpp";

			Lexer lexer =
					new Lexer(
							new PushbackReader(  
									new FileReader(arquivo), 1024)); 
			Token token;
			while(!((token = lexer.next()) instanceof EOF)) {
				System.out.println(token.getClass());
				System.out.println(" ( "+token.toString()+")");
			}
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
		}
		
		
		
	}

}
