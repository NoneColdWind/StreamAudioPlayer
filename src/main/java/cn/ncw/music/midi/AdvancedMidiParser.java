package cn.ncw.music.midi;

import javax.sound.midi.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.*;

/**
 * 高级MIDI解析器：生成包含音符时长、和弦和节奏变化的乐谱
 */
public class AdvancedMidiParser {

    // 时值符号定义（符合标准音乐符号）
    private static final String WHOLE = "w";      // 全音符
    private static final String HALF = "h";        // 二分音符
    private static final String QUARTER = "q";     // 四分音符
    private static final String EIGHTH = "e";      // 八分音符
    private static final String SIXTEENTH = "s";   // 十六分音符
    private static final String THIRTY_SECOND = "t";// 三十二分音符
    private static final String SIXTY_FOURTH = "x";// 六十四分音符

    public static void main(String[] args) {

        try {
            generateMusicScore("input.mid", "output.txt");
        } catch (Exception e) {
            System.err.println("转换失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 生成音乐乐谱
     *
     * @param midiPath   MIDI文件路径
     * @param outputPath 输出文件路径
     */
    public static void generateMusicScore(String midiPath, String outputPath)
            throws InvalidMidiDataException, IOException {
        // 加载MIDI序列
        Sequence sequence = MidiSystem.getSequence(new File(midiPath));
        int resolution = sequence.getResolution(); // PPQ（每四分音符脉冲数）

        // 解析全局信息
        GlobalMusicInfo globalInfo = parseGlobalInfo(sequence, resolution);

        // 处理所有音轨
        List<String> scoreLines = new ArrayList<>();
        scoreLines.add("// =================== 音乐乐谱 ===================");
        scoreLines.add("// 标题: " + globalInfo.title);
        scoreLines.add("// 作曲家: " + globalInfo.composer);
        scoreLines.add("// 速度: " + globalInfo.bpm + " BPM");
        scoreLines.add("// 拍号: " + globalInfo.timeSignature);
        scoreLines.add("// 调号: " + globalInfo.keySignature);
        scoreLines.add("// 解析精度: 1/" + resolution + " tick");
        scoreLines.add("// 格式说明: <MIDI编号>_<时值> (例: 60_q = 中央C四分音符)");
        scoreLines.add("// 和弦格式: [音符1_时值, 音符2_时值]");
        scoreLines.add("// 特殊时值: . = 附点, t = 三连音");
        scoreLines.add("// ================================================\n");

        // 检测并记录节奏变化
        scoreLines.add("// 节拍变化:");
        for (TempoChange change : globalInfo.tempoChanges) {
            scoreLines.add(String.format("//   - 第 %.2f 拍: %d BPM",
                    change.position, (int)change.bpm));
        }
        scoreLines.add("");

        // 解析每个音轨
        Track[] tracks = sequence.getTracks();
        for (int i = 0; i < tracks.length; i++) {
            TrackData trackData = parseTrack(tracks[i], resolution, globalInfo);

            if (!trackData.events.isEmpty()) {
                scoreLines.add(String.format("=== 音轨 %d: %s ===",
                        i + 1, trackData.instrument));
                scoreLines.add("// 乐段: " + trackData.phraseStructure);
                scoreLines.addAll(formatTrack(trackData, globalInfo));
                scoreLines.add("");
            }
        }

        // 写入文件
        Files.write(Path.of(outputPath), scoreLines);
    }

    /**
     * 解析全局音乐信息
     */
    private static GlobalMusicInfo parseGlobalInfo(Sequence sequence, int resolution) {
        GlobalMusicInfo info = new GlobalMusicInfo();
        info.resolution = resolution;
        info.bpm = 120.0; // 默认BPM
        info.timeSignature = "4/4";
        info.keySignature = "C";

        for (Track track : sequence.getTracks()) {
            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                MidiMessage msg = event.getMessage();

                if (msg instanceof MetaMessage metaMsg) {
                    try {
                        byte[] data = metaMsg.getData();
                        switch (metaMsg.getType()) {
                            case 0x03: // 曲目标题
                                info.title = new String(data);
                                break;
                            case 0x04: // 乐器名称/作曲家
                                info.composer = new String(data);
                                break;
                            case 0x51: // 速度事件
                                if (data.length >= 3) {
                                    int microseconds = ((data[0] & 0xFF) << 16) |
                                            ((data[1] & 0xFF) << 8) |
                                            (data[2] & 0xFF);
                                    double bpm = 60_000_000.0 / microseconds;

                                    // 记录节奏变化
                                    double position = (double)event.getTick() / resolution;
                                    info.tempoChanges.add(new TempoChange(position, bpm));

                                    // 设置初始速度
                                    if (event.getTick() == 0) {
                                        info.bpm = bpm;
                                    }
                                }
                                break;
                            case 0x58: // 拍号事件
                                if (data.length >= 4) {
                                    int numerator = data[0];
                                    int denominator = (int)Math.pow(2, data[1]);
                                    info.timeSignature = numerator + "/" + denominator;
                                }
                                break;
                            case 0x59: // 调号事件
                                if (data.length >= 2) {
                                    int key = data[0];
                                    String[] keyNames = {"C", "G", "D", "A", "E", "B", "F#", "C#",
                                            "F", "Bb", "Eb", "Ab", "Db", "Gb", "Cb"};
                                    info.keySignature = keyNames[(key + 7) % 15];
                                }
                                break;
                        }
                    } catch (Exception e) {
                        // 忽略解析错误的元信息
                    }
                }
            }
        }

        // 默认值设置
        if (info.title == null || info.title.isEmpty())
            info.title = "未命名乐曲";
        if (info.composer == null || info.composer.isEmpty())
            info.composer = "未知作曲家";

        return info;
    }

    /**
     * 解析单条音轨数据
     */
    private static TrackData parseTrack(Track track, int resolution, GlobalMusicInfo globalInfo) {
        TrackData trackData = new TrackData();
        Map<Integer, NoteEvent> activeNotes = new HashMap<>();
        Map<Long, Set<Integer>> chordMap = new TreeMap<>();
        long lastNoteTime = 0;
        int highestNote = 0;
        int lowestNote = 127;

        // 第一遍：检测音符和和弦
        for (int i = 0; i < track.size(); i++) {
            MidiEvent event = track.get(i);
            MidiMessage msg = event.getMessage();
            long tick = event.getTick();

            if (msg instanceof ShortMessage shortMsg) {
                int command = shortMsg.getCommand();
                int note = shortMsg.getData1();
                int velocity = shortMsg.getData2();

                // 检测乐器信息
                if (command == ShortMessage.PROGRAM_CHANGE) {
                    trackData.instrument = getInstrumentName(shortMsg.getData1());
                }

                // 处理音符开始
                else if (command == ShortMessage.NOTE_ON && velocity > 0) {
                    activeNotes.put(note, new NoteEvent(note, tick, velocity));
                    chordMap.computeIfAbsent(tick, k -> new HashSet<>()).add(note);
                    lastNoteTime = Math.max(lastNoteTime, tick);

                    // 更新音域范围
                    if (note > highestNote) highestNote = note;
                    if (note < lowestNote) lowestNote = note;
                }

                // 处理音符结束
                else if (command == ShortMessage.NOTE_OFF ||
                        (command == ShortMessage.NOTE_ON && velocity == 0)) {
                    if (activeNotes.containsKey(note)) {
                        NoteEvent startEvent = activeNotes.get(note);
                        long duration = tick - startEvent.startTick;
                        startEvent.duration = duration;
                        startEvent.durationType = calculateDurationType(duration, resolution);
                        trackData.events.add(startEvent);
                        activeNotes.remove(note);
                    }
                }
            }
        }

        // 第二遍：检测音乐结构
        detectPhrasing(trackData, globalInfo);
        detectTuplets(trackData, resolution);
        detectRhythmicPatterns(trackData);

        // 设置音轨信息
        trackData.range = highestNote - lowestNote;
        trackData.density = (double)trackData.events.size() / (lastNoteTime == 0 ? 1 : lastNoteTime);

        // 排序事件
        trackData.events.sort(Comparator.comparingLong(e -> e.startTick));
        return trackData;
    }

    /**
     * 格式化音轨输出
     */
    private static List<String> formatTrack(TrackData trackData, GlobalMusicInfo globalInfo) {
        List<String> lines = new ArrayList<>();
        int measure = 1;
        double measureLength = globalInfo.getBeatsPerMeasure() * globalInfo.resolution;
        long measureStart = 0;
        long measureEnd = (long)measureLength;
        StringBuilder currentMeasure = new StringBuilder("【" + measure + "】 ");

        // 检测和弦分组
        Map<Long, List<NoteEvent>> chordGroups = groupChords(trackData.events);

        // 格式化事件
        for (NoteEvent event : trackData.events) {
            // 检查是否需要新小节
            if (event.startTick >= measureEnd) {
                lines.add(currentMeasure.toString());
                currentMeasure = new StringBuilder();

                // 更新小节计数
                measure++;
                measureStart = measureEnd;
                measureEnd = measureStart + (long)measureLength;

                // 添加新小节标记
                currentMeasure.append("【").append(measure).append("】 ");
            }

            // 处理当前事件
            if (chordGroups.containsKey(event.startTick) && chordGroups.get(event.startTick).size() > 1) {
                // 处理整个和弦组
                List<NoteEvent> chordNotes = chordGroups.get(event.startTick);
                currentMeasure.append("[");
                for (int i = 0; i < chordNotes.size(); i++) {
                    if (i > 0) currentMeasure.append(",");
                    formatNote(chordNotes.get(i), currentMeasure);
                }
                currentMeasure.append("] ");
            } else {
                // 处理单个音符
                formatNote(event, currentMeasure);
            }
        }

        // 添加最后一小节
        if (!currentMeasure.toString().equals("【" + measure + "】 ")) {
            lines.add(currentMeasure.toString());
        }

        return lines;
    }

    // 格式化单个音符输出
    private static void formatNote(NoteEvent note, StringBuilder sb) {
        // 基本音符信息
        sb.append(note.note);

        // 添加特殊节奏标记
        if (note.isTuplet) {
            sb.append("t");
        }

        // 添加强度标记
        if (note.velocity > 100) sb.append("f");   // 强音
        if (note.velocity < 50) sb.append("p");     // 弱音

        // 添加时值信息
        sb.append("_").append(note.durationType);

        // 添加附点
        if (note.hasDot) {
            sb.append(".");
        }

        sb.append(" ");
    }

    // 分组和弦音符
    private static Map<Long, List<NoteEvent>> groupChords(List<NoteEvent> events) {
        Map<Long, List<NoteEvent>> chordGroups = new HashMap<>();
        Set<Long> processed = new HashSet<>();

        for (int i = 0; i < events.size(); i++) {
            NoteEvent event = events.get(i);
            long tick = event.startTick;

            if (!processed.contains(tick)) {
                List<NoteEvent> chord = new ArrayList<>();
                for (int j = i; j < events.size(); j++) {
                    NoteEvent next = events.get(j);
                    if (next.startTick == tick) {
                        chord.add(next);
                    } else {
                        break;
                    }
                }

                if (chord.size() > 1) {
                    chordGroups.put(tick, chord);
                    processed.add(tick);
                }
            }
        }

        return chordGroups;
    }

    // ========================== 高级音乐分析算法 ==========================

    /**
     * 计算时值类型（带精度容差）
     */
    private static String calculateDurationType(long durationTicks, int resolution) {
        double quarterLength = resolution;
        double ratio = (double)durationTicks / quarterLength;

        // 允许时值容差（百分比）
        double tolerance = 0.15; // 15%容差

        // 检查时值类型
        if (isWithin(ratio, 4.0, tolerance)) return WHOLE;
        if (isWithin(ratio, 3.0, tolerance)) return WHOLE + "."; // 附点二分音符
        if (isWithin(ratio, 2.0, tolerance)) return HALF;
        if (isWithin(ratio, 1.5, tolerance)) return HALF + "."; // 附点四分音符
        if (isWithin(ratio, 1.0, tolerance)) return QUARTER;
        if (isWithin(ratio, 0.75, tolerance)) return QUARTER + "."; // 附点八分音符
        if (isWithin(ratio, 0.5, tolerance)) return EIGHTH;
        if (isWithin(ratio, 0.375, tolerance)) return EIGHTH + "."; // 附点十六分音符
        if (isWithin(ratio, 0.25, tolerance)) return SIXTEENTH;
        if (isWithin(ratio, 0.125, tolerance)) return THIRTY_SECOND;
        if (isWithin(ratio, 0.0625, tolerance)) return SIXTY_FOURTH;

        // 不规则的时值
        DecimalFormat df = new DecimalFormat("0.##");
        return "(" + df.format(ratio) + ")";
    }

    // 容差检测方法
    private static boolean isWithin(double value, double target, double tolerance) {
        double min = target * (1 - tolerance);
        double max = target * (1 + tolerance);
        return value >= min && value <= max;
    }

    /**
     * 检测三连音
     */
    private static void detectTuplets(TrackData trackData, int resolution) {
        double eighthDuration = resolution / 2.0; // 八分音符的tick数
        double tripleTolerance = eighthDuration * 0.3; // 30%容差

        // 搜索可能的节奏组
        for (int i = 0; i < trackData.events.size() - 2; i++) {
            NoteEvent e1 = trackData.events.get(i);
            NoteEvent e2 = trackData.events.get(i+1);
            NoteEvent e3 = trackData.events.get(i+2);

            // 检查是否在相同时间范围内
            double groupStart = e1.startTick;
            double groupEnd = e3.startTick + e3.duration;
            double groupDuration = groupEnd - groupStart;

            // 典型的三连音占用三分之二的节奏单位
            if (Math.abs(groupDuration - (2 * eighthDuration)) < tripleTolerance &&
                    // 检查三个音符是否大致等距
                    Math.abs(e2.startTick - e1.startTick - eighthDuration/3) < tripleTolerance &&
                    Math.abs(e3.startTick - e2.startTick - eighthDuration/3) < tripleTolerance) {

                e1.isTuplet = true;
                e2.isTuplet = true;
                e3.isTuplet = true;
                i += 2; // 跳过已标记的音符
            }
        }
    }

    /**
     * 检测乐句结构
     */
    private static void detectPhrasing(TrackData trackData, GlobalMusicInfo globalInfo) {
        if (trackData.events.isEmpty()) return;

        // 排序事件
        trackData.events.sort(Comparator.comparingLong(e -> e.startTick));

        // 检测小节分隔
        long measureDuration = (long)(globalInfo.getBeatsPerMeasure() * globalInfo.resolution);
        long lastEventTime = trackData.events.get(trackData.events.size()-1).startTick;
        int measureCount = (int)Math.ceil((double)lastEventTime / measureDuration);

        // 检测乐句（通常4或8小节）
        int phraseLength = 4; // 默认4小节一个乐句
        StringBuilder structure = new StringBuilder();
        int measure = 0;

        for (int i = 0; i < measureCount; i++) {
            // 检测小节结束时的终止感
            long measureEnd = (i+1) * measureDuration;
            boolean hasCadence = false;

            // 检查最后两个音符是否构成终止
            for (int j = trackData.events.size()-1; j >= 0; j--) {
                NoteEvent event = trackData.events.get(j);
                if (event.startTick < measureEnd) {
                    if (j >= 1) {
                        NoteEvent prev = trackData.events.get(j-1);
                        // 简单的终止模式检测（从高音下行到主音）
                        if (prev.note > event.note && event.note % 12 == 0) {
                            hasCadence = true;
                        }
                    }
                    break;
                }
            }

            // 标记乐句结构
            if (hasCadence) {
                structure.append("A");
                phraseLength = i+1 - measure;
                measure = i+1;
            }
        }

        trackData.phraseStructure = structure.toString();
        if (trackData.phraseStructure.isEmpty()) {
            trackData.phraseStructure = "主旋律";
        }
    }

    /**
     * 检测节奏模式
     */
    private static void detectRhythmicPatterns(TrackData trackData) {
        // 此方法实现节奏模式检测（如拉丁节奏、摇滚节奏等）
        // 简单实现：计算强拍上的音符比例

        int strongBeatNotes = 0;
        int totalNotes = trackData.events.size();

        for (NoteEvent event : trackData.events) {
            // 检查是否是强拍位置（每小节的第一拍）
            if (event.startTick % 480 == 0) {
                strongBeatNotes++;
            }
        }

        if (strongBeatNotes > totalNotes * 0.4) {
            trackData.rhythmType = "进行曲节奏";
        } else if (strongBeatNotes < totalNotes * 0.2) {
            trackData.rhythmType = "摇摆节奏";
        } else {
            trackData.rhythmType = "标准节奏";
        }
    }

    // ========================== 辅助方法和类 ==========================

    private static String getInstrumentName(int program) {
        String[] instruments = {
                "钢琴", "明亮钢琴", "电钢琴", "大键琴",
                "电风琴", "摇滚风琴", "教堂管风琴", "簧风琴",
                "口琴", "手风琴", "古典吉他", "钢弦吉他",
                "爵士电吉他", "清音吉他", "失真吉他", "和声吉他",
                "贝斯", "指弹贝斯", "电子贝斯", "无品贝斯",
                "小提琴", "中提琴", "大提琴", "低音提琴",
                "弦乐", "颤音弦乐", "拨弦弦乐", "竖琴",
                "小号", "长号", "大号", "弱音小号",
                "法国号", "铜管", "合成铜管", "中音萨克斯",
                "次中音萨克斯", "上低音萨克斯", "双簧管", "英国管",
                "巴松管", "单簧管", "短笛", "长笛",
                "竖笛", "排箫", "合成主音", "合成背景音",
                "合成效果", "民族乐器", "打击乐", "音效"
        };

        return instruments[program % instruments.length];
    }

    // ========================== 内部类定义 ==========================

    /**
     * 全局音乐信息
     */
    static class GlobalMusicInfo {
        String title = "";
        String composer = "";
        double bpm = 120.0;
        String timeSignature = "4/4";
        String keySignature = "C";
        int resolution = 480;
        List<TempoChange> tempoChanges = new ArrayList<>();

        public double getBeatsPerMeasure() {
            return Double.parseDouble(timeSignature.split("/")[0]);
        }
    }

    /**
     * 节奏变化点
     */
    static class TempoChange {
        double position; // 在音乐中的位置（拍数）
        double bpm;

        TempoChange(double position, double bpm) {
            this.position = position;
            this.bpm = bpm;
        }
    }

    /**
     * 音轨数据
     */
    static class TrackData {
        String instrument = "未知乐器";
        List<NoteEvent> events = new ArrayList<>();
        int range = 0;
        double density = 0.0;
        String phraseStructure = "";
        String rhythmType = "";
    }

    /**
     * 音符事件
     */
    static class NoteEvent {
        int note;            // MIDI音符编号
        long startTick;      // 开始时间（tick）
        long duration;       // 持续时间（tick）
        int velocity;        // 音符力度
        String durationType; // 时值类型（q, e, s...）
        boolean hasDot;      // 是否有附点
        boolean isTuplet;    // 是否在三连音中

        NoteEvent(int note, long startTick, int velocity) {
            this.note = note;
            this.startTick = startTick;
            this.velocity = velocity;
        }

        NoteEvent(int note, long startTick, int velocity, long duration) {
            this(note, startTick, velocity);
            this.duration = duration;
        }
    }
}
