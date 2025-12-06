package cn.ncw.music.midi;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;

public class MidiPlayer {

    private static Synthesizer synth;
    private static MidiChannel[] channels;

    // 常用音色常量
    public static final int PIANO = 0;
    public static final int MARIMBA = 12;
    public static final int ORGAN = 16;
    public static final int GUITAR = 25;
    public static final int BASS = 33;
    public static final int VIOLIN = 40;
    public static final int TRUMPET = 56;
    public static final int FLUTE = 73;

    // 初始化MIDI合成器
    static {
        try {
            synth = MidiSystem.getSynthesizer();
            synth.open();
            channels = synth.getChannels();
        } catch (MidiUnavailableException e) {
            e.printStackTrace();
        }
    }

    /**
     * 播放和弦
     * @param notes 音符数组（MIDI编号）
     * @param duration 持续时间（毫秒）
     * @param instrument 音色编号（0-127）
     */
    public static void playChord(int[] notes, int velocity, int duration, int instrument) {
        playChord(notes, velocity, duration, instrument, 0);
    }

    /**
     * 播放和弦（指定通道）
     * @param notes 音符数组
     * @param duration 持续时间
     * @param instrument 音色编号
     * @param channel 通道编号（0-15）
     */
    public static void playChord(int[] notes, int velocity, int duration, int instrument, int channel) {
        if (notes == null || notes.length == 0) return;

        try {
            // 设置通道音色
            channels[channel].programChange(instrument);

            // 同时播放所有音符（形成和弦）
            for (int note : notes) {
                channels[channel].noteOn(note, velocity); // 音量80（0-127）
            }

            // 持续播放
            Thread.sleep(duration);

            // 停止所有音符
            for (int note : notes) {
                channels[channel].noteOff(note);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 播放单音
     * @param note MIDI音符编号
     * @param duration 持续时间(毫秒)
     * @param instrument 音色编号
     * @param velocity 音量(0-127)
     */
    public static void playNote(int note, int velocity, int duration, int instrument) {
        playNote(note, duration, instrument, velocity, 0);
    }

    /**
     * 播放单音(指定通道)
     * @param note MIDI音符编号
     * @param duration 持续时间(毫秒)
     * @param instrument 音色编号
     * @param velocity 音量(0-127)
     * @param channel 通道编号(0-15)
     */
    public static void playNote(int note, int velocity, int duration, int instrument,  int channel) {
        try {
            channels[channel].programChange(instrument);
            channels[channel].noteOn(note, velocity);
            Thread.sleep(duration);
            channels[channel].noteOff(note);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 转换音名到MIDI编号
     * @param noteName 音名字符串（如 "C4", "A#5", "Gb3"）
     * @return MIDI音符编号
     */
    public static int convertToMidi(String noteName) {
        // 音符映射 C=0, C#=1, D=2, D#=3, E=4, F=5, F#=6, G=7, G#=8, A=9, A#=10, B=11
        String[] notes = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

        // 解析八度
        int octave = Integer.parseInt(noteName.replaceAll("\\D", ""));
        String baseNote = noteName.replaceAll("\\d", "");

        // 查找基础音高
        int noteValue = -1;
        for (int i = 0; i < notes.length; i++) {
            if (notes[i].equalsIgnoreCase(baseNote)) {
                noteValue = i;
                break;
            }
        }

        // 计算MIDI编号 (C4 = 60)
        return 12 * (octave + 1) + noteValue;
    }
}
