package cn.ncw.music.stream.manager;

import cn.ncw.music.stream.enums.PlayMode;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/**
 * 播放列表管理器
 */
public class PlaylistManager {
    private final List<File> playlist = new CopyOnWriteArrayList<>();
    private final AtomicInteger currentIndex = new AtomicInteger(0);
    private volatile PlayMode playMode = PlayMode.NORMAL;
    private List<Integer> shuffleIndices = Collections.emptyList();
    private final AtomicBoolean shuffleGenerated = new AtomicBoolean(false);
    private final Object shuffleLock = new Object();
    private final Predicate<File> formatSupportChecker;

    public PlaylistManager(Predicate<File> formatSupportChecker) {
        this.formatSupportChecker = formatSupportChecker;
    }

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

    public PlayMode getPlayMode() {
        return playMode;
    }

    public void setPlayMode(PlayMode mode) {
        this.playMode = mode;
        if (mode == PlayMode.SHUFFLE) {
            generateShuffleList();
        }
    }

    public boolean addToPlaylist(File file) {
        if (!formatSupportChecker.test(file)) {
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
                .filter(formatSupportChecker)
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
