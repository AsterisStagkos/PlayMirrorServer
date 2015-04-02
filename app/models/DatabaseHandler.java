package models;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class DatabaseHandler {
	private static Connection dbConnection = null;
	
	 public static void enterUpdateStatement(String statement) throws SQLException, URISyntaxException {
		 Connection connection = getConnection();
	        
	        Statement stmt = connection.createStatement();
	        stmt.executeUpdate(statement);
	        System.out.println("Entered statement to database: " + statement);

	 }
	 public static String enterSelectStatement(String statement) throws SQLException, URISyntaxException {
		 Connection connection = getConnection();
		 Statement stmt = connection.createStatement();
		 ArrayList<String> returnColumns = new ArrayList<String>();
		 String returnColumn = "";
		 String toReturn = "";
		 ResultSet rs = stmt.executeQuery(statement);
		 statement = statement.replace(",", " ");
		 StringTokenizer statementTokens = new StringTokenizer(statement);
		 if (statementTokens.hasMoreTokens()) {
			 // skip first token (SELECT)
			 statementTokens.nextToken();
		 }
		 // Get columns to fetch
		 while (statementTokens.hasMoreTokens()) {
			 returnColumn = statementTokens.nextToken();
			 if (!returnColumn.equals("FROM")) {
				 returnColumns.add(returnColumn);
			 } else {
				 break;
			 }
		 }
	        while (rs.next()) {
	        	String result = "";
	        	if (returnColumns.get(0).equals("*")) {
	        		int columnCount = rs.getMetaData().getColumnCount();
	        		for (int i = 1; i <=columnCount; i++) {
	        			result += rs.getString(i) + " ";
	        		}
	        	} else {
	        	
	           for (int i = 0; i < returnColumns.size(); i++) {
	        	   result += rs.getString(returnColumns.get(i)) + " ";
	           }
	        	}
	           toReturn += result + " \n";
	           System.out.println("QUERY RESULTS: " + result);
	        }
	        
	        System.out.println("Fetching info from database with statement: " + statement);
	     return toReturn;
	 }

	 private static Connection getConnection() throws URISyntaxException, SQLException {
		 	if (dbConnection != null) {
		 		return dbConnection;
		 	} else {
	        URI dbUri = new URI(System.getenv("DATABASE_URL"));

	        String username = dbUri.getUserInfo().split(":")[0];
	        String password = dbUri.getUserInfo().split(":")[1];
	        String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + dbUri.getPath();
	        dbConnection = DriverManager.getConnection(dbUrl, username, password);
	        return dbConnection;
		 	}
	    }
}
