package cn.ncw.music.stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class StreamAudioPlayerTest {

    private StreamAudioPlayer player;

    @BeforeEach
    void setUp() {
        player = new StreamAudioPlayer();
    }

    @AfterEach
    void tearDown() {
        if (player != null) {
            player.stop();
        }
    }

    @Test
    void testInitialState() {
        assertEquals(StreamAudioPlayer.STATE_STOPPED, player.getPlaybackState());
        assertFalse(player.isVolumeSupported());
        assertTrue(player.isPositionSupported());
        assertEquals(0, player.getCurrentFrame());
        assertEquals(0, player.getTotalFrames());
        assertEquals(0.8, player.getVolume());
    }

    @Test
    void testVolumeControl() {
        // 测试音量设置
        assertTrue(player.setVolume(0.5));
        assertEquals(0.5, player.getVolume());

        // 测试音量范围限制
        assertTrue(player.setVolume(1.5)); // 超过上限，应该被限制
        assertEquals(1.0, player.getVolume());

        assertTrue(player.setVolume(-0.5)); // 低于下限，应该被限制
        assertEquals(0.0, player.getVolume());

        // 测试静音
        assertTrue(player.setMute());
        assertEquals(0.0, player.getVolume());

        // 测试音量增减
        assertTrue(player.increaseVolume(0.3));
        assertEquals(0.3, player.getVolume());

        assertTrue(player.decreaseVolume(0.1));
        assertEquals(0.2, player.getVolume());
    }

    @Test
    void testPositionControl() {
        // 测试初始位置信息
        assertEquals("位置控制不支持", player.getPositionInfo());
        assertEquals(0.0, player.getCurrentTime());
        assertEquals(0.0, player.getTotalTime());
        assertEquals(0.0, player.getPlaybackProgress());
    }

    @Test
    void testAudioFormat() {
        // 测试初始状态下的音频格式
        assertNull(player.getAudioFormat());
    }

    @Test
    void testSeekMethods() {
        // 测试跳转方法在未播放时的行为
        assertFalse(player.seekToTime(10.0));
        assertFalse(player.seekToFrame(100));
        assertFalse(player.seekForward(5.0));
        assertFalse(player.seekBackward(5.0));
    }

    // 注意：以下测试需要实际的音频文件，可能需要根据实际情况调整
    @Test
    void testPlayback() throws UnsupportedAudioFileException, LineUnavailableException, IOException, InterruptedException {
        // 检查是否存在测试音频文件
        java.io.File testFile = new java.io.File("music1.wav");
        if (!testFile.exists()) {
            System.out.println("测试音频文件不存在，跳过播放测试");
            return;
        }

        // 测试播放
        player.play("music1.wav");
        assertEquals(StreamAudioPlayer.STATE_PLAYING, player.getPlaybackState());
        assertNotNull(player.getAudioFormat());

        // 测试暂停
        player.pause();
        assertEquals(StreamAudioPlayer.STATE_PAUSED, player.getPlaybackState());

        // 测试恢复
        player.resume();
        assertEquals(StreamAudioPlayer.STATE_PLAYING, player.getPlaybackState());

        // 测试停止
        player.stop();
        assertEquals(StreamAudioPlayer.STATE_STOPPED, player.getPlaybackState());
    }
}
