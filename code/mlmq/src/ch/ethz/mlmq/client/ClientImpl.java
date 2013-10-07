package ch.ethz.mlmq.client;

import java.io.IOException;
import java.util.List;

import ch.ethz.mlmq.dto.BrokerDto;
import ch.ethz.mlmq.dto.ClientDto;
import ch.ethz.mlmq.dto.MessageDto;
import ch.ethz.mlmq.dto.MessageQueryInfoDto;
import ch.ethz.mlmq.dto.QueueDto;
import ch.ethz.mlmq.exception.MlmqException;
import ch.ethz.mlmq.net.ClientConnection;
import ch.ethz.mlmq.net.ConnectionPool;
import ch.ethz.mlmq.net.request.CreateQueueRequest;
import ch.ethz.mlmq.net.request.DeleteQueueRequest;
import ch.ethz.mlmq.net.request.DequeueMessageRequest;
import ch.ethz.mlmq.net.request.PeekMessageRequest;
import ch.ethz.mlmq.net.request.QueueRequest;
import ch.ethz.mlmq.net.request.QueuesWithPendingMessagesRequest;
import ch.ethz.mlmq.net.request.RegistrationRequest;
import ch.ethz.mlmq.net.request.Request;
import ch.ethz.mlmq.net.request.SendMessageRequest;
import ch.ethz.mlmq.net.response.CreateQueueResponse;
import ch.ethz.mlmq.net.response.ExceptionResponse;
import ch.ethz.mlmq.net.response.MessageResponse;
import ch.ethz.mlmq.net.response.QueuesWithPendingMessagesResponse;
import ch.ethz.mlmq.net.response.RegistrationResponse;
import ch.ethz.mlmq.net.response.Response;

public class ClientImpl implements Client {

	private ClientDto registeredAs;
	private final ConnectionPool brokerConnections;
	private BrokerDto defaultBroker;

	private final String name;

	public ClientImpl(String name, BrokerDto defaultBroker, long responseTimeoutTime) throws IOException {
		this.name = name;
		this.brokerConnections = new ConnectionPool(responseTimeoutTime);
		this.defaultBroker = defaultBroker;
	}

	@Override
	public void close() throws IOException {
		brokerConnections.close();
	}

	private Response sendRequest(QueueRequest request) throws IOException {
		return sendRequestToBroker(request, defaultBroker);
	}

	private Response sendRequest(Request request) throws IOException {
		return sendRequestToBroker(request, defaultBroker);
	}

	/**
	 * Sends a request to a specific broker.
	 * 
	 * @param request
	 * @param broker
	 * @param timeout
	 * @return
	 * @throws IOException
	 * @throws MlmqException
	 */
	private Response sendRequestToBroker(Request request, BrokerDto broker) throws IOException {
		ClientConnection c = brokerConnections.getConnection(broker);
		Response response = c.submitRequest(request);

		if (response instanceof ExceptionResponse) {
			ExceptionResponse r = (ExceptionResponse) response;
			Exception e = r.getException();
			if (e != null) {
				throw new IOException("Go ExceptionResponse from Server " + e.getMessage(), e);
			}
		}

		return response;
	}

	@Override
	public ClientDto register() throws IOException {
		RegistrationResponse repsonse = (RegistrationResponse) sendRequest(new RegistrationRequest(name));
		registeredAs = repsonse.getClientDto();
		return registeredAs;
	}

	@Override
	public QueueDto createQueue() throws IOException {
		CreateQueueResponse repsonse = (CreateQueueResponse) sendRequest(new CreateQueueRequest());
		return repsonse.getQueueDto();
	}

	@Override
	public void deleteQueue(long id) throws IOException {
		sendRequest(new DeleteQueueRequest(id));
	}

	@Override
	public void sendMessage(long queueId, byte[] content, int prio) throws IOException {
		sendRequest(new SendMessageRequest(queueId, content, prio));
	}

	@Override
	public void sendMessage(long[] queueIds, byte[] content, int prio) throws IOException {
		for (long q : queueIds) {
			sendMessage(q, content, prio);
		}
	}

	@Override
	public MessageDto peekMessage(MessageQueryInfoDto messageQueryInfo) throws IOException {
		MessageResponse response = (MessageResponse) sendRequest(new PeekMessageRequest(messageQueryInfo));
		return response.getMessageDto();
	}

	@Override
	public MessageDto dequeueMessage(MessageQueryInfoDto messageQueryInfo) throws IOException {
		MessageResponse response = (MessageResponse) sendRequest(new DequeueMessageRequest(messageQueryInfo));
		return response.getMessageDto();
	}

	@Override
	public List<QueueDto> queuesWithPendingMessages() throws IOException {
		QueuesWithPendingMessagesResponse response = (QueuesWithPendingMessagesResponse) sendRequest(new QueuesWithPendingMessagesRequest());
		return response.getQueues();
	}

}
