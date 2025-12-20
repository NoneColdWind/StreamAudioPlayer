package cn.ncw.music.stream;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 支持暂停/继续功能和位置调节的流式音频播放器
 */
public class StreamAudioPlayer {

    // 播放状态常量
    public static final int STATE_PLAYING = 0;
    public static final int STATE_PAUSED = 1;
    public static final int STATE_STOPPED = 2;

    // 成员变量
    private SourceDataLine sourceDataLine;
    private Thread playbackThread;
    private AudioInputStream audioStream;
    private volatile boolean playing;
    private volatile boolean paused;
    private volatile int playbackState;
    private final Object playLock = new Object();

    // 音量控制相关
    private FloatControl volumeControl;
    private double currentVolume = 0.8; // 默认音量 80%
    private float minVolume;
    private float maxVolume;
    private boolean volumeSupported = false;

    // 位置控制相关
    private File audioFile;
    private AudioFormat originalFormat;
    private long totalFrames; // 总帧数
    private long currentFrame; // 当前帧位置
    private int frameSize; // 每帧的字节数
    private boolean positionSupported = true;

    public StreamAudioPlayer() {

    }

    /**
     * 播放指定的WAV文件
     *
     * @param filePath WAV文件路径
     * @throws UnsupportedAudioFileException 如果文件格式不支持
     * @throws IOException 如果文件读取错误
     * @throws LineUnavailableException 如果音频设备不可用
     */
    public void play(String filePath)
            throws UnsupportedAudioFileException, IOException, LineUnavailableException, InterruptedException {

        // 停止当前播放
        stop();

        // 准备音频流
        audioFile = new File(filePath);
        audioStream = AudioSystem.getAudioInputStream(audioFile);
        originalFormat = audioStream.getFormat();

        // 计算总帧数和帧大小
        totalFrames = audioStream.getFrameLength();
        frameSize = originalFormat.getFrameSize();

        // 重置当前位置
        currentFrame = 0;

        // 如果不支持此格式，则转换为标准PCM格式
        if (!isFormatSupported(originalFormat)) {
            AudioFormat targetFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    originalFormat.getSampleRate(),
                    16,
                    originalFormat.getChannels(),
                    originalFormat.getChannels() * 2,
                    originalFormat.getSampleRate(),
                    false);
            audioStream = AudioSystem.getAudioInputStream(targetFormat, audioStream);
            // 更新帧信息
            frameSize = targetFormat.getFrameSize();
            // 重新计算总帧数（转换后可能不同）
            totalFrames = audioStream.getFrameLength();
        }

        // 打开数据行
        AudioFormat format = audioStream.getFormat();
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        sourceDataLine = (SourceDataLine) AudioSystem.getLine(info);
        sourceDataLine.open(format);

        // 初始化音量控制
        initVolumeControl();

        // 设置初始状态
        playing = true;
        paused = false;
        playbackState = STATE_PLAYING;

        // 启动播放线程
        playbackThread = new Thread(this::streamPlayback);
        playbackThread.setDaemon(true);
        playbackThread.start();
    }

    /**
     * 暂停播放
     */
    public void pause() {
        if (sourceDataLine != null && sourceDataLine.isRunning()) {
            synchronized (playLock) {
                sourceDataLine.stop();
                paused = true;
                playbackState = STATE_PAUSED;
            }
        }
    }

    /**
     * 继续播放
     */
    public void resume() {
        if (sourceDataLine != null && !sourceDataLine.isRunning() && paused) {
            synchronized (playLock) {
                sourceDataLine.start();
                paused = false;
                playbackState = STATE_PLAYING;
                playLock.notifyAll(); // 唤醒播放线程
            }
        }
    }

    /**
     * 停止播放并释放资源
     */
    public void stop() {
        if (sourceDataLine != null) {
            playing = false;
            paused = false;

            // 唤醒可能处于等待状态的播放线程
            synchronized (playLock) {
                playLock.notifyAll();
            }

            // 等待播放线程结束
            try {
                if (playbackThread != null && playbackThread.isAlive()) {
                    playbackThread.join(500);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // 关闭资源
            closeResources();
            playbackState = STATE_STOPPED;
            currentFrame = 0;
        }
    }

    /**
     * 跳转到指定位置（基于时间）
     *
     * @param seconds 跳转到的位置（秒）
     * @return 是否跳转成功
     */
    public boolean seekToTime(double seconds) {
        if (!positionSupported || audioStream == null) {
            return false;
        }

        float frameRate = originalFormat.getFrameRate();
        long targetFrame = (long) (seconds * frameRate);
        return seekToFrame(targetFrame);
    }

    /**
     * 跳转到指定帧位置
     *
     * @param frame 目标帧位置
     * @return 是否跳转成功
     */
    public boolean seekToFrame(long frame) {
        if (!positionSupported || audioFile == null || frame < 0 || frame >= totalFrames) {
            return false;
        }

        // 保存当前状态
        boolean wasPlaying = (playbackState == STATE_PLAYING);

        // 暂停播放
        if (wasPlaying) {
            pause();
        }

        try {
            // 关闭当前流
            if (audioStream != null) {
                audioStream.close();
            }

            // 重新打开音频流并跳转到指定位置
            audioStream = AudioSystem.getAudioInputStream(audioFile);

            // 如果需要格式转换
            if (!isFormatSupported(originalFormat)) {
                AudioFormat targetFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        originalFormat.getSampleRate(),
                        16,
                        originalFormat.getChannels(),
                        originalFormat.getChannels() * 2,
                        originalFormat.getSampleRate(),
                        false);
                audioStream = AudioSystem.getAudioInputStream(targetFormat, audioStream);
            }

            // 跳过指定数量的帧
            long bytesToSkip = frame * frameSize;
            long skipped = audioStream.skip(bytesToSkip);

            if (skipped != bytesToSkip) {
                System.out.println("跳转不精确: 期望 " + bytesToSkip + " 字节, 实际跳过 " + skipped + " 字节");
            }

            currentFrame = frame;

            // 如果之前正在播放，则恢复播放
            if (wasPlaying) {
                resume();
            }

            return true;
        } catch (Exception e) {
            System.out.println("跳转失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 向前跳转指定时间
     *
     * @param seconds 跳转的时间（秒）
     * @return 是否跳转成功
     */
    public boolean seekForward(double seconds) {
        if (!positionSupported) {
            return false;
        }

        float frameRate = originalFormat.getFrameRate();
        long framesToSkip = (long) (seconds * frameRate);
        long targetFrame = Math.min(currentFrame + framesToSkip, totalFrames - 1);

        return seekToFrame(targetFrame);
    }

    /**
     * 向后跳转指定时间
     *
     * @param seconds 跳转的时间（秒）
     * @return 是否跳转成功
     */
    public boolean seekBackward(double seconds) {
        if (!positionSupported) {
            return false;
        }

        float frameRate = originalFormat.getFrameRate();
        long framesToSkip = (long) (seconds * frameRate);
        long targetFrame = Math.max(0, currentFrame - framesToSkip);

        return seekToFrame(targetFrame);
    }

    /**
     * 获取当前播放位置（秒）
     *
     * @return 当前播放时间（秒）
     */
    public double getCurrentTime() {
        if (!positionSupported || originalFormat == null) {
            return 0;
        }

        return currentFrame / originalFormat.getFrameRate();
    }

    /**
     * 获取音频总时长（秒）
     *
     * @return 总时长（秒）
     */
    public double getTotalTime() {
        if (!positionSupported || originalFormat == null) {
            return 0;
        }

        return totalFrames / originalFormat.getFrameRate();
    }

    /**
     * 获取当前播放位置（百分比）
     *
     * @return 播放进度百分比（0.0 - 1.0）
     */
    public double getPlaybackProgress() {
        if (!positionSupported || totalFrames == 0) {
            return 0;
        }

        return (double) currentFrame / totalFrames;
    }

    /**
     * 获取当前帧位置
     *
     * @return 当前帧数
     */
    public long getCurrentFrame() {
        return currentFrame;
    }

    /**
     * 获取总帧数
     *
     * @return 总帧数
     */
    public long getTotalFrames() {
        return totalFrames;
    }

    /**
     * 检查是否支持位置控制
     *
     * @return 如果支持位置控制返回true
     */
    public boolean isPositionSupported() {
        return positionSupported;
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
     * 获取当前播放状态
     *
     * @return 播放状态（STATE_PLAYING/STATE_PAUSED/STATE_STOPPED）
     */
    public int getPlaybackState() {
        return playbackState;
    }

    /**
     * 初始化音量控制
     */
    private void initVolumeControl() {
        try {
            // 尝试获取主音量控制
            volumeControl = (FloatControl) sourceDataLine.getControl(FloatControl.Type.MASTER_GAIN);
            volumeSupported = true;
            minVolume = volumeControl.getMinimum();
            maxVolume = volumeControl.getMaximum();

            // 设置默认音量
            setVolume(currentVolume);
        } catch (IllegalArgumentException e) {
            // 如果不支持MASTER_GAIN，尝试VOLUME控制
            try {
                volumeControl = (FloatControl) sourceDataLine.getControl(FloatControl.Type.VOLUME);
                volumeSupported = true;
                minVolume = volumeControl.getMinimum();
                maxVolume = volumeControl.getMaximum();
                setVolume(currentVolume);
            } catch (IllegalArgumentException ex) {
                // 两种音量控制都不支持
                volumeSupported = false;
                System.out.println("警告：当前音频设备不支持音量控制");
            }
        }
    }

    /**
     * 设置音量
     *
     * @param volume 音量值，范围0.0-1.0（0% - 100%）
     * @return 是否设置成功
     */
    public boolean setVolume(double volume) {
        if (!volumeSupported || volumeControl == null) {
            return false;
        }

        // 确保音量在有效范围内
        currentVolume = Math.max(0.0f, Math.min(1.0f, volume));

        try {
            if (volumeControl.getType() == FloatControl.Type.MASTER_GAIN) {
                // MASTER_GAIN使用分贝(dB)表示，需要将线性音量转换为分贝
                float dB = (float) (Math.log10(currentVolume == 0 ? 0.0001 : currentVolume) * 20.0);
                dB = Math.max(minVolume, Math.min(maxVolume, dB));
                volumeControl.setValue(dB);
            } else if (volumeControl.getType() == FloatControl.Type.VOLUME) {
                // VOLUME使用线性标度
                float linearVolume = (float) (minVolume + (maxVolume - minVolume) * currentVolume);
                volumeControl.setValue(linearVolume);
            }
            return true;
        } catch (IllegalArgumentException e) {
            System.out.println("音量设置失败: " + e.getMessage());
            return false;
        }
    }

    public boolean setMute() {
        return setVolume(0.0);
    }

    /**
     * 获取当前音量
     *
     * @return 当前音量值，范围0.0-1.0
     */
    public double getVolume() {
        return currentVolume;
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
     * 检查是否支持音量控制
     *
     * @return 如果支持音量控制返回true
     */
    public boolean isVolumeSupported() {
        return volumeSupported;
    }

    /**
     * 获取音量范围信息
     *
     * @return 包含最小、最大和当前音量的字符串，如果不支持则返回null
     */
    public String getVolumeInfo() {
        if (!volumeSupported || volumeControl == null) {
            return null;
        }
        return String.format("音量范围: %.1f - %.1f, 当前音量: %.1f%%",
                minVolume, maxVolume, currentVolume * 100);
    }

    /**
     * 获取音频流的格式信息（播放前不可用）
     *
     * @return 音频格式对象，如果没有音频流则返回null
     */
    public AudioFormat getAudioFormat() {
        return sourceDataLine != null ? sourceDataLine.getFormat() : null;
    }

    // 流式播放核心逻辑
    private void streamPlayback() {
        try {
            sourceDataLine.start();
            int bufferSize = 4096; // 4KB缓冲区
            byte[] buffer = new byte[bufferSize];
            int bytesRead;

            // 播放循环
            while (playing) {
                // 当暂停时，等待
                synchronized (playLock) {
                    while (paused) {
                        try {
                            playLock.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }

                // 读取数据
                bytesRead = audioStream.read(buffer, 0, buffer.length);

                if (bytesRead == -1) {
                    break; // 文件结束
                }

                // 写入数据行
                if (bytesRead > 0) {
                    sourceDataLine.write(buffer, 0, bytesRead);
                    // 更新当前位置（基于读取的字节数计算帧数）
                    int framesRead = bytesRead / frameSize;
                    currentFrame += framesRead;
                }
            }

            // 播放完成，释放资源
            sourceDataLine.drain();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (playing) {
                closeResources();
            }
            playbackState = STATE_STOPPED;
        }
    }

    // 关闭音频资源
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
            e.printStackTrace();
        }
    }

    // 检查格式是否支持
    private boolean isFormatSupported(AudioFormat format) {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        return AudioSystem.isLineSupported(info);
    }

    public static void main(String[] args) throws UnsupportedAudioFileException, LineUnavailableException, IOException, InterruptedException {
        StreamAudioPlayer player = new StreamAudioPlayer();
        player.play("music1.wav");

        // 显示音频信息
        System.out.println("音频信息: " + player.getPositionInfo());

        TimeUnit.MILLISECONDS.sleep(2000);

        // 跳转到30秒位置
        System.out.println("跳转到30秒位置");
        player.seekToTime(30.0);
        System.out.println("跳转后: " + player.getPositionInfo());

        TimeUnit.MILLISECONDS.sleep(3000);

        // 向前跳转10秒
        System.out.println("向前跳转10秒");
        player.seekForward(10.0);
        System.out.println("跳转后: " + player.getPositionInfo());

        TimeUnit.MILLISECONDS.sleep(3000);

        // 向后跳转5秒
        System.out.println("向后跳转5秒");
        player.seekBackward(5.0);
        System.out.println("跳转后: " + player.getPositionInfo());

        // 播放过程中显示进度
        for (int i = 0; i < 10; i++) {
            TimeUnit.MILLISECONDS.sleep(1000);
            System.out.printf("播放进度: %.1f%%\n", player.getPlaybackProgress() * 100);
        }

        // 检查是否支持位置控制
        if (player.isPositionSupported()) {
            System.out.println("位置控制功能已启用");
        }

        TimeUnit.MILLISECONDS.sleep(5000);
        player.stop();
    }
}