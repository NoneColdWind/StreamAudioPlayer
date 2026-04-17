package cn.ncw.music.midi.enums;

/**
 * MIDI 乐器枚举
 */
public enum MidiInstrument {
    ACOUSTIC_GRAND_PIANO(0, "Acoustic Grand Piano"),
    BRIGHT_ACOUSTIC_PIANO(1, "Bright Acoustic Piano"),
    ELECTRIC_GRAND_PIANO(2, "Electric Grand Piano"),
    HONKY_TONK_PIANO(3, "Honky-tonk Piano"),
    RHODES_PIANO(4, "Rhodes Piano"),
    CHORUSED_PIANO(5, "Chorused Piano"),
    HARPSICHORD(6, "Harpsichord"),
    CLAVICHORD(7, "Clavichord"),
    CELESTA(8, "Celesta"),
    GLOCKENSPIEL(9, "Glockenspiel"),
    MUSIC_BOX(10, "Music Box"),
    VIBRAPHONE(11, "Vibraphone"),
    MARIMBA(12, "Marimba"),
    XYLOPHONE(13, "Xylophone"),
    TUBULAR_BELLS(14, "Tubular Bells"),
    DULCIMER(15, "Dulcimer"),
    HAMMOND_ORGAN(16, "Hammond Organ"),
    PERCUSSIVE_ORGAN(17, "Percussive Organ"),
    ROCK_ORGAN(18, "Rock Organ"),
    CHURCH_ORGAN(19, "Church Organ"),
    REED_ORGAN(20, "Reed Organ"),
    ACCORDION(21, "Accordion"),
    HARMONICA(22, "Harmonica"),
    TANGO_ACCORDIAN(23, "Tango Accordian"),
    ACOUSTIC_GUITAR_NYLON(24, "Acoustic Guitar (nylon)"),
    ACOUSTIC_GUITAR_STEEL(25, "Acoustic Guitar (steel)"),
    ELECTRIC_GUITAR_JAZZ(26, "Electric Guitar (jazz)"),
    ELECTRIC_GUITAR_CLEAN(27, "Electric Guitar (clean)"),
    ELECTRIC_GUITAR_MUTED(28, "Electric Guitar (muted)"),
    OVERDRIVEN_GUITAR(29, "Overdriven Guitar"),
    DISTORTION_GUITAR(30, "Distortion Guitar"),
    GUITAR_HARMONICS(31, "Guitar Harmonics"),
    ACOUSTIC_BASS(32, "Acoustic Bass"),
    ELECTRIC_BASS_FINGER(33, "Electric Bass (finger)"),
    ELECTRIC_BASS_PICK(34, "Electric Bass (pick)"),
    FRETLESS_BASS(35, "Fretless Bass"),
    SLAP_BASS_1(36, "Slap Bass 1"),
    SLAP_BASS_2(37, "Slap Bass 2"),
    SYNTH_BASS_1(38, "Synth Bass 1"),
    SYNTH_BASS_2(39, "Synth Bass 2"),
    VIOLIN(40, "Violin"),
    VIOLA(41, "Viola"),
    CELLO(42, "Cello"),
    CONTRABASS(43, "Contrabass"),
    TREMOLO_STRINGS(44, "Tremolo Strings"),
    PIZZICATO_STRINGS(45, "Pizzicato Strings"),
    ORCHESTRAL_HARP(46, "Orchestral Harp"),
    TIMPANI(47, "Timpani"),
    STRING_ENSEMBLE_1(48, "String Ensemble 1"),
    STRING_ENSEMBLE_2(49, "String Ensemble 2"),
    SYNTH_STRINGS_1(50, "Synth Strings 1"),
    SYNTH_STRINGS_2(51, "Synth Strings 2"),
    CHOIR_AAHS(52, "Choir Aahs"),
    VOICE_OOHS(53, "Voice Oohs"),
    SYNTH_VOICE(54, "Synth Voice"),
    ORCHESTRA_HIT(55, "Orchestra Hit"),
    TRUMPET(56, "Trumpet"),
    TROMBONE(57, "Trombone"),
    TUBA(58, "Tuba"),
    MUTED_TRUMPET(59, "Muted Trumpet"),
    FRENCH_HORN(60, "French Horn"),
    BRASS_SECTION(61, "Brass Section"),
    SYNTH_BRASS_1(62, "Synth Brass 1"),
    SYNTH_BRASS_2(63, "Synth Brass 2"),
    SOPRANO_SAX(64, "Soprano Sax"),
    ALTO_SAX(65, "Alto Sax"),
    TENOR_SAX(66, "Tenor Sax"),
    BARITONE_SAX(67, "Baritone Sax"),
    OBOE(68, "Oboe"),
    ENGLISH_HORN(69, "English Horn"),
    BASSOON(70, "Bassoon"),
    CLARINET(71, "Clarinet"),
    PICCOLO(72, "Piccolo"),
    FLUTE(73, "Flute"),
    RECORDER(74, "Recorder"),
    PAN_FLUTE(75, "Pan Flute"),
    BOTTLE_BLOW(76, "Bottle Blow"),
    SHAKUHACHI(77, "Shakuhachi"),
    WHISTLE(78, "Whistle"),
    OCARINA(79, "Ocarina"),
    LEAD_1_SQUARE(80, "Lead 1 (square)"),
    LEAD_2_SAWTOOTH(81, "Lead 2 (sawtooth)"),
    LEAD_3_CALIope_LEAD(82, "Lead 3 (caliope lead)"),
    LEAD_4_CHIFF_LEAD(83, "Lead 4 (chiff lead)"),
    LEAD_5_CHARANG(84, "Lead 5 (charang)"),
    LEAD_6_VOICE(85, "Lead 6 (voice)"),
    LEAD_7_FIFTHS(86, "Lead 7 (fifths)"),
    LEAD_8_BASS_AND_LEAD(87, "Lead 8 (bass and lead)"),
    PAD_1_NEW_AGE(88, "Pad 1 (new age)"),
    PAD_2_WARM(89, "Pad 2 (warm)"),
    PAD_3_POLYSYNTH(90, "Pad 3 (polysynth)"),
    PAD_4_CHOIR(91, "Pad 4 (choir)"),
    PAD_5_BOWED(92, "Pad 5 (bowed)"),
    PAD_6_METALLIC(93, "Pad 6 (metallic)"),
    PAD_7_HALO(94, "Pad 7 (halo)"),
    PAD_8_SWEEP(95, "Pad 8 (sweep)"),
    FX_1_RAIN(96, "FX 1 (rain)"),
    FX_2_SOUNDTRACK(97, "FX 2 (soundtrack)"),
    FX_3_CRYSTAL(98, "FX 3 (crystal)"),
    FX_4_ATMOSPHERE(99, "FX 4 (atmosphere)"),
    FX_5_BRIGHTNESS(100, "FX 5 (brightness)"),
    FX_6_GOBLINS(101, "FX 6 (goblins)"),
    FX_7_ECHOES(102, "FX 7 (echoes)"),
    FX_8_SCI_FI(103, "FX 8 (sci-fi)"),
    SITAR(104, "Sitar"),
    BANJO(105, "Banjo"),
    SHAMISEN(106, "Shamisen"),
    KOTO(107, "Koto"),
    KALIMBA(108, "Kalimba"),
    BAGPIPE(109, "Bagpipe"),
    FIDDLE(110, "Fiddle"),
    SHANAI(111, "Shanai"),
    TINKLE_BELL(112, "Tinkle Bell"),
    AGOGO_AGOGO(113, "Agogo Agogo"),
    STEEL_DRUMS(114, "Steel Drums"),
    WOODBLOCK(115, "Woodblock"),
    TAIKO_DRUM(116, "Taiko Drum"),
    MELODIC_TOM(117, "Melodic Tom"),
    SYNTH_DRUM(118, "Synth Drum"),
    REVERSE_CYMBAL(119, "Reverse Cymbal"),
    GUITAR_FRET_NOISE(120, "Guitar Fret Noise"),
    BREATH_NOISE(121, "Breath Noise"),
    SEASHORE(122, "Seashore"),
    BIRD_TWEET(123, "Bird Tweet"),
    TELEPHONE_RING(124, "Telephone Ring"),
    HELICOPTER(125, "Helicopter"),
    APPLAUSE(126, "Applause"),
    GUNSHOT(127, "Gunshot");

    private final int value;
    private final String name;

    MidiInstrument(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    /**
     * 根据数值获取乐器枚举
     */
    public static MidiInstrument fromValue(int value) {
        for (MidiInstrument instrument : values()) {
            if (instrument.value == value) {
                return instrument;
            }
        }
        throw new IllegalArgumentException("Invalid MIDI instrument value: " + value);
    }

    /**
     * 根据名称获取乐器枚举
     */
    public static MidiInstrument fromName(String name) {
        for (MidiInstrument instrument : values()) {
            if (instrument.name.equalsIgnoreCase(name)) {
                return instrument;
            }
        }
        throw new IllegalArgumentException("Invalid MIDI instrument name: " + name);
    }
}
