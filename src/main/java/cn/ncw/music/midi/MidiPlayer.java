package cn.ncw.music.midi;

import cn.ncw.music.midi.enums.MidiInstrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;

public class MidiPlayer {

    private static Synthesizer synth;
    private static MidiChannel[] channels;

    // 常用音色枚举
    public static final MidiInstrument PIANO = MidiInstrument.ACOUSTIC_GRAND_PIANO;
    public static final MidiInstrument MARIMBA = MidiInstrument.MARIMBA;
    public static final MidiInstrument ORGAN = MidiInstrument.HAMMOND_ORGAN;
    public static final MidiInstrument GUITAR = MidiInstrument.ACOUSTIC_GUITAR_STEEL;
    public static final MidiInstrument BASS = MidiInstrument.ELECTRIC_BASS_FINGER;
    public static final MidiInstrument VIOLIN = MidiInstrument.VIOLIN;
    public static final MidiInstrument TRUMPET = MidiInstrument.TRUMPET;
    public static final MidiInstrument FLUTE = MidiInstrument.FLUTE;

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
     * @param velocity 音量(0-127)
     * @param duration 持续时间（毫秒）
     * @param instrument 音色编号（0-127）
     */
    public static void playChord(int[] notes, int velocity, int duration, int instrument) {
        playChord(notes, velocity, duration, instrument, 0);
    }

    /**
     * 播放和弦（指定通道）
     * @param notes 音符数组
     * @param velocity 音量(0-127)
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
                channels[channel].noteOn(note, velocity);
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
     * 播放和弦（使用枚举）
     * @param notes 音符数组（MIDI编号）
     * @param velocity 音量(0-127)
     * @param duration 持续时间（毫秒）
     * @param instrument 音色枚举
     */
    public static void playChord(int[] notes, int velocity, int duration, MidiInstrument instrument) {
        playChord(notes, velocity, duration, instrument.getValue(), 0);
    }

    /**
     * 播放和弦（使用枚举，指定通道）
     * @param notes 音符数组
     * @param velocity 音量(0-127)
     * @param duration 持续时间
     * @param instrument 音色枚举
     * @param channel 通道编号（0-15）
     */
    public static void playChord(int[] notes, int velocity, int duration, MidiInstrument instrument, int channel) {
        playChord(notes, velocity, duration, instrument.getValue(), channel);
    }

    /**
     * 播放单音
     * @param note MIDI音符编号
     * @param velocity 音量(0-127)
     * @param duration 持续时间(毫秒)
     * @param instrument 音色编号
     */
    public static void playNote(int note, int velocity, int duration, int instrument) {
        playNote(note, velocity, duration, instrument, 0);
    }

    /**
     * 播放单音(指定通道)
     * @param note MIDI音符编号
     * @param velocity 音量(0-127)
     * @param duration 持续时间(毫秒)
     * @param instrument 音色编号
     * @param channel 通道编号(0-15)
     */
    public static void playNote(int note, int velocity, int duration, int instrument, int channel) {
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
     * 播放单音（使用枚举）
     * @param note MIDI音符编号
     * @param velocity 音量(0-127)
     * @param duration 持续时间(毫秒)
     * @param instrument 音色枚举
     */
    public static void playNote(int note, int velocity, int duration, MidiInstrument instrument) {
        playNote(note, velocity, duration, instrument.getValue(), 0);
    }

    /**
     * 播放单音(使用枚举，指定通道)
     * @param note MIDI音符编号
     * @param velocity 音量(0-127)
     * @param duration 持续时间(毫秒)
     * @param instrument 音色枚举
     * @param channel 通道编号(0-15)
     */
    public static void playNote(int note, int velocity, int duration, MidiInstrument instrument, int channel) {
        playNote(note, velocity, duration, instrument.getValue(), channel);
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

    public static void main(String[] args) {
        playNote(60, 100, 500, 0, 0);
    }

}
