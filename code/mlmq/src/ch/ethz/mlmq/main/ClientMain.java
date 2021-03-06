package ch.ethz.mlmq.main;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

import ch.ethz.mlmq.client.ClientConfiguration;
import ch.ethz.mlmq.client.ClientImpl;
import ch.ethz.mlmq.client.testscenario.TestScenarioManager;
import ch.ethz.mlmq.logging.LoggerUtil;
import ch.ethz.mlmq.logging.PerformanceLoggerManager;
import ch.ethz.mlmq.server.BrokerCommandFileHandler;
import ch.ethz.mlmq.server.CommandListener;
import ch.ethz.mlmq.util.ConfigurationUtil;

public class ClientMain implements CommandListener {

	private static final Logger logger = Logger.getLogger(ClientMain.class.getSimpleName());

	private ClientConfiguration config;

	private ClientImpl clientInterface;
	private BrokerCommandFileHandler commandFileHandler;
	private TestScenarioManager testScenarioMgr;

	public void run(String clientConfigurationFile) {
		try {
			Properties props = ConfigurationUtil.loadPropertiesFromFile(clientConfigurationFile);
			config = new ClientConfiguration(props);

			logger.info("Configuring Performance Logger");
			PerformanceLoggerManager.configureLogger(config.getPerformanceLoggerConfig());

			logger.info("CommandFileHandler...");
			commandFileHandler = new BrokerCommandFileHandler(config.getCommandoFilePath(), config.getCommandFileCheckIntervall(), this);
			commandFileHandler.start();
			logger.info("CommandFileHandler started");

			logger.info("Starting Client " + config.getName());
			clientInterface = new ClientImpl(config);
			clientInterface.init();
			logger.info("Client started");

			testScenarioMgr = new TestScenarioManager(clientInterface);
			testScenarioMgr.startTest(config);

		} catch (Exception e) {
			logger.severe("Exception " + LoggerUtil.getStackTraceString(e));
		}
	}

	@Override
	public void onCommand(String command) {
		logger.info("BrokerCommandFile - onCommand [" + command + "]");

		command = command.toLowerCase();
		if (command.contains(BrokerCommandFileHandler.COMMAND_SHUTDOWN)) {
			doShutdown();
			return;
		} else if (command.contains(BrokerCommandFileHandler.COMMAND_LOG_STACKTRACE)) {
			LoggerUtil.logStackTrace(logger);
			return;
		} else {
			logger.info("BrokerCommand unexpected command [" + command + "]");
		}
	}

	private void doShutdown() {
		logger.info("Doing Client " + config.getName() + " shutdown...");

		commandFileHandler.stop();

		try {
			clientInterface.close();
		} catch (IOException e) {
			logger.severe("Error while shutting down " + config.getName() + " " + LoggerUtil.getStackTraceString(e));
		}

		PerformanceLoggerManager.shutDown();
	}
}
