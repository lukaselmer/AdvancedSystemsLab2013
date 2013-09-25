package ch.ethz.mlmq.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * Helper to initialize, update and drop a Database Schema
 * 
 * 
 */
public class DatabaseInitializer {

	private static final Logger logger = Logger.getLogger("default");

	private final String url;
	private final String userName;
	private final String password;
	private final String schemaName;

	/**
	 * This Initializer maintains it's own database connection
	 */
	private Connection connection = null;

	public DatabaseInitializer(String url, String userName, String password, String schemaName) {
		this.password = password;
		this.userName = userName;
		this.url = url;
		this.schemaName = schemaName;
	}

	/**
	 * Establish a database connection
	 * 
	 * @throws SQLException
	 */
	public void connect() throws SQLException {
		try {
			Class.forName("org.postgresql.Driver");
			connection = DriverManager.getConnection(url, userName, password);
		} catch (ClassNotFoundException e) {
			throw new SQLException(e);
		}
	}

	public void disconnect() throws SQLException {
		if (connection != null) {
			connection.close();
		}
	}

	public void createSchema() throws SQLException {
		//@formatter:off
		String sqlStatement = "" 
				+ " CREATE DATABASE " + schemaName 
				+ " WITH OWNER = " + userName
				+ " ENCODING = 'UTF8'"
				+ " TABLESPACE = pg_default"
				+ " LC_COLLATE = 'German_Switzerland.1252'"
				+ " LC_CTYPE = 'German_Switzerland.1252'"
				+ " CONNECTION LIMIT = -1;";
		//@formatter:on

		try (Statement stmt = connection.createStatement()) {
			logger.info("Executing " + sqlStatement);
			stmt.execute(sqlStatement);
		}
	}

	public void deleteSchema() throws SQLException {
		//@formatter:off
		String sqlStatement = "" 
				+ " DROP DATABASE " + schemaName
				+ ";";
		//@formatter:on

		try (Statement stmt = connection.createStatement()) {
			logger.info("Executing " + sqlStatement);
			stmt.execute(sqlStatement);
		}
	}
}