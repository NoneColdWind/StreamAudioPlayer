package cn.ncw.music.exception;

/**
 * 音频播放器异常类
 */
public class AudioPlayerException extends RuntimeException {
    public AudioPlayerException() {
        super();
    }

    public AudioPlayerException(String message) {
        super(message);
    }

    public AudioPlayerException(String message, Throwable cause) {
        super(message, cause);
    }

    public AudioPlayerException(Throwable cause) {
        super(cause);
    }
}
