package scm2pgsql;

import java.io.IOException;
import org.eclipse.jgit.errors.MissingObjectException;
import git.GitParser;
import db.DbConnection;

public class Main {
	public static GitParser gitParser = new GitParser();
	public static void main(String[] args)
	{
		System.out.println("Scm2Pgsql tool developed by eggnet.");
		try {
			System.out.println(args.length);
			if (args.length < 1)
			{
				throw new ArrayIndexOutOfBoundsException();
			}
			else
			{
				try 
				{
					gitParser.parseRepo(".git");
				} 
				catch (MissingObjectException e) 
				{
					e.printStackTrace();
				} 
				catch (IOException e) 
				{
					e.printStackTrace();
				}
			}
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
			System.out.println("Usage scm2pgsql <input repository>");
		}
	}
}
