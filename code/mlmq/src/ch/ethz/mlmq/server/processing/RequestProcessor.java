package ch.ethz.mlmq.server.processing;

import java.sql.SQLException;
import java.util.logging.Logger;

import ch.ethz.mlmq.dto.ClientDto;
import ch.ethz.mlmq.dto.MessageDto;
import ch.ethz.mlmq.dto.QueueDto;
import ch.ethz.mlmq.exception.MlmqException;
import ch.ethz.mlmq.logging.PerformanceLogger;
import ch.ethz.mlmq.logging.PerformanceLoggerManager;
import ch.ethz.mlmq.net.request.CreateQueueRequest;
import ch.ethz.mlmq.net.request.DeleteQueueRequest;
import ch.ethz.mlmq.net.request.DequeueMessageRequest;
import ch.ethz.mlmq.net.request.PeekMessageRequest;
import ch.ethz.mlmq.net.request.QueuesWithPendingMessagesRequest;
import ch.ethz.mlmq.net.request.RegistrationRequest;
import ch.ethz.mlmq.net.request.Request;
import ch.ethz.mlmq.net.request.SendClientMessageRequest;
import ch.ethz.mlmq.net.request.SendMessageRequest;
import ch.ethz.mlmq.net.response.CreateQueueResponse;
import ch.ethz.mlmq.net.response.DeleteQueueResponse;
import ch.ethz.mlmq.net.response.MessageResponse;
import ch.ethz.mlmq.net.response.RegistrationResponse;
import ch.ethz.mlmq.net.response.Response;
import ch.ethz.mlmq.net.response.SendClientMessageResponse;
import ch.ethz.mlmq.net.response.SendMessageResponse;
import ch.ethz.mlmq.server.ClientApplicationContext;
import ch.ethz.mlmq.server.db.DbConnection;
import ch.ethz.mlmq.server.db.DbConnectionPool;
import ch.ethz.mlmq.server.db.dao.ClientDao;
import ch.ethz.mlmq.server.db.dao.MessageDao;
import ch.ethz.mlmq.server.db.dao.QueueDao;

public class RequestProcessor {

	private final Logger logger = Logger.getLogger(RequestProcessor.class.getSimpleName());

	private final PerformanceLogger perfLog = PerformanceLoggerManager.getLogger();

	public RequestProcessor() {
	}

	public Response process(ClientApplicationContext clientApplicationContext, Request request, DbConnectionPool pool) throws MlmqException {
		long startTime = System.currentTimeMillis();

		if (!clientApplicationContext.isRegistered() && !(request instanceof RegistrationRequest)) {
			throw new MlmqException("Client not yet registere");
		}

		logger.info("Process Request " + request);
		try {

			if (request instanceof CreateQueueRequest) {
				return processCreateQueueRequest((CreateQueueRequest) request, pool);

			} else if (request instanceof QueuesWithPendingMessagesRequest) {
				return processQueuesWithPendingMessagesRequest((QueuesWithPendingMessagesRequest) request, clientApplicationContext, pool);

			} else if (request instanceof RegistrationRequest) {
				return processRegistrationRequest((RegistrationRequest) request, clientApplicationContext, pool);

			} else if (request instanceof DeleteQueueRequest) {
				return processDeleteQueueRequest((DeleteQueueRequest) request, pool);

			} else if (request instanceof DequeueMessageRequest) {
				return processDequeueMessageRequest((DequeueMessageRequest) request, clientApplicationContext, pool);

			} else if (request instanceof PeekMessageRequest) {
				return processPeekMessageRequest((PeekMessageRequest) request, clientApplicationContext, pool);

			} else if (request instanceof SendMessageRequest) {
				return processSendMessageRequest((SendMessageRequest) request, clientApplicationContext, pool);

			} else if (request instanceof SendClientMessageRequest) {
				return processSendClientMessageRequest((SendClientMessageRequest) request, clientApplicationContext, pool);

			} else {
				throw new MlmqException("Unexpected Request to process " + request.getClass().getSimpleName() + " - " + request);
			}

		} finally {

			perfLog.log(System.currentTimeMillis() - startTime, "Request Processes - " + request.getClass().getSimpleName());
		}
	}

	private Response processSendClientMessageRequest(SendClientMessageRequest request, ClientApplicationContext clientApplicationContext, DbConnectionPool pool)
			throws MlmqException {

		DbConnection connection = null;
		try {
			connection = pool.getConnection();

			QueueDao queueDao = connection.getQueueDao();

			long receivingClientQueueId = queueDao.getQueueByClientId(request.getClientId());

			MessageDao messageDao = connection.getMessageDao();

			Long context = request.getConversationContext();
			if (request.isConversation() && context == null) {
				context = messageDao.generateNewConversationContext();
			}
			messageDao.insertMessage(receivingClientQueueId, clientApplicationContext.getClient().getId(), request.getContent(), request.getPrio(), context);
			SendClientMessageResponse response = new SendClientMessageResponse();
			response.setConversationContext(context);
			return response;

		} catch (SQLException ex) {
			connection.close();
			throw new MlmqException(ex);
		} finally {
			if (connection != null) {
				pool.returnConnection(connection);
			}
		}

	}

	private Response processQueuesWithPendingMessagesRequest(QueuesWithPendingMessagesRequest request, ClientApplicationContext clientApplicationContext,
			DbConnectionPool pool) throws MlmqException {

		DbConnection connection = null;
		try {
			connection = pool.getConnection();

			throw new SQLException("TODO");

		} catch (SQLException ex) {
			connection.close();
			throw new MlmqException(ex);
		} finally {
			if (connection != null) {
				pool.returnConnection(connection);
			}
		}

	}

	private Response processPeekMessageRequest(PeekMessageRequest request, ClientApplicationContext clientApplicationContext, DbConnectionPool pool)
			throws MlmqException {

		DbConnection connection = null;
		try {
			connection = pool.getConnection();
			MessageDao messageDao = connection.getMessageDao();

			MessageDto message = messageDao.peekMessage(request.getMessageQueryInfo());
			return new MessageResponse(message);
		} catch (SQLException ex) {
			connection.close();
			throw new MlmqException(ex);
		} finally {
			if (connection != null) {
				pool.returnConnection(connection);
			}
		}
	}

	private Response processDequeueMessageRequest(DequeueMessageRequest request, ClientApplicationContext clientApplicationContext, DbConnectionPool pool)
			throws MlmqException {

		DbConnection connection = null;
		try {
			connection = pool.getConnection();
			MessageDao messageDao = connection.getMessageDao();

			MessageDto message = messageDao.dequeueMessage(request.getMessageQueryInfo());
			return new MessageResponse(message);
		} catch (SQLException ex) {
			connection.close();
			throw new MlmqException(ex);
		} finally {
			if (connection != null) {
				pool.returnConnection(connection);
			}
		}
	}

	private Response processSendMessageRequest(SendMessageRequest request, ClientApplicationContext clientApplicationContext, DbConnectionPool pool)
			throws MlmqException {
		DbConnection connection = null;
		try {
			connection = pool.getConnection();

			MessageDao messageDao = connection.getMessageDao();
			messageDao.insertMessage(request, clientApplicationContext);
			SendMessageResponse response = new SendMessageResponse();
			return response;
		} catch (SQLException ex) {
			connection.close();
			throw new MlmqException(ex);
		} finally {
			if (connection != null) {
				pool.returnConnection(connection);
			}
		}
	}

	private Response processDeleteQueueRequest(DeleteQueueRequest request, DbConnectionPool pool) throws MlmqException {

		DbConnection connection = null;
		try {
			connection = pool.getConnection();

			QueueDao queueDao = connection.getQueueDao();

			long queueIdToDelete = request.getQueueId();
			queueDao.deleteQueue(queueIdToDelete);

			return new DeleteQueueResponse();

		} catch (SQLException ex) {
			connection.close();
			throw new MlmqException(ex);
		} finally {
			if (connection != null) {
				pool.returnConnection(connection);
			}
		}

	}

	private Response processCreateQueueRequest(CreateQueueRequest request, DbConnectionPool pool) throws MlmqException {
		DbConnection connection = null;
		try {
			connection = pool.getConnection();

			QueueDao queueDao = connection.getQueueDao();
			QueueDto queue = queueDao.createQueue();

			CreateQueueResponse response = new CreateQueueResponse(queue);
			return response;

		} catch (SQLException ex) {
			connection.close();
			throw new MlmqException(ex);
		} finally {
			if (connection != null) {
				pool.returnConnection(connection);
			}
		}
	}

	private Response processRegistrationRequest(RegistrationRequest request, ClientApplicationContext clientApplicationContext, DbConnectionPool pool)
			throws MlmqException {

		DbConnection connection = null;
		try {
			connection = pool.getConnection();

			// insert new Client
			ClientDao clientDao = connection.getClientDao();
			QueueDao queueDao = connection.getQueueDao();

			String name = request.getClientName();

			Integer clientId = clientDao.getClientId(name);
			QueueDto clientQueue;
			if (clientId == null) {
				int newClientId = clientDao.insertNewClient(name);
				clientId = newClientId;

				// insert new ClientQueue
				clientQueue = queueDao.createClientQueue(newClientId);
			} else {

				long queueId = queueDao.getQueueByClientId(clientId);
				clientQueue = new QueueDto(queueId);

				logger.info("Welcome back " + name + " ClientId [" + clientId + "] ClientQueue [" + queueId + "]");
			}

			ClientDto clientDto = new ClientDto(clientId);
			clientDto.setName(request.getClientName());

			clientApplicationContext.setClient(clientDto);
			clientApplicationContext.setClientQueue(clientQueue);

			RegistrationResponse response = new RegistrationResponse(clientDto);

			return response;

		} catch (SQLException ex) {
			connection.close();
			throw new MlmqException(ex);
		} finally {
			if (connection != null) {
				pool.returnConnection(connection);
			}
		}
	}
}
