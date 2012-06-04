package db;

import git.BlameResultRecord;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.eclipse.jgit.diff.DiffEntry;

public class GitDb extends DbConnection
{
	public FileWriter writer;
	public BufferedWriter buff;
	public boolean connect(String dbName)
	{
		super.connect(dbName);
		try {
			callableBatch = conn.prepareCall("{call upsert_owner_rec(?,?,?,?,?,?,?) } ");
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	public GitDb () {
		super();
		try
		{
			writer = new FileWriter("dump.sql");
			buff = new BufferedWriter(writer);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean execBatch() {
		try {
			buff.write(currentBatch.toString());
			currentBatch.clearBatch();
			return true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean execCallableBatch() {
		try {
			buff.write(callableBatch.toString());
			callableBatch.clearBatch();
			return true;
		}
		catch (Exception e)
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
			s.setString(4, "");
			s.execute();
		}
		catch (Exception e)
		{
			return false;
		}
		return true;
	}
		
	public boolean InsertFileDiff(FileDiffsTO diff)
	{
		try { 
			PreparedStatement s = conn.prepareStatement(
					"INSERT INTO file_diffs (file_id, new_commit_id, old_commit_id, diff_text, char_start, char_end, diff_type)" +
					" VALUES(?, ?, ?, ?, ?, ?, ?);");
			s.setString(1, diff.getFile_id());
			s.setString(2, diff.getNewCommit_id());
			s.setString(3, diff.getOldCommit_id());
			s.setString(4, diff.getDiff_text());
			s.setInt(5, diff.getChar_start());
			s.setInt(6, diff.getChar_end());
			s.setString(7, diff.getDiff_type().toString());
			buff.write(s.toString());
		}
		catch (Exception e)
		{
			return false;
		}
		return true;
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
		    buff.write(s.toString());
	    }
	    catch (Exception e)
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
			buff.write(s.toString());
		}
		catch (Exception e)
		{
			return false;
		}
		return true;
	}
	
	public boolean InsertChangeEntry(String commitId, String fileId, DiffEntry.ChangeType type)
	{
		try { 
			PreparedStatement s = conn.prepareStatement(
					"INSERT INTO changes (commit_id, file_id, change_type)" +
					" VALUES(?, ?, ?);");
			s.setString(1, commitId);
			s.setString(2, fileId);
			s.setString(3, type.toString());
			currentBatch.addBatch(s.toString());
			return true;
		}
		catch (SQLException e)
		{
			e.printStackTrace();
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
	
	public void insertOwnerRecord(BlameResultRecord rec)
	{
		try
		{
			callableBatch.setString(1, rec.getCommitId());
			callableBatch.setString(2, rec.getSourceCommitId());
			callableBatch.setString(3, rec.getAuthorId());
			callableBatch.setString(4, rec.getFileId());
			callableBatch.setInt(5, rec.getLineStart());
			callableBatch.setInt(6, rec.getLineEnd());
			callableBatch.setString(7, rec.getType().toString());
			callableBatch.addBatch();
		}
		catch(SQLException e)
		{
			e.printStackTrace();
		}
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
			// First create the DB.
			s = conn.prepareStatement("CREATE DATABASE " + dbName + ";");
			// Now load our default schema in.
			sr.runScript(new InputStreamReader(this.getClass().getResourceAsStream("createdb.sql")));
			
			//--------------------------------------------------------------------------------------
			// Stored procedure for checking before inserting in a batch.											
			// http://stackoverflow.com/questions/1109061/insert-on-duplicate-update-postgresql			
			//--------------------------------------------------------------------------------------
			s = conn.prepareStatement(
					"CREATE FUNCTION upsert_owner_rec(c_id varchar(255), s_c_id varchar(255), a_id varchar(255), f_id varchar(255), c_start INT, c_end INT, c_type varchar(12)) RETURNS VOID AS" +
						"'" +
						" DECLARE " + 
							"dummy integer;" + 
						" BEGIN " +
							" LOOP " +
								" select owners.char_start into dummy from owners where commit_id=c_id and source_commit_id=s_c_id and owner_id=a_id and file_id=f_id and char_start=c_start and char_end=c_end and change_type=c_type;" +
								" IF found THEN " +
									" RETURN ;" +
								" END IF;" +
								" BEGIN " +
									" INSERT INTO owners VALUES (c_id, s_c_id, a_id, f_id, c_start, c_end, c_type);" +
									" RETURN; " +
								" EXCEPTION WHEN unique_violation THEN " +
								" END; " +
							" END LOOP;" +
						" END; " +
						"'" +
					" LANGUAGE plpgsql;");
			s.execute();
			
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public void insertCommitFamilyEntry(String commit, String parentCommit)
	{
		try { 
			PreparedStatement s = conn.prepareStatement(
					"INSERT INTO commit_family (parent, child)" +
					" VALUES(?, ?)");
			s.setString(1, parentCommit);
			s.setString(2, commit);
			currentBatch.addBatch(s.toString());
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}
	
	public boolean close() {
		try {
			conn.close();
			buff.close();
			return true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}
}
