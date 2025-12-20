package cn.ncw.music.stream;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 支持暂停/继续功能的流式音频播放器
 */
public class StreamAudioPlayer {

    // 播放状态常量
    public static final int STATE_PLAYING = 0;
    public static final int STATE_PAUSED = 1;
    public static final int STATE_STOPPED = 2;

    // 成员变量
    private String currentFilePath;
    private SourceDataLine sourceDataLine;
    private Thread playbackThread;
    private AudioInputStream audioStream;
    private long audioLength;
    private long currentPosition;
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

        if (!(playbackThread == null)) {
            if (playbackThread.isAlive()) {
                playbackThread.join();
            }
        }

        // 准备音频流
        currentFilePath = filePath;
        File audioFile = new File(currentFilePath);
        audioStream = AudioSystem.getAudioInputStream(audioFile);
        AudioFormat format = audioStream.getFormat();



        // 如果不支持此格式，则转换为标准PCM格式
        if (!isFormatSupported(format)) {
            format = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    format.getSampleRate(),
                    16,
                    format.getChannels(),
                    format.getChannels() * 2,
                    format.getSampleRate(),
                    false);
            audioStream = AudioSystem.getAudioInputStream(format, audioStream);
        }

        // 打开数据行
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
        }
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
     * 跳转到指定位置
     * @param positionMs 位置（毫秒）
     * @return 是否成功
     */
    public boolean seek(long positionMs) {
        if (audioStream == null || sourceDataLine == null) {
            return false;
        }

        try {
            // 停止当前播放
            if (sourceDataLine.isRunning()) {
                sourceDataLine.stop();
            }

            // 计算要跳转的字节位置
            AudioFormat format = audioStream.getFormat();
            long bytesPerMs = (long) (format.getFrameRate() * format.getFrameSize() / 1000);
            long targetBytePosition = positionMs * bytesPerMs;

            // 重置流到开始位置
            audioStream = AudioSystem.getAudioInputStream(new File(currentFilePath));

            // 跳过指定字节数
            long skipped = audioStream.skip(targetBytePosition);
            if (skipped != targetBytePosition) {
                return false;
            }

            currentPosition = positionMs;

            // 重新开始播放
            if (playing && !paused) {
                sourceDataLine.start();
            }

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取当前播放位置
     */
    public long getCurrentPosition() {
        return currentPosition;
    }

    /**
     * 获取音频总时长
     */
    public long getDuration() {
        return audioLength;
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
    // 修改streamPlayback方法以更新当前位置
    private void streamPlayback() {
        try {
            AudioFormat format = audioStream.getFormat();
            long bytesPerMs = (long) (format.getFrameRate() * format.getFrameSize() / 1000);

            sourceDataLine.start();
            int bufferSize = 4096;
            byte[] buffer = new byte[bufferSize];
            int bytesRead;

            while (playing) {
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

                bytesRead = audioStream.read(buffer, 0, buffer.length);
                if (bytesRead == -1) break;

                if (bytesRead > 0) {
                    sourceDataLine.write(buffer, 0, bytesRead);
                    currentPosition += bytesRead / bytesPerMs;
                }
            }

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
        double volume0 = player.getVolume();
        System.out.println(volume0);

        TimeUnit.MILLISECONDS.sleep(5000);

        // 设置音量为50%
        player.setVolume(0.5);
        double volume1 = player.getVolume();
        System.out.println(volume1);

        TimeUnit.MILLISECONDS.sleep(5000);

        // 增加20%音量
        player.increaseVolume(0.2);
        double volume2 = player.getVolume();
        System.out.println(volume2);

        TimeUnit.MILLISECONDS.sleep(5000);

        for (int i = 0; i < 10; i++) {
            player.decreaseVolume(0.05);
            TimeUnit.MILLISECONDS.sleep(1000);
            System.out.println(player.getVolume());
        }

        // 检查是否支持音量控制
        if (player.isVolumeSupported()) {
            System.out.println(player.getVolumeInfo());
        }

        TimeUnit.MILLISECONDS.sleep(114514);
    }

}