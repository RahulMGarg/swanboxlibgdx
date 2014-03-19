package com.swandev.jukebox;

import io.socket.IOAcknowledge;

import java.util.List;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.swandev.jukebox.Jukebox.SongData;
import com.swandev.jukebox.Jukebox.SongRequest;
import com.swandev.swanlib.screen.SwanScreen;
import com.swandev.swanlib.socket.EventCallback;
import com.swandev.swanlib.socket.EventEmitter;
import com.swandev.swanlib.socket.SocketIOState;

public class JukeboxServerScreen extends SwanScreen {

	private final Jukebox jukebox;
	private final Stage stage;
	private final Table playListTable;
	private final JukeboxServer game;
	private final Label timeElapsed;

	public JukeboxServerScreen(SocketIOState socketIO, JukeboxServer game) {
		super(socketIO);
		this.game = game;
		jukebox = new Jukebox(this);
		stage = new Stage();
		timeElapsed = new Label("", game.getAssets().getSkin());
		playListTable = new Table();
		stage.addActor(playListTable);
	}

	public void uiUpdatePlayList() {
		final Skin skin = game.getAssets().getSkin();
		playListTable.clear();
		final List<SongRequest> requests = jukebox.getPlayList();
		playListTable.add(new Label("Time elapsed:", skin));
		playListTable.add(timeElapsed);
		playListTable.row();
		playListTable.add(new Label("Song:", skin));
		playListTable.add(new Label("Requester:", skin));
		playListTable.row();
		for (final SongRequest request : requests) {
			final Label song = new Label(request.getSongName(), skin);
			final Label requester = new Label(request.getRequester(), skin);
			playListTable.add(song);
			playListTable.add(requester);
			playListTable.row();
		}
		playListTable.setFillParent(true);
	}

	@Override
	public void render(float delta) {
		super.render(delta);
		stage.draw();
		stage.act(delta);
		final SongData songData = jukebox.getCurrentSongData();
		if (songData != null) {
			// TODO: it seems like its returning seconds even though its supposed to return millis?
			timeElapsed.setText(formatTime((long) jukebox.getCurrentSongData().getMusic().getPosition()));
		}
	}

	@Override
	public void show() {
		super.show();
		jukebox.reset();
	}

	@Override
	public void dispose() {
		jukebox.dispose();
		stage.dispose();
	}

	@Override
	public void resize(int width, int height) {
		stage.setViewport(width, height, true);
	}

	@Override
	protected void registerEvents() {
		getSocketIO().on(JukeboxLib.REQUEST_SONGLIST, new EventCallback() {

			@Override
			public void onEvent(IOAcknowledge ack, Object... args) {
				getSocketIO().swanBroadcast(JukeboxLib.SEND_SONGLIST, jukebox.getLibrary());
			}

		});

		getSocketIO().on(JukeboxLib.ADD_TO_PLAYLIST, new EventCallback() {

			@Override
			public void onEvent(IOAcknowledge ack, Object... args) {
				final String sender = (String) args[0];
				final String songName = (String) args[1];
				jukebox.request(sender, songName);
				uiUpdatePlayList();
			}

		});
		getSocketIO().on(JukeboxLib.USER_PLAY, new EventCallback() {

			@Override
			public void onEvent(IOAcknowledge ack, Object... args) {
				jukebox.play();
			}
		});

		getSocketIO().on(JukeboxLib.USER_PAUSE, new EventCallback() {

			@Override
			public void onEvent(IOAcknowledge ack, Object... args) {
				jukebox.pause();
			}

		});
		getSocketIO().on(JukeboxLib.USER_NEXT, new EventCallback() {

			@Override
			public void onEvent(IOAcknowledge ack, Object... args) {
				jukebox.next();
			}

		});

	}

	private String formatTime(long seconds) {
		return String.format("%d s", seconds);
	}

	@Override
	protected void unregisterEvents(EventEmitter eventEmitter) {
		eventEmitter.unregisterEvent(JukeboxLib.ADD_TO_PLAYLIST);
		eventEmitter.unregisterEvent(JukeboxLib.REQUEST_SONGLIST);
		eventEmitter.unregisterEvent(JukeboxLib.USER_NEXT);
		eventEmitter.unregisterEvent(JukeboxLib.USER_PLAY);
		eventEmitter.unregisterEvent(JukeboxLib.USER_PAUSE);
	}

}