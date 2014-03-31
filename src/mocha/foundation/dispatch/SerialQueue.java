/**
 *  @author Shaun
 *  @date 2/18/13
 *  @copyright 2013 Mocha. All rights reserved.
 */
package mocha.foundation.dispatch;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public final class SerialQueue extends ExecutorServiceQueue {
	private static Map<Priority,SerialQueue> globalQueues = new HashMap<Priority, SerialQueue>();

	/**
	 * Get a global queue based on the priority you request
	 * Queue's with higher priorities will have their tasks
	 * executed before jobs with lower priorities.
	 *
	 * @param priority queue priority
	 * @return Global queue for requested priority
	 */
	public static synchronized SerialQueue getGlobalQueue(Priority priority) {
		if(priority == null) priority = Priority.DEFAULT;
		SerialQueue globalQueue = globalQueues.get(priority);

		if(globalQueue == null) {
			globalQueue = new SerialQueue("mocha.foundation.global." + priority);
			globalQueue.global = true;
			globalQueue.priority = priority;
			globalQueues.put(priority, globalQueue);
		}

		return globalQueue;
	}

	/**
	 * Create a new queue
	 *
	 * @param label label for the queue, may be null
	 */
	public SerialQueue(String label) {
		this(label, Priority.DEFAULT);
	}

	/**
	 * Create a new queue
	 *
	 * @param label label for the queue, may be null
	 * @param priority Priority for the queue
	 */
	public SerialQueue(String label, Priority priority) {
		this.label = label;
		this.priority = priority == null ? Priority.DEFAULT : priority;
	}

	/**
	 * Create a new queue
	 *
	 * @param label label for the queue, may be null
	 * @param targetQueue target queue for this queue
	 */
	public SerialQueue(String label, SerialQueue targetQueue) {
		this(label);
		this.setTargetQueue(targetQueue);
	}

	/**
	 * @inheritDoc
	 */
	public void setTargetQueue(SerialQueue queue) {
		super.setTargetQueue(queue);
	}

	synchronized ExecutorService getExecutorService() {
		if(this.executorService == null) {
			this.executorService = QueueExecutors.serialQueue(this.priority, this.label, 60, TimeUnit.SECONDS);
		}

		return this.executorService;
	}

}
