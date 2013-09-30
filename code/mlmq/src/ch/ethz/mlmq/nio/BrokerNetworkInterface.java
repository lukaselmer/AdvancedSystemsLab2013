package ch.ethz.mlmq.nio;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

import ch.ethz.mlmq.logging.LoggerUtil;
import ch.ethz.mlmq.server.BrokerConfiguration;
import ch.ethz.mlmq.server.processing.ResponseQueue;

public class BrokerNetworkInterface implements Runnable, Closeable {

	private static final Logger logger = Logger.getLogger("BrokerNetworkInterface");

	private Selector selector;

	/**
	 * ServerSocket which accepts clientconnection
	 */
	private ServerSocketChannel serverSocketChannel;

	/**
	 * TCP port where we are listening
	 */
	private final int listenPort;

	/**
	 * indicates whether we are running or not
	 */
	private volatile boolean running = false;

	/**
	 * Queue where workers post their responses, which then are transfered by us
	 */
	private final NetworkIntefaceResponseQueue responseQueue;

	/**
	 * Thread which handles networking on the broker
	 */
	private Thread networkingThread;

	public BrokerNetworkInterface(BrokerConfiguration config) {
		this.listenPort = config.getListenPort();
		this.responseQueue = new NetworkIntefaceResponseQueue();
	}

	public void init() {

		if (networkingThread != null) {
			throw new RuntimeException("BrokerNetworkInterface already running");
		}

		logger.info("Initializing NetworkInterface on Port " + listenPort);

		networkingThread = new Thread(this);
		networkingThread.setName("NetworkInterface");
		networkingThread.start();
	}

	@Override
	public void close() {
		running = false;

		logger.info("Wakeup selector");
		selector.wakeup();
	}

	@Override
	public void run() {
		running = true;
		try {
			setup();
			while (running) {

				selector.select();

				Set<SelectionKey> selectedKeys = selector.selectedKeys();
				Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
				while (keyIterator.hasNext()) {
					try {
						SelectionKey key = keyIterator.next();
						selectKey(key);
					} catch (Exception ex) {
						logger.info("Exception " + ex);
					} finally {
						keyIterator.remove();
					}
				}
			}
		} catch (IOException ex) {
			logger.severe("IOException in NetworkInterface " + LoggerUtil.getStackTraceString(ex));
		} finally {
			running = false;
			networkingThread = null;

			logger.info("NetworkInterface closing...");
			try {
				teardown();
			} catch (IOException ex) {
				logger.warning("Exception while shutdown " + LoggerUtil.getStackTraceString(ex));
			}
			logger.info("NetworkInterface closed");
		}
	}

	private void selectKey(SelectionKey key) {

		if (key.isValid() && key.isAcceptable()) {
			try {
				selectAccept(key);
			} catch (Exception ex) {
				logger.severe("Exception while accepting " + LoggerUtil.getStackTraceString(ex));
			}
		}
		if (key.isValid() && key.isReadable()) {
			try {
				selectRead(key);
			} catch (Exception ex) {
				logger.severe("Exception while reading " + LoggerUtil.getStackTraceString(ex));
			}
		}
		if (key.isValid() && key.isWritable()) {
			try {
				selectWrite(key);
			} catch (Exception ex) {
				logger.severe("Exception while writing " + LoggerUtil.getStackTraceString(ex));
			}
		}

	}

	private void selectWrite(SelectionKey key) {

		ConnectedClient clientInstance = (ConnectedClient) key.attachment();
		logger.finer("Write " + clientInstance);

		boolean isWriting = clientInstance.isProtocolStateWriting();
		// TODO Auto-generated method stub

		if (isWriting) {
			key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
		} else {
			key.interestOps(SelectionKey.OP_READ);
		}
	}

	private void selectRead(SelectionKey key) throws IOException {

		ConnectedClient clientInstance = (ConnectedClient) key.attachment();

		SocketChannel clientChannel = (SocketChannel) key.channel();

		ByteBuffer rxBuffer = clientInstance.getRxBuffer();
		int byteCount = clientChannel.read(rxBuffer);
		logger.info("reading num bytes " + byteCount);

	}

	private void selectAccept(SelectionKey key) throws IOException, ClosedChannelException {
		SocketChannel newClientChannel = serverSocketChannel.accept();
		newClientChannel.configureBlocking(false);

		logger.info(newClientChannel + " new connection established");

		ConnectedClient clientInstance = new ConnectedClient(newClientChannel.getRemoteAddress().toString());
		newClientChannel.register(selector, SelectionKey.OP_READ, clientInstance);
	}

	private void teardown() throws IOException {

		logger.info("Close ServerSocket");
		if (serverSocketChannel.isOpen()) {
			serverSocketChannel.close();
		}

		logger.info("Close Selector");
		if (selector.isOpen()) {
			selector.close();
		}
	}

	private void setup() throws IOException {
		logger.info("Open Selector");
		selector = Selector.open();

		logger.info("Open ServerSocket");
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.configureBlocking(false);
		serverSocketChannel.bind(new InetSocketAddress(listenPort));
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

		logger.info("ServerSocketChannel bound to " + listenPort);

	}

	/**
	 * returns the queue where workers put their response
	 * 
	 * @return
	 */
	public ResponseQueue getResponseQueue() {
		return responseQueue;
	}
}