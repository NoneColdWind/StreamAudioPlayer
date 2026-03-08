package cn.ncw.music.stream;

import lombok.Getter;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
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

    // 播放模式常量
    public static final int PLAY_MODE_NORMAL = 0;      // 顺序播放
    public static final int PLAY_MODE_REPEAT_ONE = 1;  // 单曲循环
    public static final int PLAY_MODE_REPEAT_ALL = 2;  // 顺序循环
    public static final int PLAY_MODE_SHUFFLE = 3;     // 无序循环

    // 支持的文件格式
    private static final Set<String> SUPPORTED_FORMATS = Set.of(
            "wav", "mp3", "aiff", "au", "snd", "flac"
    );

    // 成员变量
    private SourceDataLine sourceDataLine;
    private volatile Thread playbackThread;
    private AudioInputStream audioStream;
    private volatile boolean playing;
    private volatile boolean paused;

    @Getter
    private volatile int playbackState;
    private final Object playLock = new Object();
    private final Object audioStreamLock = new Object();

    // 音量控制相关
    private FloatControl volumeControl;

    @Getter
    private double currentVolume = 0.8;
    private float minVolume;
    private float maxVolume;
    private boolean volumeSupported = false;

    // 位置控制相关
    private File audioFile;
    private AudioFormat originalFormat;

    @Getter
    private long totalFrames;

    @Getter
    private volatile long currentFrame;
    private int frameSize;

    @Getter
    private boolean positionSupported = true;

    // 播放列表相关
    private List<File> playlist;
    private AtomicInteger currentTrackIndex;

    @Getter
    private volatile int playMode = PLAY_MODE_NORMAL;
    private List<Integer> shuffleIndices;  // 用于记录随机播放顺序
    private AtomicBoolean isShuffleGenerated = new AtomicBoolean(false);

    // 线程池管理
    private ExecutorService executorService;
    private final AtomicBoolean executorShutdown = new AtomicBoolean(false);

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
        void onPlayModeChanged(int newMode);
        void onPlaylistUpdated();
    }

    public AdvancedStreamAudioPlayer() {
        this.playlist = new ArrayList<>();
        this.currentTrackIndex = new AtomicInteger(0);
        this.playCount = new ConcurrentHashMap<>();
        this.listeners = new CopyOnWriteArrayList<>();
        this.soundControls = new HashMap<>();
        this.audioBuffer = new byte[bufferSize];
        this.shuffleIndices = new ArrayList<>();

        // 初始化线程池
        initExecutorService();
    }

    /**
     * 初始化线程池
     */
    private void initExecutorService() {
        executorService = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName("AudioPlayer-Worker-" + thread.getId());
            return thread;
        });
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
        synchronized (audioStreamLock) {
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
        startPlaybackThread();
    }

    /**
     * 启动播放线程
     */
    private void startPlaybackThread() {
        // 清理之前的线程
        cleanupPlaybackThread();

        playbackThread = new Thread(this::streamPlayback);
        playbackThread.setName("AudioPlayer-Playback-" + System.currentTimeMillis());
        playbackThread.setDaemon(true);
        playbackThread.setPriority(Thread.MAX_PRIORITY);
        playbackThread.start();
    }

    /**
     * 清理播放线程
     */
    private void cleanupPlaybackThread() {
        if (playbackThread != null && playbackThread.isAlive()) {
            try {
                playing = false;
                paused = false;

                // 中断线程
                playbackThread.interrupt();

                // 等待线程结束
                try {
                    playbackThread.join(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // 确保线程被清理
                playbackThread = null;
            } catch (Exception e) {
                notifyError(e);
            }
        }
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

        // 尝试获取各种音效控制
        if (sourceDataLine != null) {
            Control[] controls = sourceDataLine.getControls();
            for (Control control : controls) {
                if (control instanceof FloatControl) {
                    FloatControl floatControl = (FloatControl) control;
                    soundControls.put(floatControl.getType().toString(), floatControl);
                }
            }
        }

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
     * 设置播放模式
     */
    public void setPlayMode(int mode) {
        if (mode < 0 || mode > 3) {
            throw new IllegalArgumentException("无效的播放模式");
        }

        this.playMode = mode;

        // 如果是随机播放模式，重新生成随机列表
        if (mode == PLAY_MODE_SHUFFLE) {
            generateShuffleList();
        }

        notifyPlayModeChanged(mode);
    }

    /**
     * 生成随机播放列表
     */
    private void generateShuffleList() {
        shuffleIndices.clear();
        for (int i = 0; i < playlist.size(); i++) {
            shuffleIndices.add(i);
        }
        Collections.shuffle(shuffleIndices);
        isShuffleGenerated.set(true);
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
        double newVolume = Math.min(1.0, currentVolume + increment);
        return setVolume(newVolume);
    }

    /**
     * 减小音量
     *
     * @param decrement 减小的量，范围0.0-1.0
     * @return 是否成功
     */
    public boolean decreaseVolume(double decrement) {
        double newVolume = Math.max(0.0, currentVolume - decrement);
        return setVolume(newVolume);
    }

    /**
     * 获取支持的音效类型
     */
    public List<String> getSupportedSoundEffects() {
        return new ArrayList<>(soundControls.keySet());
    }

    // 播放控制方法
    public void pause() {
        synchronized (playLock) {
            if (sourceDataLine != null && sourceDataLine.isRunning()) {
                sourceDataLine.stop();
                paused = true;
                playbackState = STATE_PAUSED;
                notifyPlaybackPaused();
            }
        }
    }

    public void resume() {
        synchronized (playLock) {
            if (sourceDataLine != null && !sourceDataLine.isRunning() && paused) {
                sourceDataLine.start();
                paused = false;
                playbackState = STATE_PLAYING;
                playLock.notifyAll();  // 唤醒等待的线程
                notifyPlaybackResumed();
            }
        }
    }

    public void stop() {
        playing = false;
        paused = false;
        buffering = false;

        // 唤醒可能等待的线程
        synchronized (playLock) {
            playLock.notifyAll();
        }

        cleanupPlaybackThread();

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

    /**
     * 安全停止播放器，清理所有资源
     */
    public void shutdown() {
        stop();

        // 关闭线程池
        if (!executorShutdown.getAndSet(true)) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    // 位置控制方法
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
            synchronized (audioStreamLock) {
                if (audioStream != null) {
                    audioStream.close();
                }

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
            }

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

            // 重新生成随机列表
            if (playMode == PLAY_MODE_SHUFFLE) {
                generateShuffleList();
            }

            notifyPlaylistUpdated();
        }
    }

    public void addToPlaylist(List<File> files) {
        files.stream().filter(this::isFormatSupported).forEach(playlist::add);

        // 重新生成随机列表
        if (playMode == PLAY_MODE_SHUFFLE) {
            generateShuffleList();
        }

        notifyPlaylistUpdated();
    }

    public void removeFromPlaylist(int index) {
        if (index >= 0 && index < playlist.size()) {
            playlist.remove(index);

            // 调整当前索引
            if (currentTrackIndex.get() >= index && currentTrackIndex.get() > 0) {
                currentTrackIndex.decrementAndGet();
            }

            // 重新生成随机列表
            if (playMode == PLAY_MODE_SHUFFLE) {
                generateShuffleList();
            }

            notifyPlaylistUpdated();
        }
    }

    public void clearPlaylist() {
        playlist.clear();
        shuffleIndices.clear();
        currentTrackIndex.set(0);
        isShuffleGenerated.set(false);
        notifyPlaylistUpdated();
    }

    /**
     * 播放下一首（根据播放模式）
     */
    public void nextTrack() throws Exception {
        if (playlist.isEmpty()) {
            stop();
            return;
        }

        switch (playMode) {
            case PLAY_MODE_REPEAT_ONE:
                // 单曲循环：重新播放当前曲目
                File currentFile = getCurrentFile();
                if (currentFile != null) {
                    play(currentFile);
                    notifyTrackChanged(currentFile, currentFile);
                }
                break;

            case PLAY_MODE_REPEAT_ALL:
                // 顺序循环
                int nextIndex = (currentTrackIndex.get() + 1) % playlist.size();
                File current = getCurrentFile();
                File next = playlist.get(nextIndex);
                currentTrackIndex.set(nextIndex);
                play(next);
                if (current != null) {
                    notifyTrackChanged(current, next);
                }
                break;

            case PLAY_MODE_SHUFFLE:
                // 无序循环
                if (!isShuffleGenerated.get() || shuffleIndices.isEmpty()) {
                    generateShuffleList();
                }

                int currentShuffleIndex = findCurrentShuffleIndex();
                int nextShuffleIndex = (currentShuffleIndex + 1) % shuffleIndices.size();
                int nextTrackIndex = shuffleIndices.get(nextShuffleIndex);

                File shuffleCurrent = playlist.get(shuffleIndices.get(currentShuffleIndex));
                File shuffleNext = playlist.get(nextTrackIndex);
                currentTrackIndex.set(nextTrackIndex);
                play(shuffleNext);
                notifyTrackChanged(shuffleCurrent, shuffleNext);
                break;

            case PLAY_MODE_NORMAL:
            default:
                // 顺序播放
                int normalNextIndex = currentTrackIndex.get() + 1;
                if (normalNextIndex >= playlist.size()) {
                    stop();
                    notifyPlaybackFinished();
                } else {
                    File normalCurrent = getCurrentFile();
                    File normalNext = playlist.get(normalNextIndex);
                    currentTrackIndex.set(normalNextIndex);
                    play(normalNext);
                    if (normalCurrent != null) {
                        notifyTrackChanged(normalCurrent, normalNext);
                    }
                }
                break;
        }
    }

    /**
     * 播放上一首（根据播放模式）
     */
    public void previousTrack() throws Exception {
        if (playlist.isEmpty()) return;

        switch (playMode) {
            case PLAY_MODE_REPEAT_ONE:
                // 单曲循环：重新播放当前曲目
                File currentFile = getCurrentFile();
                if (currentFile != null) {
                    play(currentFile);
                    notifyTrackChanged(currentFile, currentFile);
                }
                break;

            case PLAY_MODE_REPEAT_ALL:
                // 顺序循环
                int prevIndex = (currentTrackIndex.get() - 1 + playlist.size()) % playlist.size();
                File current = getCurrentFile();
                File prev = playlist.get(prevIndex);
                currentTrackIndex.set(prevIndex);
                play(prev);
                if (current != null) {
                    notifyTrackChanged(current, prev);
                }
                break;

            case PLAY_MODE_SHUFFLE:
                // 无序循环
                if (!isShuffleGenerated.get() || shuffleIndices.isEmpty()) {
                    generateShuffleList();
                }

                int currentShuffleIndex = findCurrentShuffleIndex();
                int prevShuffleIndex = (currentShuffleIndex - 1 + shuffleIndices.size()) % shuffleIndices.size();
                int prevTrackIndex = shuffleIndices.get(prevShuffleIndex);

                File shuffleCurrent = playlist.get(shuffleIndices.get(currentShuffleIndex));
                File shufflePrev = playlist.get(prevTrackIndex);
                currentTrackIndex.set(prevTrackIndex);
                play(shufflePrev);
                notifyTrackChanged(shuffleCurrent, shufflePrev);
                break;

            case PLAY_MODE_NORMAL:
            default:
                // 顺序播放
                int normalPrevIndex = currentTrackIndex.get() - 1;
                if (normalPrevIndex < 0) {
                    // 如果是第一首，回到开头
                    normalPrevIndex = 0;
                }
                File normalCurrent = getCurrentFile();
                File normalPrev = playlist.get(normalPrevIndex);
                currentTrackIndex.set(normalPrevIndex);
                play(normalPrev);
                if (normalCurrent != null) {
                    notifyTrackChanged(normalCurrent, normalPrev);
                }
                break;
        }
    }

    /**
     * 查找当前曲目在随机列表中的位置
     */
    private int findCurrentShuffleIndex() {
        int currentIndex = currentTrackIndex.get();
        for (int i = 0; i < shuffleIndices.size(); i++) {
            if (shuffleIndices.get(i) == currentIndex) {
                return i;
            }
        }
        return 0;
    }

    // 事件监听器管理
    public void addPlaybackEventListener(PlaybackEventListener listener) {
        listeners.add(listener);
    }

    public void removePlaybackEventListener(PlaybackEventListener listener) {
        listeners.remove(listener);
    }

    private void notifyPlaybackStarted(File file) {
        executorService.submit(() -> {
            for (PlaybackEventListener listener : listeners) {
                try {
                    listener.onPlaybackStarted(file);
                } catch (Exception e) {
                    // 防止监听器异常影响播放
                    e.printStackTrace();
                }
            }
        });
    }

    private void notifyPlaybackPaused() {
        executorService.submit(() -> {
            for (PlaybackEventListener listener : listeners) {
                try {
                    listener.onPlaybackPaused();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void notifyPlaybackResumed() {
        executorService.submit(() -> {
            for (PlaybackEventListener listener : listeners) {
                try {
                    listener.onPlaybackResumed();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void notifyPlaybackStopped() {
        executorService.submit(() -> {
            for (PlaybackEventListener listener : listeners) {
                try {
                    listener.onPlaybackStopped();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void notifyPlaybackFinished() {
        executorService.submit(() -> {
            for (PlaybackEventListener listener : listeners) {
                try {
                    listener.onPlaybackFinished();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void notifyTrackChanged(File previous, File next) {
        executorService.submit(() -> {
            for (PlaybackEventListener listener : listeners) {
                try {
                    listener.onTrackChanged(previous, next);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void notifyError(Exception e) {
        executorService.submit(() -> {
            for (PlaybackEventListener listener : listeners) {
                try {
                    listener.onError(e);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private void notifyPositionChanged(double position) {
        executorService.submit(() -> {
            for (PlaybackEventListener listener : listeners) {
                try {
                    listener.onPositionChanged(position);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void notifyVolumeChanged(double volume) {
        executorService.submit(() -> {
            for (PlaybackEventListener listener : listeners) {
                try {
                    listener.onVolumeChanged(volume);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void notifyPlayModeChanged(int newMode) {
        executorService.submit(() -> {
            for (PlaybackEventListener listener : listeners) {
                try {
                    listener.onPlayModeChanged(newMode);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void notifyPlaylistUpdated() {
        executorService.submit(() -> {
            for (PlaybackEventListener listener : listeners) {
                try {
                    listener.onPlaylistUpdated();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // 流式播放核心逻辑
    private void streamPlayback() {
        try {
            sourceDataLine.start();
            int bytesRead;

            while (playing && !Thread.currentThread().isInterrupted()) {
                synchronized (playLock) {
                    while (paused && playing) {
                        try {
                            playLock.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }

                if (Thread.currentThread().isInterrupted() || !playing) {
                    break;
                }

                // 读取音频数据
                bytesRead = 0;
                synchronized (audioStreamLock) {
                    if (audioStream != null) {
                        bytesRead = audioStream.read(audioBuffer, 0, bufferSize);
                    } else {
                        break;
                    }
                }

                if (bytesRead == -1) {
                    // 播放完成
                    handlePlaybackCompletion();
                    break;
                }

                if (bytesRead > 0 && sourceDataLine != null) {
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

            if (sourceDataLine != null) {
                sourceDataLine.drain();
            }
        } catch (IOException e) {
            if (!Thread.currentThread().isInterrupted()) {
                notifyError(e);
            }
        } finally {
            if (playing) {
                closeResources();
            }
            playbackState = STATE_STOPPED;
        }
    }

    private void handlePlaybackCompletion() {
        notifyPlaybackFinished();

        try {
            // 根据播放模式处理下一首
            if (playlist.isEmpty()) {
                // 如果播放列表为空，停止播放
                stop();
            } else {
                // 使用线程池处理下一首播放
                executorService.submit(() -> {
                    try {
                        nextTrack();
                    } catch (Exception e) {
                        notifyError(e);
                    }
                });
            }
        } catch (Exception e) {
            notifyError(e);
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
            synchronized (audioStreamLock) {
                if (audioStream != null) {
                    audioStream.close();
                    audioStream = null;
                }
            }
            if (sourceDataLine != null) {
                sourceDataLine.stop();
                sourceDataLine.close();
                sourceDataLine = null;
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
        stats.put("当前播放模式", getPlayModeDescription(playMode));
        return stats;
    }

    /**
     * 获取播放模式描述
     */
    private String getPlayModeDescription(int mode) {
        switch (mode) {
            case PLAY_MODE_NORMAL: return "顺序播放";
            case PLAY_MODE_REPEAT_ONE: return "单曲循环";
            case PLAY_MODE_REPEAT_ALL: return "顺序循环";
            case PLAY_MODE_SHUFFLE: return "无序循环";
            default: return "未知模式";
        }
    }

    // 保留原有的基础功能方法
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

    // 获取播放列表
    public List<File> getPlaylist() {
        return new ArrayList<>(playlist);
    }

    // 获取当前播放文件
    public File getCurrentFile() {
        int index = currentTrackIndex.get();
        if (index >= 0 && index < playlist.size()) {
            return playlist.get(index);
        }
        return null;
    }

    // 测试主方法
    public static void main(String[] args) {
        AdvancedStreamAudioPlayer player = null;
        try {
            player = new AdvancedStreamAudioPlayer();

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
                    e.printStackTrace();
                }
                @Override public void onPositionChanged(double position) {
                    System.out.printf("播放进度: %.1f%%\n", position * 100);
                }
                @Override public void onVolumeChanged(double volume) {
                    System.out.printf("音量改变: %.0f%%\n", volume * 100);
                }
                @Override public void onPlayModeChanged(int newMode) {
                    System.out.println("播放模式改变: " + newMode);
                }
                @Override public void onPlaylistUpdated() {
                    System.out.println("播放列表已更新");
                }
            });

            // 创建测试文件列表
            List<File> testFiles = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                File file = new File("test" + i + ".wav");
                if (file.exists()) {
                    testFiles.add(file);
                }
            }

            if (testFiles.isEmpty()) {
                System.out.println("请创建test1.wav到test5.wav测试文件");
                return;
            }

            // 添加到播放列表
            player.addToPlaylist(testFiles);

            /*

            // 测试不同的播放模式
            System.out.println("\n=== 测试顺序播放 ===");
            player.setPlayMode(AdvancedStreamAudioPlayer.PLAY_MODE_NORMAL);
            player.play(testFiles.get(0).getAbsolutePath());
            TimeUnit.SECONDS.sleep(2);

            // 测试暂停/恢复
            System.out.println("\n=== 测试暂停/恢复 ===");
            player.pause();
            TimeUnit.SECONDS.sleep(1);

            player.resume();
            TimeUnit.SECONDS.sleep(2);

            // 测试音量控制
            System.out.println("\n=== 测试音量控制 ===");
            player.setVolume(0.5);
            TimeUnit.SECONDS.sleep(1);

            player.increaseVolume(0.2);
            TimeUnit.SECONDS.sleep(1);

            player.decreaseVolume(0.3);
            TimeUnit.SECONDS.sleep(1);

            // 测试跳转
            System.out.println("\n=== 测试跳转功能 ===");
            boolean seekResult = player.seekToTime(5.0);
            System.out.println("跳转结果: " + seekResult);
            TimeUnit.SECONDS.sleep(2);

            seekResult = player.seekToTime(10.0);
            System.out.println("跳转结果: " + seekResult);
            TimeUnit.SECONDS.sleep(2);

            player.stop();

            // 测试单曲循环
            System.out.println("\n=== 测试单曲循环 ===");
            player.setPlayMode(AdvancedStreamAudioPlayer.PLAY_MODE_REPEAT_ONE);
            player.play(testFiles.get(1).getAbsolutePath());
            TimeUnit.SECONDS.sleep(3);
            player.stop();

             */

            // 测试顺序循环
            System.out.println("\n=== 测试顺序循环 ===");
            player.setPlayMode(AdvancedStreamAudioPlayer.PLAY_MODE_REPEAT_ALL);
            player.addToPlaylist(testFiles);
            player.play(testFiles.get(0).getAbsolutePath());
            TimeUnit.SECONDS.sleep(1145141919810L);


            // 显示统计信息
            System.out.println("\n=== 播放统计 ===");
            Map<String, Object> stats = player.getPlaybackStatistics();
            stats.forEach((k, v) -> System.out.println(k + ": " + v));

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (player != null) {
                player.shutdown();
                System.out.println("播放器已关闭");
            }
        }
    }
}