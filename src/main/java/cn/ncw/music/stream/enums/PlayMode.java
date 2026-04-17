package cn.ncw.music.stream.enums;

import lombok.Getter;

/**
 * 播放模式枚举
 */
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

    /**
     * 根据字符串获取播放模式
     */
    public static PlayMode fromString(String playModeString) {
        return switch (playModeString) {
            case "REPEAT_ONE" -> REPEAT_ONE;
            case "REPEAT_ALL" -> REPEAT_ALL;
            case "SHUFFLE" -> SHUFFLE;
            default -> NORMAL;
        };
    }
}
