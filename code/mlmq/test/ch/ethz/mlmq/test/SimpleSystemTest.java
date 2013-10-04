package ch.ethz.mlmq.test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ch.ethz.mlmq.logging.LoggerUtil;
import ch.ethz.mlmq.server.Broker;
import ch.ethz.mlmq.server.BrokerConfiguration;
import ch.ethz.mlmq.server.db.util.DatabaseInitializer;

public class SimpleSystemTest {

	private final Logger logger = Logger.getLogger(SimpleSystemTest.class.getSimpleName());

	private static BrokerConfiguration config;
	private static DatabaseInitializer dbInitializer;
	private static String dbName = "mlmqunittest" + System.currentTimeMillis();

	private Broker broker;

	@BeforeClass
	public static void beforeClass() throws IOException, SQLException {
		LoggerUtil.initConsoleDebug();

		Properties props = BrokerConfiguration.loadProperties("unittestconfig.example.properties");
		props.put(BrokerConfiguration.DB_NAME, dbName);
		config = new BrokerConfiguration(props);

		dbInitializer = new DatabaseInitializer(config.getDbUrl(), config.getDbUserName(), config.getDbPassword(), config.getDbName());

		dbInitializer.connect();
		dbInitializer.createDatabase();
		dbInitializer.createTables();
	}

	@AfterClass
	public static void afterClass() throws SQLException {
		dbInitializer.deleteDatabase();
	}

	@Before
	public void before() {
		broker = new Broker(config);

	}

	@After
	public void after() {
		broker.shutdown();
	}

	@Test
	public void testSimple() {
		logger.info("Start SimpleTest");

		broker.startup();

	}
}
