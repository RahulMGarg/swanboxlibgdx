package com.swandev.swangame.socket;

import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;

import java.net.MalformedURLException;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

import org.json.JSONArray;
import org.json.JSONObject;

import com.badlogic.gdx.Gdx;
import com.google.common.collect.Lists;
import com.swandev.swangame.util.CommonLogTags;
import com.swandev.swangame.util.SwanUtil;

public class SocketIOState {

	@Getter
	SocketIO client;
	
	@Getter
	String serverAddress;

	@Getter
	final EventEmitter eventEmitter;

	@Getter
	@Setter
	private String nickname;

	@Getter
	private List<String> nicknames = Lists.newArrayList();

	@Getter
	@Setter
	private boolean playerListReady;

	public SocketIOState() {
		this.eventEmitter = new EventEmitter();
		on(CommonSocketIOEvents.GET_NICKNAMES, new EventCallback() {

			@Override
			public void onEvent(IOAcknowledge ack, Object... args) {
				nicknames = SwanUtil.parseJsonList((JSONArray) args[0]);
				playerListReady = true;
			}
		});
	}

	public static final String SCREEN_NAME = "Screen";

	public void on(String eventName, EventCallback callback) {
		eventEmitter.on(eventName, callback);
	}

	@Getter
	@Setter
	private boolean host = false;

	public boolean isConnected() {
		return getClient() != null && getClient().isConnected();
	}

	public void swanEmit(String event, String addressee, Object... args) {
		client.emit(CommonSocketIOEvents.SWAN_EMIT, addressee, event, args);
	}

	public void emitToScreen(String event, Object... args) {
		swanEmit(event, SocketIOState.SCREEN_NAME, args);
	}
	
	public void emitToServer(String event, Object... args){
		client.emit(event, args);
	}

	public void swanBroadcast(String event, Object... args) {
		client.emit(CommonSocketIOEvents.SWAN_BROADCAST, event, args);
	}

	/** Updates the nicknames */
	public void requestNicknames() {
		client.emit(CommonSocketIOEvents.GET_NICKNAMES);
	}

	/** Call this at the end of the render loop */
	public void flushEvents() {
		this.eventEmitter.flushEvents();
	}
	
	public void connect(final String serverAddress, final String nickname, final boolean isScreen, final ConnectCallback connectCallback) throws MalformedURLException {
		this.serverAddress = serverAddress;
		final SocketIO socketIO = new SocketIO(serverAddress);
		this.client = socketIO;
		// TODO: make this modular...
		client.connect(new IOCallback() {

			@Override
			public void onMessage(JSONObject arg0, IOAcknowledge arg1) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onMessage(String arg0, IOAcknowledge arg1) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onError(SocketIOException ex) {
				Gdx.app.error(CommonLogTags.SOCKET_IO, "Connection error", ex);
				connectCallback.onConnect(ex);
			}

			@Override
			public void onDisconnect() {
				Gdx.app.debug(CommonLogTags.SOCKET_IO, "Disconnected");
				eventEmitter.clear();
			}

			@Override
			public void onConnect() {
				Gdx.app.debug(CommonLogTags.SOCKET_IO, "Connected to " + serverAddress);
				if (!isScreen) {
					client.emit(CommonSocketIOEvents.NICKNAME_SET, nickname);
					setNickname(nickname);
				} else {
					client.emit(CommonSocketIOEvents.SCREEN_SET);
				}
				connectCallback.onConnect(null);
			}

			@Override
			public void on(String event, IOAcknowledge ack, Object... arguments) {
				eventEmitter.recordEvent(event, ack, arguments);
			}
		});
	}
}
