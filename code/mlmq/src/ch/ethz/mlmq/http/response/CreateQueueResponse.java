package ch.ethz.mlmq.http.response;

import ch.ethz.mlmq.dto.QueueDto;

public class CreateQueueResponse implements Response {

	private QueueDto queue;

	public QueueDto getQueueDto() {
		// TODO Auto-generated method stub
		return queue;
	}

}
