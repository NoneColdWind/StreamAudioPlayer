package cn.ncw.music.stream;

import lombok.Getter;
import lombok.Setter;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 多功能流式媒体播放器 - 支持多种音频格式和高级功能
 */
public class AdvancedStreamAudioPlayer {

    // 播放状态常量
    public static final int STATE_PLAYING = 0;
    public static final int STATE_PAUSED = 1;
    public static final int STATE_STOPPED = 2;
    public static final int STATE_BUFFERING = 3;

    // 支持的文件格式
    private static final Set<String> SUPPORTED_FORMATS = Set.of(
            "wav", "mp3", "aiff", "au", "snd", "flac"
    );

    // 成员变量
    private SourceDataLine sourceDataLine;
    private Thread playbackThread;
    private AudioInputStream audioStream;
    private volatile boolean playing;
    private volatile boolean paused;
    /**
     * -- GETTER --
     *  获取当前播放状态
     *
     * @return 播放状态（STATE_PLAYING/STATE_PAUSED/STATE_STOPPED）
     */
    @Getter
    private volatile int playbackState;
    private final Object playLock = new Object();

    // 音量控制相关
    private FloatControl volumeControl;
    /**
     * -- GETTER --
     *  获取当前音量
     *
     * @return 当前音量值，范围0.0-1.0
     */
    @Getter
    private double currentVolume = 0.8;
    private float minVolume;
    private float maxVolume;
    private boolean volumeSupported = false;

    // 位置控制相关
    private File audioFile;
    private AudioFormat originalFormat;
    /**
     * -- GETTER --
     *  获取总帧数
     *
     * @return 总帧数
     */
    @Getter
    private long totalFrames;
    /**
     * -- GETTER --
     *  获取当前帧位置
     *
     * @return 当前帧数
     */
    @Getter
    private long currentFrame;
    private int frameSize;
    /**
     * -- GETTER --
     *  检查是否支持位置控制
     *
     * @return 如果支持位置控制返回true
     */
    @Getter
    private boolean positionSupported = true;

    // 播放列表相关
    private List<File> playlist;
    private AtomicInteger currentTrackIndex;
    // 播放模式设置
    @Setter
    private boolean repeatMode = false;
    private boolean shuffleMode = false;
    private List<File> shuffledPlaylist;

    // 音效控制相关
    private Map<String, FloatControl> soundControls;
    private boolean equalizerSupported = false;

    // 统计信息
    private long totalPlayTime;
    private long startTime;
    private Map<String, Integer> playCount;

    // 事件监听器
    private List<PlaybackEventListener> listeners;

    // 缓冲控制
    private byte[] audioBuffer;
    private int bufferSize = 8192;
    private volatile boolean buffering = false;

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
    }

    public AdvancedStreamAudioPlayer() {
        this.playlist = new ArrayList<>();
        this.currentTrackIndex = new AtomicInteger(0);
        this.playCount = new ConcurrentHashMap<>();
        this.listeners = new ArrayList<>();
        this.soundControls = new HashMap<>();
        this.audioBuffer = new byte[bufferSize];
    }

    /**
     * 播放指定文件
     */
    public void play(String filePath) throws UnsupportedAudioFileException,
            IOException, LineUnavailableException, InterruptedException {
        play(new File(filePath));
    }

    public void play(File file) throws UnsupportedAudioFileException,
            IOException, LineUnavailableException, InterruptedException {
        if (!isFormatSupported(file)) {
            throw new UnsupportedAudioFileException("不支持的音频格式: " + getFileExtension(file));
        }

        stop();
        audioFile = file;

        // 更新播放统计
        playCount.merge(file.getName(), 1, Integer::sum);

        // 通知监听器
        notifyPlaybackStarted(file);

        // 准备音频流
        audioStream = AudioSystem.getAudioInputStream(file);
        originalFormat = audioStream.getFormat();

        // 计算音频信息
        totalFrames = audioStream.getFrameLength();
        frameSize = originalFormat.getFrameSize();
        currentFrame = 0;

        // 格式转换（如果需要）
        if (!isFormatSupported(originalFormat)) {
            AudioFormat targetFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    originalFormat.getSampleRate(),
                    16,
                    originalFormat.getChannels(),
                    originalFormat.getChannels() * 2,
                    originalFormat.getSampleRate(),
                    false
            );
            audioStream = AudioSystem.getAudioInputStream(targetFormat, audioStream);
            frameSize = targetFormat.getFrameSize();
            totalFrames = audioStream.getFrameLength();
        }

        // 打开音频设备
        AudioFormat format = audioStream.getFormat();
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        sourceDataLine = (SourceDataLine) AudioSystem.getLine(info);
        sourceDataLine.open(format);

        // 初始化控制功能
        initAudioControls();

        // 设置播放状态
        playing = true;
        paused = false;
        playbackState = STATE_PLAYING;
        startTime = System.currentTimeMillis();

        // 启动播放线程
        playbackThread = new Thread(this::streamPlayback);
        playbackThread.setDaemon(true);
        playbackThread.start();
    }

    /**
     * 初始化音频控制功能
     */
    private void initAudioControls() {
        // 音量控制
        try {
            volumeControl = (FloatControl) sourceDataLine.getControl(FloatControl.Type.MASTER_GAIN);
            volumeSupported = true;
            minVolume = volumeControl.getMinimum();
            maxVolume = volumeControl.getMaximum();
            setVolume(currentVolume);
        } catch (IllegalArgumentException e) {
            try {
                volumeControl = (FloatControl) sourceDataLine.getControl(FloatControl.Type.VOLUME);
                volumeSupported = true;
                minVolume = volumeControl.getMinimum();
                maxVolume = volumeControl.getMaximum();
                setVolume(currentVolume);
            } catch (IllegalArgumentException ex) {
                volumeSupported = false;
            }
        }

        // 音效控制
        initSoundControls();
    }

    /**
     * 初始化音效控制
     */
    private void initSoundControls() {
        soundControls.clear();



        equalizerSupported = !soundControls.isEmpty();
    }

    /**
     * 设置音效参数
     */
    public boolean setSoundEffect(String effectType, float value) {
        FloatControl control = soundControls.get(effectType.toUpperCase());
        if (control != null) {
            try {
                control.setValue(value);
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        return false;
    }

    /**
     * 获取当前位置信息
     *
     * @return 格式化的位置信息字符串
     */
    public String getPositionInfo() {
        if (!positionSupported) {
            return "位置控制不支持";
        }

        double currentTime = getCurrentTime();
        double totalTime = getTotalTime();
        double progress = getPlaybackProgress() * 100;

        return String.format("当前位置: %d/%d 帧 (%.1f/%.1f 秒, %.1f%%)",
                currentFrame, totalFrames, currentTime, totalTime, progress);
    }

    /**
     * 增加音量
     *
     * @param increment 增加的量，范围0.0-1.0
     * @return 是否成功
     */
    public boolean increaseVolume(double increment) {
        int var_1 = (int) (currentVolume * 100);
        int var_2 = (int) (increment * 100);
        int var = var_1 + var_2;
        return setVolume(var / 100.0);
    }

    /**
     * 减小音量
     *
     * @param decrement 减小的量，范围0.0-1.0
     * @return 是否成功
     */
    public boolean decreaseVolume(double decrement) {
        int var_1 = (int) (currentVolume * 100.0);
        int var_2 = (int) (decrement * 100.0);
        int var = var_1 - var_2;
        return setVolume(var / 100.0);
    }

    /**
     * 获取支持的音效类型
     */
    public List<String> getSupportedSoundEffects() {
        return new ArrayList<>(soundControls.keySet());
    }

    // 播放控制方法（保持原有基础功能）
    public void pause() {
        if (sourceDataLine != null && sourceDataLine.isRunning()) {
            synchronized (playLock) {
                sourceDataLine.stop();
                paused = true;
                playbackState = STATE_PAUSED;
                notifyPlaybackPaused();
            }
        }
    }

    public void resume() {
        if (sourceDataLine != null && !sourceDataLine.isRunning() && paused) {
            synchronized (playLock) {
                sourceDataLine.start();
                paused = false;
                playbackState = STATE_PLAYING;
                playLock.notifyAll();
                notifyPlaybackResumed();
            }
        }
    }

    public void stop() {
        if (sourceDataLine != null) {
            playing = false;
            paused = false;
            buffering = false;

            try {
                if (playbackThread != null && playbackThread.isAlive()) {
                    playbackThread.join(1000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            closeResources();
            playbackState = STATE_STOPPED;
            currentFrame = 0;

            // 更新总播放时间
            if (startTime > 0) {
                totalPlayTime += System.currentTimeMillis() - startTime;
                startTime = 0;
            }

            notifyPlaybackStopped();
        }
    }

    // 位置控制方法（优化版本）
    public boolean seekToTime(double seconds) {
        if (!positionSupported || audioStream == null) return false;

        float frameRate = originalFormat.getFrameRate();
        long targetFrame = (long) (seconds * frameRate);
        return seekToFrame(targetFrame);
    }

    public boolean seekToFrame(long frame) {
        if (!positionSupported || audioFile == null || frame < 0 || frame >= totalFrames) {
            return false;
        }

        boolean wasPlaying = (playbackState == STATE_PLAYING);

        if (wasPlaying) pause();

        try {
            if (audioStream != null) audioStream.close();

            audioStream = AudioSystem.getAudioInputStream(audioFile);

            if (!isFormatSupported(originalFormat)) {
                AudioFormat targetFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        originalFormat.getSampleRate(),
                        16,
                        originalFormat.getChannels(),
                        originalFormat.getChannels() * 2,
                        originalFormat.getSampleRate(),
                        false
                );
                audioStream = AudioSystem.getAudioInputStream(targetFormat, audioStream);
            }

            long bytesToSkip = frame * frameSize;
            long skipped = audioStream.skip(bytesToSkip);
            currentFrame = frame;

            if (wasPlaying) resume();

            notifyPositionChanged(getPlaybackProgress());
            return true;
        } catch (Exception e) {
            notifyError(e);
            return false;
        }
    }

    // 播放列表管理功能
    public void addToPlaylist(File file) {
        if (isFormatSupported(file)) {
            playlist.add(file);
            if (shuffleMode) {
                updateShuffledPlaylist();
            }
        }
    }

    public void addToPlaylist(List<File> files) {
        files.stream().filter(this::isFormatSupported).forEach(playlist::add);
        if (shuffleMode) {
            updateShuffledPlaylist();
        }
    }

    public void removeFromPlaylist(int index) {
        if (index >= 0 && index < playlist.size()) {
            playlist.remove(index);
            if (shuffleMode) {
                updateShuffledPlaylist();
            }
        }
    }

    public void clearPlaylist() {
        playlist.clear();
        shuffledPlaylist = null;
        currentTrackIndex.set(0);
    }

    public void nextTrack() throws Exception {
        List<File> currentList = shuffleMode ? shuffledPlaylist : playlist;
        if (currentList.isEmpty()) return;

        int currentIndex = currentTrackIndex.get();
        int nextIndex = (currentIndex + 1) % currentList.size();

        if (nextIndex == 0 && !repeatMode) {
            stop();
            return;
        }

        File currentFile = currentList.get(currentIndex);
        File nextFile = currentList.get(nextIndex);

        currentTrackIndex.set(nextIndex);
        play(nextFile);
        notifyTrackChanged(currentFile, nextFile);
    }

    public void previousTrack() throws Exception {
        List<File> currentList = shuffleMode ? shuffledPlaylist : playlist;
        if (currentList.isEmpty()) return;

        int currentIndex = currentTrackIndex.get();
        int prevIndex = (currentIndex - 1 + currentList.size()) % currentList.size();

        File currentFile = currentList.get(currentIndex);
        File prevFile = currentList.get(prevIndex);

        currentTrackIndex.set(prevIndex);
        play(prevFile);
        notifyTrackChanged(currentFile, prevFile);
    }

    public void setShuffleMode(boolean shuffle) {
        this.shuffleMode = shuffle;
        if (shuffle) {
            updateShuffledPlaylist();
        }
    }

    private void updateShuffledPlaylist() {
        shuffledPlaylist = new ArrayList<>(playlist);
        Collections.shuffle(shuffledPlaylist);
    }

    // 事件监听器管理
    public void addPlaybackEventListener(PlaybackEventListener listener) {
        listeners.add(listener);
    }

    public void removePlaybackEventListener(PlaybackEventListener listener) {
        listeners.remove(listener);
    }

    private void notifyPlaybackStarted(File file) {
        listeners.forEach(listener -> listener.onPlaybackStarted(file));
    }

    private void notifyPlaybackPaused() {
        listeners.forEach(PlaybackEventListener::onPlaybackPaused);
    }

    private void notifyPlaybackResumed() {
        listeners.forEach(PlaybackEventListener::onPlaybackResumed);
    }

    private void notifyPlaybackStopped() {
        listeners.forEach(PlaybackEventListener::onPlaybackStopped);
    }

    private void notifyPlaybackFinished() {
        listeners.forEach(PlaybackEventListener::onPlaybackFinished);
    }

    private void notifyTrackChanged(File previous, File next) {
        listeners.forEach(listener -> listener.onTrackChanged(previous, next));
    }

    private void notifyError(Exception e) {
        listeners.forEach(listener -> listener.onError(e));
    }

    private void notifyPositionChanged(double position) {
        listeners.forEach(listener -> listener.onPositionChanged(position));
    }

    private void notifyVolumeChanged(double volume) {
        listeners.forEach(listener -> listener.onVolumeChanged(volume));
    }

    // 流式播放核心逻辑（改进版）
    private void streamPlayback() {
        try {
            sourceDataLine.start();
            int bytesRead;

            while (playing) {
                synchronized (playLock) {
                    while (paused) {
                        playLock.wait();
                    }
                }

                // 读取音频数据
                bytesRead = audioStream.read(audioBuffer, 0, bufferSize);

                if (bytesRead == -1) {
                    // 播放完成
                    handlePlaybackCompletion();
                    break;
                }

                if (bytesRead > 0) {
                    // 写入音频设备
                    sourceDataLine.write(audioBuffer, 0, bytesRead);

                    // 更新当前位置
                    int framesRead = bytesRead / frameSize;
                    currentFrame += framesRead;

                    // 通知位置变化
                    if (currentFrame % 1000 == 0) { // 减少通知频率
                        notifyPositionChanged(getPlaybackProgress());
                    }
                }
            }

            sourceDataLine.drain();
        } catch (IOException | InterruptedException e) {
            notifyError(e);
        } finally {
            if (playing) {
                closeResources();
            }
            playbackState = STATE_STOPPED;
        }
    }

    private void handlePlaybackCompletion() {
        notifyPlaybackFinished();

        if (repeatMode || !playlist.isEmpty()) {
            try {
                if (playlist.size() > 1) {
                    nextTrack();
                } else {
                    // 单曲循环
                    seekToFrame(0);
                    if (paused) {
                        resume();
                    }
                }
            } catch (Exception e) {
                notifyError(e);
            }
        } else {
            stop();
        }
    }

    // 工具方法
    private boolean isFormatSupported(File file) {
        String extension = getFileExtension(file).toLowerCase();
        return SUPPORTED_FORMATS.contains(extension);
    }

    private boolean isFormatSupported(AudioFormat format) {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        return AudioSystem.isLineSupported(info);
    }

    private String getFileExtension(File file) {
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        return lastDot > 0 ? name.substring(lastDot + 1) : "";
    }

    private void closeResources() {
        try {
            if (sourceDataLine != null) {
                sourceDataLine.stop();
                sourceDataLine.close();
                sourceDataLine = null;
            }
            if (audioStream != null) {
                audioStream.close();
                audioStream = null;
            }
        } catch (IOException e) {
            notifyError(e);
        }
    }

    // 新增功能：音频信息获取
    public Map<String, Object> getAudioMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        if (audioFile != null) {
            metadata.put("文件名", audioFile.getName());
            metadata.put("文件大小", audioFile.length() + " bytes");
            metadata.put("路径", audioFile.getAbsolutePath());
        }
        if (originalFormat != null) {
            metadata.put("采样率", originalFormat.getSampleRate() + " Hz");
            metadata.put("声道数", originalFormat.getChannels());
            metadata.put("编码", originalFormat.getEncoding());
            metadata.put("位深度", originalFormat.getSampleSizeInBits());
        }
        return metadata;
    }

    // 统计信息获取
    public Map<String, Object> getPlaybackStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("总播放时间", totalPlayTime + " ms");
        stats.put("当前曲目播放次数", playCount.getOrDefault(
                audioFile != null ? audioFile.getName() : "", 0));
        stats.put("播放列表大小", playlist.size());
        return stats;
    }

    // 保留原有的基础功能方法（音量控制、位置获取等）
    public boolean setVolume(double volume) {
        if (!volumeSupported || volumeControl == null) return false;

        currentVolume = Math.max(0.0, Math.min(1.0, volume));

        try {
            if (volumeControl.getType() == FloatControl.Type.MASTER_GAIN) {
                float dB = (float) (Math.log10(currentVolume == 0 ? 0.0001 : currentVolume) * 20.0);
                dB = Math.max(minVolume, Math.min(maxVolume, dB));
                volumeControl.setValue(dB);
            } else {
                float linearVolume = (float) (minVolume + (maxVolume - minVolume) * currentVolume);
                volumeControl.setValue(linearVolume);
            }
            notifyVolumeChanged(currentVolume);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public double getCurrentTime() {
        return originalFormat != null ? currentFrame / originalFormat.getFrameRate() : 0;
    }

    public double getTotalTime() {
        return originalFormat != null ? totalFrames / originalFormat.getFrameRate() : 0;
    }

    public double getPlaybackProgress() {
        return totalFrames > 0 ? (double) currentFrame / totalFrames : 0;
    }

    // 新增：保存和加载播放列表
    public void savePlaylist(String filePath) throws IOException {
        // 简化的播放列表保存功能
        Properties props = new Properties();
        for (int i = 0; i < playlist.size(); i++) {
            props.setProperty("track." + i, playlist.get(i).getAbsolutePath());
        }
        // 实际实现中可以使用XML或JSON格式
    }

    public void loadPlaylist(String filePath) throws IOException {
        // 简化的播放列表加载功能
        Properties props = new Properties();
        // 实际实现中可以从文件加载
        playlist.clear();
    }

    // 测试主方法
    public static void main(String[] args) {
        try {
            AdvancedStreamAudioPlayer player = new AdvancedStreamAudioPlayer();

            // 添加事件监听器
            player.addPlaybackEventListener(new PlaybackEventListener() {
                @Override public void onPlaybackStarted(File file) {
                    System.out.println("开始播放: " + file.getName());
                }
                @Override public void onPlaybackPaused() {
                    System.out.println("播放暂停");
                }
                @Override public void onPlaybackResumed() {
                    System.out.println("播放继续");
                }
                @Override public void onPlaybackStopped() {
                    System.out.println("播放停止");
                }
                @Override public void onPlaybackFinished() {
                    System.out.println("播放完成");
                }
                @Override public void onTrackChanged(File previous, File next) {
                    System.out.println("切换曲目: " + previous.getName() + " -> " + next.getName());
                }
                @Override public void onError(Exception e) {
                    System.err.println("播放错误: " + e.getMessage());
                }
                @Override public void onPositionChanged(double position) {
                    System.out.printf("播放进度: %.1f%%\n", position * 100);
                }
                @Override public void onVolumeChanged(double volume) {
                    System.out.printf("音量改变: %.0f%%\n", volume * 100);
                }
            });

            // 测试播放
            player.play("114.wav");
            TimeUnit.SECONDS.sleep(2);

            // 测试音量控制
            player.setVolume(0.5);
            TimeUnit.SECONDS.sleep(1);

            // 测试跳转
            player.seekToTime(20.0);
            TimeUnit.SECONDS.sleep(3);

            // 测试跳转
            player.seekToTime(0.0);
            TimeUnit.SECONDS.sleep(3);

            // 测试跳转
            player.seekToTime(120.0);
            TimeUnit.SECONDS.sleep(3);

            player.stop();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
