package cn.ncw.music.stream.event;

import cn.ncw.music.stream.enums.PlayMode;

import java.io.File;

/**
 * 播放事件监听器接口
 */
public interface PlaybackEventListener {
    void onPlaybackStarted(File file);
    void onPlaybackPaused();
    void onPlaybackResumed();
    void onPlaybackStopped();
    void onPlaybackFinished();
    void onTrackChanged(File previous, File next);
    void onError(Exception e);
    void onPositionChanged(double position);
    void onVolumeChanged(double volume);
    void onPlayModeChanged(PlayMode newMode);
    void onPlaylistUpdated();
}
