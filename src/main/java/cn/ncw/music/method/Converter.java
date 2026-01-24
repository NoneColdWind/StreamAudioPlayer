package cn.ncw.music.method;

import ws.schild.jave.Encoder;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;

import java.io.File;

public class Converter {

    public static void convert(String inputFile, String outputFile, String outputFormat) {
        File source = new File(inputFile);
        File target = new File(outputFile);

        // 配置音频转换参数
        AudioAttributes audio = new AudioAttributes();
        // audio.setCodec("pcm_s16le"); // 设置WAV的PCM编码，通常可省略，库会自动处理
        // 可以根据需要设置比特率、声道数、采样率等
        // audio.setBitRate(128000);
        // audio.setChannels(2);
        // audio.setSamplingRate(44100);

        // 配置编码属性，指定输出格式为WAV
        EncodingAttributes encodingAttributes = new EncodingAttributes();
        encodingAttributes.setOutputFormat(outputFormat);
        encodingAttributes.setAudioAttributes(audio);

        // 创建编码器并执行转换
        Encoder encoder = new Encoder();
        try {
            encoder.encode(new MultimediaObject(source), target, encodingAttributes);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
