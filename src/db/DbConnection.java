package db;

import java.io.FileReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import scm2pgsql.Resources;

public class DbConnection {
	public static Connection conn = null;
	public static DbConnection ref = null;
	public static ScriptRunner sr;
	public static Statement currentBatch;
	private DbConnection() 
	{
		try 
		{
			Class.forName("org.postgresql.Driver").newInstance();
		} 
		catch (InstantiationException e) 
		{
			e.printStackTrace();
		} 
		catch (IllegalAccessException e) 
		{
			e.printStackTrace();
		} 
		catch (ClassNotFoundException e) 
		{
			e.printStackTrace();
		}
	}
	
	public static DbConnection getInstance() 
	{
		if (ref == null)
			return (ref = new DbConnection());
		else
			return ref;
	}
	
	/**
	 * Executes a string of SQL on the current database
	 * NOTE: this assumes your sql is valid.
	 * @param sql
	 * @return true if successful
	 */
	public boolean exec(String sql)
	{
		try {
			PreparedStatement s = conn.prepareStatement(sql);
			s.execute();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	/**
	 * Executes a string of SQL on the current database
	 * NOTE: this assumes your sql is valid.
	 * @param sql
	 * @return true if successful
	 */
	public boolean execPreparedStatement(PreparedStatement s)
	{
		try {
			s.execute();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	/**
	 * Executes a sql script at the given path.
	 * @param absPath
	 * @return true if succcessful
	 */
	public boolean execScript(String absPath)
	{
		try {
			sr.runScript(new FileReader(absPath));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	/**
	 * Connects to the given database.  
	 * @param connectionString
	 * @return true if successful
	 */
	public boolean connect(String connectionString)
	{
		try {
			conn = DriverManager.getConnection(connectionString, Resources.dbUser, Resources.dbPassword);
			sr = new ScriptRunner(conn, false, true);
			sr.setLogWriter(null);
			currentBatch = conn.createStatement();
		} 
		catch (SQLException e) 
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	/**
	 * Creates a db on the current connection.
	 * @param dbName
	 * @return true for success
	 */
	public boolean createDB(String dbName)
	{
		PreparedStatement s;
		try {
			// Drop the DB if it already exists
			s = conn.prepareStatement("DROP DATABASE IF EXISTS " + dbName + ";");
			s.execute();
			
			// First create the DB.
			s = conn.prepareStatement("CREATE DATABASE " + dbName + ";");
			s.execute();
			
			// Reconnect to our new database.
			connect(Resources.dbUrl + dbName.toLowerCase());
			
			// Now load our default schema in.
			sr.runScript(new InputStreamReader(this.getClass().getResourceAsStream("scripts/createdb.sql")));
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	public boolean close()
	{
		try {
			conn.close();
			return true;
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean InsertCommit(CommitsTO commit)
	{
	    try {
		    PreparedStatement s = conn.prepareStatement(
					"INSERT INTO commits (id, commit_id, author, author_email, comments, commit_date," +
					"branch_id) VALUES(" +
					"default, ?, ?, ?, ?, '" + commit.getCommit_date().toString() + "', ?);");
			s.setString(1, commit.getCommit_id());
			s.setString(2, commit.getAuthor());
			s.setString(3, commit.getAuthor_email());
			s.setString(4, commit.getComment());
		    s.setString(5, commit.getBranch_id());
			s.execute();
	    }
	    catch (SQLException e)
	    {
	    	e.printStackTrace();
	    	return false;
	    }
	    return true;
	}
	
	public boolean InsertBranchEntry(BranchEntryTO branchEntry)
	{
		try { 
			PreparedStatement s = conn.prepareStatement(
					"INSERT INTO branches (branch_id, branch_name, commit_id)" +
					" VALUES(?, ?, ?);");
			s.setString(1, branchEntry.getBranch_id());
			s.setString(2, branchEntry.getBranch_name());
			s.setString(3, branchEntry.getCommit_id());
			s.execute();
		}
		catch (SQLException e)
		{
			return false;
		}
		return true;
	}
	
	public boolean InsertChangeEntry(String commitId, String fileId)
	{
		try { 
			PreparedStatement s = conn.prepareStatement(
					"INSERT INTO changes (commit_id, file_id)" +
					" VALUES(?, ?);");
			s.setString(1, commitId);
			s.setString(2, fileId);
			currentBatch.addBatch(s.toString());
			return true;
		}
		catch (SQLException e)
		{
			return false;
		}
	}
	
	public boolean InsertFileTreeEntry(String commitId, String fileId)
	{
		try { 
			PreparedStatement s = conn.prepareStatement(
					"INSERT INTO source_trees (commit_id, file_id)" +
					" VALUES(?, ?)");
			s.setString(1, commitId);
			s.setString(2, fileId);
			currentBatch.addBatch(s.toString());
			return true;
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean execBatch() {
		try {
			currentBatch.executeBatch();
			currentBatch.clearBatch();
			return true;
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean InsertFiles(FilesTO files)
	{
		try { 
			PreparedStatement s = conn.prepareStatement(
					"INSERT INTO files (id, file_id, file_name, commit_id, raw_file)" +
					" VALUES(default, ?, ?, ?, ?);");
			s.setString(1, files.getFile_id());
			s.setString(2, files.getFile_name());
			s.setString(3, files.getCommit_id());
			s.setString(4, files.getRaw_file());
			s.execute();
		}
		catch (SQLException e)
		{
			return false;
		}
		return true;
	}
}
