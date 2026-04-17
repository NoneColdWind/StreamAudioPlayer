package cn.ncw.music.stream.event;

import cn.ncw.logger.log.NCWLoggerFactory;
import cn.ncw.music.stream.enums.PlayMode;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * 播放事件发布器
 */
public class PlaybackEventPublisher {
    private final List<PlaybackEventListener> listeners = new CopyOnWriteArrayList<>();
    private final ExecutorService executor;
    private final NCWLoggerFactory logger;

    public PlaybackEventPublisher(ExecutorService executor, NCWLoggerFactory logger) {
        this.executor = executor;
        this.logger = logger;
    }

    public void addListener(PlaybackEventListener listener) {
        listeners.add(listener);
    }

    public void removeListener(PlaybackEventListener listener) {
        listeners.remove(listener);
    }

    public void removeAllListeners() {
        listeners.clear();
    }

    public void firePlaybackStarted(File file) {
        fireEvent(listener -> listener.onPlaybackStarted(file), "onPlaybackStarted");
    }

    public void firePlaybackPaused() {
        fireEvent(PlaybackEventListener::onPlaybackPaused, "onPlaybackPaused");
    }

    public void firePlaybackResumed() {
        fireEvent(PlaybackEventListener::onPlaybackResumed, "onPlaybackResumed");
    }

    public void firePlaybackStopped() {
        fireEvent(PlaybackEventListener::onPlaybackStopped, "onPlaybackStopped");
    }

    public void firePlaybackFinished() {
        fireEvent(PlaybackEventListener::onPlaybackFinished, "onPlaybackFinished");
    }

    public void fireTrackChanged(File previous, File next) {
        fireEvent(listener -> listener.onTrackChanged(previous, next), "onTrackChanged");
    }

    public void fireError(Exception e) {
        fireEvent(listener -> listener.onError(e), "onError");
    }

    public void firePositionChanged(double position) {
        fireEvent(listener -> listener.onPositionChanged(position), "onPositionChanged");
    }

    public void fireVolumeChanged(double volume) {
        fireEvent(listener -> listener.onVolumeChanged(volume), "onVolumeChanged");
    }

    public void firePlayModeChanged(PlayMode newMode) {
        fireEvent(listener -> listener.onPlayModeChanged(newMode), "onPlayModeChanged");
    }

    public void firePlaylistUpdated() {
        fireEvent(PlaybackEventListener::onPlaylistUpdated, "onPlaylistUpdated");
    }

    private void fireEvent(Consumer<PlaybackEventListener> action, String eventName) {
        executor.submit(() -> {
            for (PlaybackEventListener listener : listeners) {
                try {
                    action.accept(listener);
                } catch (Exception e) {
                    logger.error("Error in listener during " + eventName + " event", "fireEvent", e);
                }
            }
        });
    }
}
