package cn.ncw.music.stream;

import cn.ncw.logger.log.NCWLoggerFactory;
import lombok.Getter;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * 多功能流式媒体播放器 - 支持多种音频格式和高级功能
 * <p>
 * 优化要点：
 * 1. 修复字节写入不匹配问题
 * 2. 使用枚举替代int常量，提高类型安全性
 * 3. 提取播放列表管理器，分离职责
 * 4. 提取事件发布器，解耦事件处理
 * 5. 强化资源管理和异常处理
 * 6. 优化线程安全和并发控制
 * 7. 改进API设计，提高可用性
 */

public class AdvancedStreamAudioPlayer {

    private final NCWLoggerFactory logger;

    // 播放状态枚举
    public enum PlaybackState {
        PLAYING, PAUSED, STOPPED, BUFFERING, ERROR
    }

    // 播放模式枚举
    @Getter
    public enum PlayMode {
        NORMAL("顺序播放"),
        REPEAT_ONE("单曲循环"),
        REPEAT_ALL("顺序循环"),
        SHUFFLE("随机播放");

        private final String description;

        PlayMode(String description) {
            this.description = description;
        }

    }

    private PlayMode getPlayModeFromString(String playModeString) {
        return switch (playModeString) {
            case "REPEAT_ONE" -> PlayMode.REPEAT_ONE;
            case "REPEAT_ALL" -> PlayMode.REPEAT_ALL;
            case "SHUFFLE" -> PlayMode.SHUFFLE;
            default -> PlayMode.NORMAL;
        };
    }

    // 支持的文件格式
    private static final Set<String> SUPPORTED_FORMATS = Set.of(
            "wav", "mp3", "aiff", "au", "snd", "flac"
    );

    // 常量配置
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    private static final double DEFAULT_VOLUME = 0.8;
    private static final int POSITION_UPDATE_THRESHOLD = 1000; // 位置更新阈值（帧）

    // 核心音频组件
    private volatile SourceDataLine sourceDataLine;
    private volatile Thread playbackThread;
    private volatile AudioInputStream audioStream;
    private final Object audioStreamLock = new Object();

    // 播放控制
    private final AtomicBoolean playing = new AtomicBoolean(false);
    private final AtomicBoolean paused = new AtomicBoolean(false);
    @Getter
    private volatile PlaybackState playbackState = PlaybackState.STOPPED;
    private final Object playControlLock = new Object();

    // 音频信息
    private File currentAudioFile;
    private AudioFormat originalFormat;
    @Getter
    private final AtomicLong totalFrames = new AtomicLong(0);
    @Getter
    private final AtomicLong currentFrame = new AtomicLong(0);
    private int frameSize;

    // 音量控制
    private FloatControl volumeControl;
    @Getter
    private final AtomicDouble currentVolume = new AtomicDouble(DEFAULT_VOLUME);
    private float minVolume;
    private float maxVolume;
    @Getter
    private boolean volumeSupported = false;

    // 播放列表管理
    private final PlaylistManager playlistManager;

    // 音效控制
    private final Map<String, FloatControl> soundControls = new ConcurrentHashMap<>();
    @Getter
    private boolean equalizerSupported = false;

    // 事件系统
    private final PlaybackEventPublisher eventPublisher;

    // 统计信息
    private final AtomicLong totalPlayTime = new AtomicLong(0);
    private volatile long startTime = 0;
    private final Map<String, AtomicInteger> playCount = new ConcurrentHashMap<>();

    // 线程池
    private final ExecutorService executorService;
    private final AtomicBoolean executorShutdown = new AtomicBoolean(false);

    // 缓冲控制
    private final byte[] audioBuffer;
    private final int bufferSize;

    /**
     * 内部类：线程安全的Double值
     */
    private static class AtomicDouble {
        private double value;

        public AtomicDouble(double initialValue) {
            this.value = initialValue;
        }

        public synchronized double get() {
            return value;
        }

        public synchronized void set(double newValue) {
            this.value = newValue;
        }

        public synchronized double getAndSet(double newValue) {
            double oldValue = this.value;
            this.value = newValue;
            return oldValue;
        }
    }

    /**
     * 播放列表管理器 - 职责分离
     */
    private class PlaylistManager {
        private final List<File> playlist = new CopyOnWriteArrayList<>();
        private final AtomicInteger currentIndex = new AtomicInteger(0);
        @Getter
        private volatile PlayMode playMode = PlayMode.NORMAL;
        private List<Integer> shuffleIndices = Collections.emptyList();
        private final AtomicBoolean shuffleGenerated = new AtomicBoolean(false);
        private final Object shuffleLock = new Object();

        public List<File> getPlaylist() {
            return new ArrayList<>(playlist);
        }

        public int getCurrentIndex() {
            return currentIndex.get();
        }

        public File getCurrentFile() {
            int index = currentIndex.get();
            if (index >= 0 && index < playlist.size()) {
                return playlist.get(index);
            }
            return null;
        }

        public void setCurrentIndex(int index) {
            if (index >= 0 && index < playlist.size()) {
                currentIndex.set(index);
            }
        }

        public void setPlayMode(PlayMode mode) {
            this.playMode = mode;
            if (mode == PlayMode.SHUFFLE) {
                generateShuffleList();
            }
        }

        public boolean addToPlaylist(File file) {
            if (!isFormatSupported(file)) {
                return false;
            }
            playlist.add(file);
            if (playMode == PlayMode.SHUFFLE) {
                generateShuffleList();
            }
            return true;
        }

        public boolean addToPlaylist(Collection<File> files) {
            List<File> supportedFiles = files.stream()
                    .filter(AdvancedStreamAudioPlayer.this::isFormatSupported)
                    .toList();
            boolean changed = playlist.addAll(supportedFiles);
            if (changed && playMode == PlayMode.SHUFFLE) {
                generateShuffleList();
            }
            return changed;
        }

        public boolean removeFromPlaylist(int index) {
            if (index < 0 || index >= playlist.size()) {
                return false;
            }
            playlist.remove(index);

            // 调整当前索引
            int current = currentIndex.get();
            if (current == index) {
                currentIndex.set(-1);
            } else if (current > index) {
                currentIndex.decrementAndGet();
            }

            if (playMode == PlayMode.SHUFFLE) {
                generateShuffleList();
            }
            return true;
        }

        public void clearPlaylist() {
            playlist.clear();
            currentIndex.set(-1);
            shuffleIndices = Collections.emptyList();
            shuffleGenerated.set(false);
        }

        public int getNextIndex() {
            if (playlist.isEmpty()) {
                return -1;
            }

            switch (playMode) {
                case REPEAT_ONE:
                    return currentIndex.get();

                case REPEAT_ALL:
                    return (currentIndex.get() + 1) % playlist.size();

                case SHUFFLE:
                    if (!shuffleGenerated.get()) {
                        generateShuffleList();
                    }
                    if (shuffleIndices.isEmpty()) {
                        return -1;
                    }
                    int currentIdx = findCurrentShuffleIndex();
                    int nextShuffleIdx = (currentIdx + 1) % shuffleIndices.size();
                    return shuffleIndices.get(nextShuffleIdx);
                case NORMAL:
                default:
                    int normalNext = currentIndex.get() + 1;
                    return normalNext < playlist.size() ? normalNext : -1;
            }
        }

        public int getPreviousIndex() {
            if (playlist.isEmpty()) {
                return -1;
            }

            switch (playMode) {
                case REPEAT_ONE:
                    return currentIndex.get();

                case REPEAT_ALL:
                    return (currentIndex.get() - 1 + playlist.size()) % playlist.size();

                case SHUFFLE:
                    if (!shuffleGenerated.get()) {
                        generateShuffleList();
                    }
                    if (shuffleIndices.isEmpty()) {
                        return -1;
                    }
                    int currentIdx = findCurrentShuffleIndex();
                    int prevShuffleIdx = (currentIdx - 1 + shuffleIndices.size()) % shuffleIndices.size();
                    return shuffleIndices.get(prevShuffleIdx);

                case NORMAL:
                default:
                    int normalPrev = currentIndex.get() - 1;
                    return Math.max(0, normalPrev);
            }
        }

        public int size() {
            return playlist.size();
        }

        public boolean isEmpty() {
            return playlist.isEmpty();
        }

        private void generateShuffleList() {
            synchronized (shuffleLock) {
                shuffleIndices = new ArrayList<>(playlist.size());
                for (int i = 0; i < playlist.size(); i++) {
                    shuffleIndices.add(i);
                }
                Collections.shuffle(shuffleIndices);
                shuffleGenerated.set(true);
            }
        }

        private int findCurrentShuffleIndex() {
            int current = currentIndex.get();
            synchronized (shuffleLock) {
                for (int i = 0; i < shuffleIndices.size(); i++) {
                    if (shuffleIndices.get(i) == current) {
                        return i;
                    }
                }
            }
            return 0;
        }
    }

    /**
     * 事件发布器 - 职责分离
     */
    private class PlaybackEventPublisher {
        private final List<PlaybackEventListener> listeners = new CopyOnWriteArrayList<>();
        private final ExecutorService executor;

        public PlaybackEventPublisher(ExecutorService executor) {
            this.executor = executor;
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

    /**
     * 事件监听器接口
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

    /**
     * 默认构造函数
     */
    public AdvancedStreamAudioPlayer(NCWLoggerFactory loggerFactory) {
        this(DEFAULT_BUFFER_SIZE, loggerFactory);
    }

    /**
     * 可配置缓冲大小的构造函数
     */
    public AdvancedStreamAudioPlayer(int bufferSize, NCWLoggerFactory loggerFactory) {
        this.logger = loggerFactory;
        this.bufferSize = bufferSize > 0 ? bufferSize : DEFAULT_BUFFER_SIZE;
        this.audioBuffer = new byte[this.bufferSize];
        this.playlistManager = new PlaylistManager();

        // 初始化线程池
        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName("AudioPlayer-Worker-" + thread.threadId());
            thread.setUncaughtExceptionHandler((t, e) ->
                    logger.error("Uncaught exception in AudioPlayer worker thread", "AudioPlayer", (Exception) e));
            return thread;
        });

        this.eventPublisher = new PlaybackEventPublisher(executorService);
    }

    public void play(int index) throws UnsupportedAudioFileException,
            LineUnavailableException, IOException, InterruptedException {
        play(playlistManager.playlist.get(index));
    }

    public void playFirst() throws UnsupportedAudioFileException,
            LineUnavailableException, IOException, InterruptedException {
        play(playlistManager.playlist.getFirst());
    }

    public void playLast() throws UnsupportedAudioFileException,
            LineUnavailableException, IOException, InterruptedException {
        play(playlistManager.playlist.getLast());
    }

    /**
     * 播放指定文件
     */
    public void play(String filePath) throws UnsupportedAudioFileException,
            IOException, LineUnavailableException, InterruptedException {
        validateFilePath(filePath);
        play(new File(filePath));
    }

    /**
     * 播放指定文件
     */
    public void play(File file) throws UnsupportedAudioFileException,
            IOException, LineUnavailableException, InterruptedException {
        validateFile(file);

        // 停止当前播放
        stop();

        // 更新当前文件
        currentAudioFile = file;

        // 更新播放统计
        playCount.computeIfAbsent(file.getName(), k -> new AtomicInteger(0))
                .incrementAndGet();

        // 准备音频流
        prepareAudioStream(file);

        // 打开音频设备
        openAudioDevice();

        // 初始化控制功能
        initAudioControls();

        // 设置播放状态
        playing.set(true);
        paused.set(false);
        playbackState = PlaybackState.PLAYING;
        startTime = System.currentTimeMillis();

        // 通知监听器
        eventPublisher.firePlaybackStarted(file);

        // 启动播放线程
        startPlaybackThread();
    }

    /**
     * 准备音频流
     */
    private void prepareAudioStream(File file) throws UnsupportedAudioFileException, IOException {
        synchronized (audioStreamLock) {
            // 关闭现有流
            closeAudioStream();

            // 创建新流
            audioStream = AudioSystem.getAudioInputStream(file);
            originalFormat = audioStream.getFormat();

            // 计算音频信息
            long totalFramesValue = audioStream.getFrameLength();
            if (totalFramesValue == AudioSystem.NOT_SPECIFIED) {
                // 如果帧数未指定，尝试获取近似值
                totalFramesValue = estimateTotalFrames(file.length(), originalFormat);
            }
            totalFrames.set(totalFramesValue);
            frameSize = originalFormat.getFrameSize();
            currentFrame.set(0);

            // 格式转换（如果需要）
            if (isFormatSupported(originalFormat)) {
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
                long convertedFrames = audioStream.getFrameLength();
                if (convertedFrames != AudioSystem.NOT_SPECIFIED) {
                    totalFrames.set(convertedFrames);
                }
            }
        }
    }

    /**
     * 估算总帧数
     */
    private long estimateTotalFrames(long fileSize, AudioFormat format) {
        if (frameSize <= 0) {
            return 0;
        }
        return fileSize / frameSize;
    }

    /**
     * 打开音频设备
     */
    private void openAudioDevice() throws LineUnavailableException {
        AudioFormat format = audioStream.getFormat();
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        sourceDataLine = (SourceDataLine) AudioSystem.getLine(info);
        sourceDataLine.open(format);
    }

    /**
     * 启动播放线程
     */
    private void startPlaybackThread() {
        // 清理之前的线程
        cleanupPlaybackThread();

        playbackThread = new Thread(this::streamPlayback,
                "AudioPlayer-Playback-" + System.currentTimeMillis());
        playbackThread.setDaemon(true);
        playbackThread.setPriority(Thread.MAX_PRIORITY);
        playbackThread.start();
    }

    /**
     * 清理播放线程
     */
    private void cleanupPlaybackThread() {
        if (playbackThread != null && playbackThread.isAlive()) {
            playing.set(false);
            paused.set(false);

            // 中断线程
            playbackThread.interrupt();

            // 等待线程结束
            try {
                playbackThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Interrupted while waiting for playback thread to finish", "cleanup", e);
            } finally {
                playbackThread = null;
            }
        }
    }

    /**
     * 初始化音频控制功能
     */
    private void initAudioControls() {
        // 音量控制
        initVolumeControl();

        // 音效控制
        initSoundControls();
    }

    /**
     * 初始化音量控制
     */
    private void initVolumeControl() {
        try {
            volumeControl = (FloatControl) sourceDataLine.getControl(FloatControl.Type.MASTER_GAIN);
            volumeSupported = true;
            minVolume = volumeControl.getMinimum();
            maxVolume = volumeControl.getMaximum();
            setVolume(currentVolume.get());
        } catch (IllegalArgumentException e) {
            try {
                volumeControl = (FloatControl) sourceDataLine.getControl(FloatControl.Type.VOLUME);
                volumeSupported = true;
                minVolume = volumeControl.getMinimum();
                maxVolume = volumeControl.getMaximum();
                setVolume(currentVolume.get());
            } catch (IllegalArgumentException ex) {
                volumeSupported = false;
                logger.error("Volume control not supported on this audio line", "initVolume", ex);
            }
        }
    }

    /**
     * 初始化音效控制
     */
    private void initSoundControls() {
        soundControls.clear();

        if (sourceDataLine != null) {
            Control[] controls = sourceDataLine.getControls();
            for (Control control : controls) {
                if (control instanceof FloatControl floatControl) {
                    soundControls.put(floatControl.getType().toString(), floatControl);
                }
            }
        }

        equalizerSupported = !soundControls.isEmpty();
    }

    // ==================== 播放控制方法 ====================

    /**
     * 暂停播放
     */
    public void pause() {
        synchronized (playControlLock) {
            if (sourceDataLine != null && sourceDataLine.isRunning() && playing.get()) {
                sourceDataLine.stop();
                paused.set(true);
                playbackState = PlaybackState.PAUSED;
                eventPublisher.firePlaybackPaused();
            }
        }
    }

    /**
     * 恢复播放
     */
    public void resume() {
        synchronized (playControlLock) {
            if (sourceDataLine != null && !sourceDataLine.isRunning() &&
                    paused.get() && playing.get()) {
                sourceDataLine.start();
                paused.set(false);
                playbackState = PlaybackState.PLAYING;
                playControlLock.notifyAll();
                eventPublisher.firePlaybackResumed();
            }
        }
    }

    /**
     * 停止播放
     */
    public void stop() {
        playing.set(false);
        paused.set(false);

        // 唤醒可能等待的线程
        synchronized (playControlLock) {
            playControlLock.notifyAll();
        }

        cleanupPlaybackThread();
        closeResources();

        playbackState = PlaybackState.STOPPED;
        currentFrame.set(0);

        // 更新总播放时间
        if (startTime > 0) {
            totalPlayTime.addAndGet(System.currentTimeMillis() - startTime);
            startTime = 0;
        }

        eventPublisher.firePlaybackStopped();
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
                    List<Runnable> remainingTasks = executorService.shutdownNow();
                    if (!remainingTasks.isEmpty()) {
                        logger.warn("Forcefully terminated " + "remainingTasks.size()" + " remaining tasks", "shutdown");
                    }
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    // ==================== 音量控制 ====================

    /**
     * 设置音量
     */
    public boolean setVolume(double volume) {
        if (!volumeSupported || volumeControl == null) {
            return false;
        }

        double newVolume = Math.max(0.0, Math.min(1.0, volume));
        double oldVolume = currentVolume.getAndSet(newVolume);

        try {
            if (volumeControl.getType() == FloatControl.Type.MASTER_GAIN) {

                float linearVolume = (float) (minVolume + (maxVolume - minVolume) * newVolume);
                volumeControl.setValue(linearVolume);
            }

            if (Math.abs(newVolume - oldVolume) > 0.01) {
                eventPublisher.fireVolumeChanged(newVolume);
            }
            return true;
        } catch (IllegalArgumentException e) {
            currentVolume.set(oldVolume); // 恢复原值
            logger.error("Failed to set volume", "setVolume", e);
            return false;
        }
    }

    /**
     * 增加音量
     */
    public boolean increaseVolume(double increment) {
        if (increment < 0) {
            throw new IllegalArgumentException("音量增量不能为负数");
        }
        double newVolume = Math.min(1.0, currentVolume.get() + increment);
        return setVolume(newVolume);
    }

    /**
     * 减小音量
     */
    public boolean decreaseVolume(double decrement) {
        if (decrement < 0) {
            throw new IllegalArgumentException("音量减量不能为负数");
        }
        double newVolume = Math.max(0.0, currentVolume.get() - decrement);
        return setVolume(newVolume);
    }

    // ==================== 位置控制 ====================

    /**
     * 跳转到指定时间（秒）
     */
    public boolean seekToTime(double seconds) {
        if (originalFormat == null || seconds < 0) {
            return false;
        }
        float frameRate = originalFormat.getFrameRate();
        long targetFrame = (long) (seconds * frameRate);
        return seekToFrame(targetFrame);
    }

    /**
     * 跳转到指定帧
     */
    public boolean seekToFrame(long frame) {
        if (currentAudioFile == null || originalFormat == null ||
                frame < 0 || frame >= totalFrames.get()) {
            return false;
        }

        boolean wasPlaying = (playbackState == PlaybackState.PLAYING);

        if (wasPlaying) {
            pause();
        }

        try {
            synchronized (audioStreamLock) {
                closeAudioStream();

                // 重新打开音频流
                audioStream = AudioSystem.getAudioInputStream(currentAudioFile);

                if (isFormatSupported(originalFormat)) {
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
                currentFrame.set(frame);
            }

            if (wasPlaying) {
                resume();
            }

            eventPublisher.firePositionChanged(getPlaybackProgress());
            return true;
        } catch (Exception e) {
            logger.error("Failed to seek to frame: " + frame, "seekToFrame", e);
            eventPublisher.fireError(e);
            return false;
        }
    }

    // ==================== 播放列表管理 ====================

    public void addToPlaylist(String filePath) {
        boolean added = playlistManager.addToPlaylist(new File(filePath));
        if (added) {
            eventPublisher.firePlaylistUpdated();
        }
    }

    public void addToPlaylist(File file) {
        boolean added = playlistManager.addToPlaylist(file);
        if (added) {
            eventPublisher.firePlaylistUpdated();
        }
    }

    public void addToPlaylist(List<?> files) {
        List<File> list = new ArrayList<>();
        for (Object file : files) {
            if (file instanceof File) {
                list.add((File) file);
            } else if (file instanceof String) {
                list.add(new File((String) file));
            } else {
                logger.error("Unsupported type.", "addToPlaylist");
            }
        }

        boolean added = playlistManager.addToPlaylist(list);
        if (added) {
            eventPublisher.firePlaylistUpdated();
        }
    }

    public void removeFromPlaylist(int index) {
        boolean removed = playlistManager.removeFromPlaylist(index);
        if (removed) {
            eventPublisher.firePlaylistUpdated();
        }
    }

    public void clearPlaylist() {
        playlistManager.clearPlaylist();
        eventPublisher.firePlaylistUpdated();
    }

    public List<File> getPlaylist() {
        return playlistManager.getPlaylist();
    }

    public List<String> getPlaylistPath() {
        List<String> path = new ArrayList<>();
        List<File> list = playlistManager.getPlaylist();
        for (File f : list) {
            path.add(f.getAbsolutePath());
        }
        return path;
    }

    public File getCurrentFile() {
        return playlistManager.getCurrentFile();
    }

    public String getCurrentFilePath() {
        return Objects.requireNonNull(playlistManager.getCurrentFile()).getAbsolutePath();
    }

    public void setPlayMode(String mode) {
        setPlayMode(getPlayModeFromString(mode));
    }

    public void setPlayMode(PlayMode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("播放模式不能为null");
        }
        PlayMode oldMode = playlistManager.getPlayMode();
        playlistManager.setPlayMode(mode);
        if (oldMode != mode) {
            eventPublisher.firePlayModeChanged(mode);
        }
    }

    public PlayMode getPlayMode() {
        return playlistManager.getPlayMode();
    }

    public String getPlayModeDescription() {
        return playlistManager.getPlayMode().name();
    }


    /**
     * 播放下一首
     */
    public void nextTrack() {
        int nextIndex = playlistManager.getNextIndex();
        if (nextIndex == -1) {
            stop();
            eventPublisher.firePlaybackFinished();
            return;
        }

        try {
            File current = getCurrentFile();
            playlistManager.setCurrentIndex(nextIndex);
            File next = playlistManager.getPlaylist().get(nextIndex);

            play(next);
            if (current != null) {
                eventPublisher.fireTrackChanged(current, next);
            }
        } catch (Exception e) {
            logger.error("Failed to play next track.", "nextTrack", e);
            eventPublisher.fireError(e);
        }
    }

    /**
     * 播放上一首
     */
    public void previousTrack() {
        int prevIndex = playlistManager.getPreviousIndex();
        if (prevIndex == -1) {
            return;
        }

        try {
            File current = getCurrentFile();
            playlistManager.setCurrentIndex(prevIndex);
            File previous = playlistManager.getPlaylist().get(prevIndex);

            play(previous);
            if (current != null) {
                eventPublisher.fireTrackChanged(current, previous);
            }
        } catch (Exception e) {
            logger.error("Failed to play previous track.", "previousTrack", e);
            eventPublisher.fireError(e);
        }
    }

    // ==================== 事件监听器管理 ====================

    public void addPlaybackEventListener(PlaybackEventListener listener) {
        eventPublisher.addListener(listener);
    }

    public void removePlaybackEventListener(PlaybackEventListener listener) {
        eventPublisher.removeListener(listener);
    }

    public void removeAllPlaybackEventListeners() {
        eventPublisher.removeAllListeners();
    }

    // ==================== 核心播放逻辑 ====================

    /**
     * 流式播放核心逻辑 - 修复字节写入不匹配问题
     */
    private void streamPlayback() {
        try {
            sourceDataLine.start();

            while (playing.get() && !Thread.currentThread().isInterrupted()) {
                synchronized (playControlLock) {
                    while (paused.get() && playing.get()) {
                        try {
                            playControlLock.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }

                if (Thread.currentThread().isInterrupted() || !playing.get()) {
                    break;
                }

                // 读取音频数据
                int bytesRead = 0;
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
                    // **修复字节写入不匹配问题** - 确保写入所有读取的数据
                    int bytesWritten = 0;

                    // 循环写入，直到所有数据都被写入
                    while (bytesWritten < bytesRead &&
                            playing.get() && !Thread.currentThread().isInterrupted()) {

                        int writeResult = sourceDataLine.write(
                                audioBuffer, bytesWritten, bytesRead - bytesWritten);

                        if (writeResult < 0) {
                            // 写入错误
                            logger.error("Failed to write to audio line, result: " + writeResult, "playback");
                            break;
                        }

                        bytesWritten += writeResult;

                        // 检查是否需要暂停
                        synchronized (playControlLock) {
                            while (paused.get() && playing.get()) {
                                try {
                                    playControlLock.wait();
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    break;
                                }
                            }
                        }
                    }

                    if (bytesWritten != bytesRead) {
                        logger.warn("Bytes written (" + bytesWritten + ") doesn't match bytes read (" + bytesRead + ")", "playback");
                    }

                    // 更新当前位置
                    int framesRead = bytesRead / frameSize;
                    long newFrame = currentFrame.addAndGet(framesRead);

                    // 通知位置变化（降低频率）
                    if (newFrame % POSITION_UPDATE_THRESHOLD == 0) {
                        eventPublisher.firePositionChanged(getPlaybackProgress());
                    }
                }
            }

            // 清空音频线缓冲区
            if (sourceDataLine != null) {
                sourceDataLine.drain();
            }
        } catch (IOException e) {
            if (!Thread.currentThread().isInterrupted()) {
                logger.error("I/O error during playback.", "playback", e);
                eventPublisher.fireError(e);
            }
        } finally {
            closeResources();
            playbackState = PlaybackState.STOPPED;
        }
    }

    /**
     * 处理播放完成
     */
    private void handlePlaybackCompletion() {
        eventPublisher.firePlaybackFinished();

        executorService.submit(() -> {
            try {
                if (playlistManager.isEmpty()) {
                    stop();
                } else {
                    nextTrack();
                }
            } catch (Exception e) {
                logger.error("Error handling playback completion.", "handlePlayback", e);
                eventPublisher.fireError(e);
            }
        });
    }

    // ==================== 工具方法 ====================

    /**
     * 验证文件路径
     */
    private void validateFilePath(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("文件路径不能为空");
        }
    }

    /**
     * 验证文件
     */
    private void validateFile(File file) {
        if (file == null) {
            throw new IllegalArgumentException("文件不能为null");
        }
        if (!file.exists()) {
            throw new IllegalArgumentException("文件不存在: " + file.getAbsolutePath());
        }
        if (!file.canRead()) {
            throw new IllegalArgumentException("文件不可读: " + file.getAbsolutePath());
        }
        if (!isFormatSupported(file)) {
            throw new IllegalArgumentException("不支持的音频格式: " + getFileExtension(file));
        }
    }

    private boolean isFormatSupported(File file) {
        String extension = getFileExtension(file).toLowerCase();
        return SUPPORTED_FORMATS.contains(extension);
    }

    private boolean isFormatSupported(AudioFormat format) {
        if (format == null) return true;
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        return !AudioSystem.isLineSupported(info);
    }

    private String getFileExtension(File file) {
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        return lastDot > 0 ? name.substring(lastDot + 1) : "";
    }

    /**
     * 关闭音频资源
     */
    private void closeAudioStream() {
        try {
            if (audioStream != null) {
                audioStream.close();
                audioStream = null;
            }
        } catch (IOException e) {
            logger.error("Error closing audio stream.", "closeStream", e);
        }
    }

    /**
     * 关闭所有资源
     */
    private void closeResources() {
        synchronized (audioStreamLock) {
            closeAudioStream();
        }

        if (sourceDataLine != null) {
            try {
                if (sourceDataLine.isRunning()) {
                    sourceDataLine.stop();
                }
                sourceDataLine.close();
            } catch (Exception e) {
                logger.error("Error closing source data line.", "closeResources", e);
            } finally {
                sourceDataLine = null;
            }
        }
    }

    // ==================== 查询方法 ====================

    public double getCurrentTime() {
        if (originalFormat == null) return 0;
        return currentFrame.get() / originalFormat.getFrameRate();
    }

    public double getTotalTime() {
        if (originalFormat == null) return 0;
        return totalFrames.get() / originalFormat.getFrameRate();
    }

    public double getPlaybackProgress() {
        long total = totalFrames.get();
        if (total <= 0) return 0;
        return (double) currentFrame.get() / total;
    }

    public String getFormattedCurrentTime() {
        return formatTime(getCurrentTime());
    }

    public String getFormattedTotalTime() {
        return formatTime(getTotalTime());
    }

    private String formatTime(double seconds) {
        int totalSeconds = (int) seconds;
        int minutes = totalSeconds / 60;
        int remainingSeconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, remainingSeconds);
    }

    public String getPositionInfo() {
        if (originalFormat == null) {
            return "未加载音频文件";
        }

        double currentTime = getCurrentTime();
        double totalTime = getTotalTime();
        double progress = getPlaybackProgress() * 100;

        return String.format("%s / %s (%.1f%%)",
                getFormattedCurrentTime(),
                getFormattedTotalTime(),
                progress);
    }

    public Map<String, Object> getAudioMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        if (currentAudioFile != null) {
            metadata.put("filename", currentAudioFile.getName());
            metadata.put("size", currentAudioFile.length());
            metadata.put("path", currentAudioFile.getAbsolutePath());
        }
        if (originalFormat != null) {
            metadata.put("sampleRate", originalFormat.getSampleRate());
            metadata.put("channels", originalFormat.getChannels());
            metadata.put("encoding", originalFormat.getEncoding().toString());
            metadata.put("bitDepth", originalFormat.getSampleSizeInBits());
            metadata.put("frameRate", originalFormat.getFrameRate());
            metadata.put("frameSize", originalFormat.getFrameSize());
        }
        return metadata;
    }

    public Map<String, Object> getPlaybackStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPlayTimeMs", totalPlayTime.get());
        stats.put("currentTrackPlayCount",
                playCount.getOrDefault(currentAudioFile != null ?
                        currentAudioFile.getName() : "", new AtomicInteger(0)).get());
        stats.put("playlistSize", playlistManager.size());
        stats.put("playMode", playlistManager.getPlayMode().getDescription());
        stats.put("playbackState", playbackState.name());
        return stats;
    }

    public boolean isPlaying() {
        return playbackState == PlaybackState.PLAYING;
    }

    public boolean isPaused() {
        return playbackState == PlaybackState.PAUSED;
    }

    public boolean isStopped() {
        return playbackState == PlaybackState.STOPPED;
    }

    public List<String> getSupportedSoundEffects() {
        return new ArrayList<>(soundControls.keySet());
    }

    public boolean setSoundEffect(String effectType, float value) {
        FloatControl control = soundControls.get(effectType.toUpperCase());
        if (control != null) {
            try {
                control.setValue(value);
                return true;
            } catch (IllegalArgumentException e) {
                logger.error("Invalid value for sound effect: " + effectType, "setEffect", e);
                return false;
            }
        }
        return false;
    }

    /**
     * 获取支持的音频格式集合
     */
    public static Set<String> getSupportedFormats() {
        return new HashSet<>(SUPPORTED_FORMATS);
    }

    /**
     * 检查是否支持特定格式的文件
     */
    public static boolean isFileFormatSupported(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return false;
        }
        String extension = filePath.substring(filePath.lastIndexOf('.') + 1).toLowerCase();
        return SUPPORTED_FORMATS.contains(extension);
    }

    // ==================== 测试主方法 ====================

    /**
     * 测试主方法
     */
    static void main() {
        AdvancedStreamAudioPlayer player = null;
        NCWLoggerFactory loggerFactory = new NCWLoggerFactory("Test");
        try {
            player = new AdvancedStreamAudioPlayer(loggerFactory);

            // 添加事件监听器
            AdvancedStreamAudioPlayer finalPlayer = player;
            player.addPlaybackEventListener(new PlaybackEventListener() {
                @Override
                public void onPlaybackStarted(File file) {
                    System.out.println("开始播放: " + file.getName());
                }

                @Override
                public void onPlaybackPaused() {
                    System.out.println("播放暂停");
                }

                @Override
                public void onPlaybackResumed() {
                    System.out.println("播放继续");
                }

                @Override
                public void onPlaybackStopped() {
                    System.out.println("播放停止");
                }

                @Override
                public void onPlaybackFinished() {
                    System.out.println("播放完成");
                }

                @Override
                public void onTrackChanged(File previous, File next) {
                    System.out.println("切换曲目: " +
                            (previous != null ? previous.getName() : "无") +
                            " -> " + next.getName());
                }

                @Override
                public void onError(Exception e) {
                    System.err.println("播放错误: " + e.getMessage());
                    e.printStackTrace();
                }

                @Override
                public void onPositionChanged(double position) {
                    System.out.printf("播放进度: %.1f%%\n", position * 100);
                }

                @Override
                public void onVolumeChanged(double volume) {
                    System.out.printf("音量改变: %.0f%%\n", volume * 100);
                }

                @Override
                public void onPlayModeChanged(PlayMode newMode) {
                    System.out.println("播放模式改变: " + newMode.getDescription());
                }

                @Override
                public void onPlaylistUpdated() {
                    System.out.println("播放列表已更新，当前大小: " + finalPlayer.getPlaylist().size());
                }
            });

            // 创建测试文件列表
            List<File> testFiles = new ArrayList<>();
            for (int i = 1; i <= 6; i++) {
                File file = new File("test" + i + ".wav");
                if (file.exists()) {
                    testFiles.add(file);
                } else {
                    System.out.println("测试文件不存在: " + file.getAbsolutePath());
                }
            }

            if (testFiles.isEmpty()) {
                System.out.println("请创建test1.wav到test3.wav测试文件");
                return;
            }

            // 添加到播放列表
            System.out.println("添加测试文件到播放列表...");
            player.addToPlaylist(testFiles);

            /*
            // 测试顺序播放
            System.out.println("\n=== 测试顺序播放 ===");
            player.setPlayMode(PlayMode.NORMAL);
            player.play(testFiles.getFirst().getAbsolutePath());

            // 等待2秒
            Thread.sleep(2000);

            // 测试暂停/恢复
            System.out.println("\n=== 测试暂停/恢复 ===");
            player.pause();
            Thread.sleep(1000);
            player.resume();
            Thread.sleep(1000);

            // 测试音量控制
            System.out.println("\n=== 测试音量控制 ===");
            player.setVolume(0.5);
            Thread.sleep(500);
            player.increaseVolume(0.2);
            Thread.sleep(500);
            player.decreaseVolume(0.3);
            Thread.sleep(1000);

            // 测试跳转
            System.out.println("\n=== 测试跳转功能 ===");
            boolean seekResult = player.seekToTime(5.0);
            System.out.println("跳转结果: " + seekResult);
            Thread.sleep(2000);

            // 停止播放
            player.stop();

            // 测试单曲循环
            System.out.println("\n=== 测试单曲循环 ===");
            player.setPlayMode(PlayMode.REPEAT_ONE);
            player.play(testFiles.get(1).getAbsolutePath());
            Thread.sleep(3000);
            player.stop();

            // 测试顺序循环
            System.out.println("\n=== 测试顺序循环 ===");
            player.setPlayMode(PlayMode.REPEAT_ALL);
            player.play(testFiles.getFirst().getAbsolutePath());
            Thread.sleep(2000);
            player.stop();

             */


            player.setPlayMode(PlayMode.REPEAT_ALL);
            player.play(0);

            Thread.sleep(1000);

            player.nextTrack();

            IO.println(player.getAudioMetadata());

            Thread.sleep(1000);

            player.nextTrack();

            IO.println(player.getAudioMetadata());

            Thread.sleep(100000);

            // 显示统计信息
            System.out.println("\n=== 播放统计 ===");
            Map<String, Object> stats = player.getPlaybackStatistics();
            stats.forEach((k, v) -> System.out.println(k + ": " + v));

            // 显示音频元数据
            System.out.println("\n=== 音频元数据 ===");
            Map<String, Object> metadata = player.getAudioMetadata();
            metadata.forEach((k, v) -> System.out.println(k + ": " + v));

            // 测试支持的音频格式
            System.out.println("\n=== 支持的音频格式 ===");
            Set<String> supportedFormats = AdvancedStreamAudioPlayer.getSupportedFormats();
            System.out.println("支持的格式: " + String.join(", ", supportedFormats));

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