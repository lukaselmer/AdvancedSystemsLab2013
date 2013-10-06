package ch.ethz.mlmq.dto;

import java.io.Serializable;

/**
 * Data Transfer Object
 * 
 * used to filter incoming messages.
 */
public class MessageQueryInfoDto implements Serializable {
	private static final long serialVersionUID = 1658560065690073547L;

	private QueueDto queue;
	private ClientDto sender;
	private boolean shouldOrderByPriority;

	public MessageQueryInfoDto(QueueDto queueFilter, ClientDto sender, boolean shouldOrderByPriority) {
		this.queue = queueFilter;
		this.sender = sender;
		this.shouldOrderByPriority = shouldOrderByPriority;
	}

	/**
	 * @return the queue
	 */
	public QueueDto getQueue() {
		return queue;
	}

	public ClientDto getSender() {
		return sender;
	}

	public boolean shouldOrderByPriority() {
		return shouldOrderByPriority;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((queue == null) ? 0 : queue.hashCode());
		result = prime * result + ((sender == null) ? 0 : sender.hashCode());
		result = prime * result + (shouldOrderByPriority ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MessageQueryInfoDto other = (MessageQueryInfoDto) obj;
		if (queue == null) {
			if (other.queue != null)
				return false;
		} else if (!queue.equals(other.queue))
			return false;
		if (sender == null) {
			if (other.sender != null)
				return false;
		} else if (!sender.equals(other.sender))
			return false;
		if (shouldOrderByPriority != other.shouldOrderByPriority)
			return false;
		return true;
	}

}
