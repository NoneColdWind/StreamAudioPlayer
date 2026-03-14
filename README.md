# StreamAudioPlayer

#### 这是一个流式播放器

## 快速开始
#### **·JDK要求**：Java 21或更高版本
#### **·构建项目**：
>\# **克隆项目**
> 
>#### git clone https://github.com/NoneColdWind/StreamAudioPlayer.git
> 
>#### cd StreamAudioPlayer
> 
>\# **使用Gradle构建**
> 
>#### ./gradlew build

#### **·使用gradle添加依赖**：
> #### build.gradle.kts
>
>     repositories {
> 
>         # 下载jar文件并放入项目目录libs\目录下
>         flatDir {
>             dirs("libs")
>         }
> 
>         mavenCentral()
>
>     }
>
>     dependencies {
> 
>         # 通过本地文件引入（JarName替换为实际的jar文件名，不包含后缀）
>         implementation(name, "JarName")
> 
>         compileOnly("org.projectlombok:lombok:1.18.42")
>         annotationProcessor("org.projectlombok:lombok:1.18.42")
>         implementation("ws.schild:jave-all-deps:3.5.0")
>    
>         // NCW Logger （项目来自：https://github.com/NoneColdWind/NCW-Logger）
>         // 下载NCW-Logger并放入libs\目录下
>         implementation(name ,"NCW-Logger-1.0.4")
>
>         // SLF4J API
>         implementation("org.slf4j:slf4j-api:1.7.32")
>
>         // Log4j2 核心库
>         implementation("org.apache.logging.log4j:log4j-core:2.25.3")
>         implementation("org.apache.logging.log4j:log4j-api:2.25.3")
>
>         // SLF4J 与 Log4j2 的桥接
>         implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.25.3")
>
>     }
>

#### **·项目前置依赖**：
>     dependencies {
> 
>         testImplementation(platform("org.junit:junit-bom:5.10.0"))
>         testImplementation("org.junit.jupiter:junit-jupiter")
>
>         compileOnly("org.projectlombok:lombok:1.18.42")
>         annotationProcessor("org.projectlombok:lombok:1.18.42")
>         implementation("ws.schild:jave-all-deps:3.5.0")
>    
>         // NCW Logger （项目来自：https://github.com/NoneColdWind/NCW-Logger）
>         implementation(name ,"NCW-Logger-1.0.4")
>
>         // SLF4J API
>         implementation("org.slf4j:slf4j-api:1.7.32")
> 
>         // Log4j2 核心库
>         implementation("org.apache.logging.log4j:log4j-core:2.25.3")
>         implementation("org.apache.logging.log4j:log4j-api:2.25.3")
> 
>         // SLF4J 与 Log4j2 的桥接
>         implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.25.3")
> 
>     }

## 使用指南
### 1.基本播放功能

> #### Main.java
>     import cn.ncw.music.stream.AdvancedStreamAudioPlayer;
>     import cn.ncw.logger.log.NCWLoggerFactory;
>     import java.util.concurrent.TimeUnit;
> 
>     public class Main {
> 
>         public static void main(String[] args) {
>             NCWLoggerFactory logger = new NCWLoggerFactory("Name");
>             AdvancedStreamAudioPlayer player = new AdvancedStreamAudioPlayer(logger);
>             
>             // 播放音乐
>             player.play("FilePath");
> 
>             
>             TimeUnit.SECONDS.sleep(5);
> 
>             // 暂停
>             player.pause();
> 
>             TimeUnit.SECONDS.sleep(5);
> 
>             // 继续
>             player.resume();
> 
>             TimeUnit.SECONDS.sleep(5);
>
>             // 终止
>             player.stop();            
>         }
>     }

### 2.播放列表

> #### Main.java
>     import cn.ncw.music.stream.AdvancedStreamAudioPlayer;
>     import cn.ncw.logger.log.NCWLoggerFactory;
>     import java.util.concurrent.TimeUnit;
>
>     public class Main {
>
>         public static void main(String[] args) {
>             NCWLoggerFactory logger = new NCWLoggerFactory("Name");
>             AdvancedStreamAudioPlayer player = new AdvancedStreamAudioPlayer(logger);
>             
>             // 创建测试文件列表
>             List<File> testFiles = new ArrayList<>();
>             for (int i = 1; i <= 6; i++) {
>                 File file = new File("test" + i + ".wav");
>                 if (file.exists()) {
>                     testFiles.add(file);
>                 } else {
>                     System.out.println("测试文件不存在: " + file.getAbsolutePath());
>                 }
>             }
>
>             if (testFiles.isEmpty()) {
>                 System.out.println("请创建test1.wav到test6.wav测试文件");
>                 return;
>             }
>
>             // 添加到播放列表
>             player.addToPlaylist(testFiles);
> 
>             player.play(testFiles.get(0));  // 或者player.play(0);
> 
>             TimeUnit.SECONDS.sleep(1145);
>         }
>     }
>

### 3.音量控制

> #### Main.java
>     import cn.ncw.music.stream.AdvancedStreamAudioPlayer;
>     import cn.ncw.logger.log.NCWLoggerFactory;
>     import java.util.concurrent.TimeUnit;
>
>     public class Main {
>
>         public static void main(String[] args) {
>             NCWLoggerFactory logger = new NCWLoggerFactory("Name");
>             AdvancedStreamAudioPlayer player = new AdvancedStreamAudioPlayer(logger);
>             
>             // 创建测试文件列表
>             List<File> testFiles = new ArrayList<>();
>             for (int i = 1; i <= 6; i++) {
>                 File file = new File("test" + i + ".wav");
>                 if (file.exists()) {
>                     testFiles.add(file);
>                 } else {
>                     System.out.println("测试文件不存在: " + file.getAbsolutePath());
>                 }
>             }
>
>             if (testFiles.isEmpty()) {
>                 System.out.println("请创建test1.wav到test6.wav测试文件");
>                 return;
>             }
>
>             // 添加到播放列表
>             player.addToPlaylist(testFiles);
>
>             player.play(0);
>             
>             // 调整音量
>             player.setVolume(0.5)
>             TimeUnit.SECONDS.sleep(5);
> 
>             // 提高音量
>             player.increaseVolume(0.2)
>             TimeUnit.SECONDS.sleep(5);
> 
>             // 降低音量
>             player.decreaseVolume(0.2)
>
>             TimeUnit.SECONDS.sleep(1145);
>         }
>     }
>

### 4.播放模式

> #### Main.java
>     import cn.ncw.music.stream.AdvancedStreamAudioPlayer;
>     import cn.ncw.logger.log.NCWLoggerFactory;
>     import java.util.concurrent.TimeUnit;
>
>     public class Main {
>
>         public static void main(String[] args) {
>             NCWLoggerFactory logger = new NCWLoggerFactory("Name");
>             AdvancedStreamAudioPlayer player = new AdvancedStreamAudioPlayer(logger);
>             
>             // 创建测试文件列表
>             List<File> testFiles = new ArrayList<>();
>             for (int i = 1; i <= 6; i++) {
>                 File file = new File("test" + i + ".wav");
>                 if (file.exists()) {
>                     testFiles.add(file);
>                 } else {
>                     System.out.println("测试文件不存在: " + file.getAbsolutePath());
>                 }
>             }
>
>             if (testFiles.isEmpty()) {
>                 System.out.println("请创建test1.wav到test6.wav测试文件");
>                 return;
>             }
>
>             // 添加到播放列表
>             player.addToPlaylist(testFiles);
>
>             player.play(0);
>             
>             // 单曲循环
>             player.setPlayMode(PlayMode.REPEAT_ONE);
>
>             /* 
>             顺序播放 PlayMode.NORMAL
>             单曲循环 PlayMode.REPEAT_ONE
>             顺序循环 PlayMode.REPEAT_ALL
>             随机循环 PlayMode.SHUFFLE
>             */
>
>             TimeUnit.SECONDS.sleep(1145);
>         }
>     }
>

### 5.播放跳转

> #### Main.java
>     import cn.ncw.music.stream.AdvancedStreamAudioPlayer;
>     import cn.ncw.logger.log.NCWLoggerFactory;
>     import java.util.concurrent.TimeUnit;
>
>     public class Main {
>
>         public static void main(String[] args) {
>             NCWLoggerFactory logger = new NCWLoggerFactory("Name");
>             AdvancedStreamAudioPlayer player = new AdvancedStreamAudioPlayer(logger);
>             
>             // 创建测试文件列表
>             List<File> testFiles = new ArrayList<>();
>             for (int i = 1; i <= 6; i++) {
>                 File file = new File("test" + i + ".wav");
>                 if (file.exists()) {
>                     testFiles.add(file);
>                 } else {
>                     System.out.println("测试文件不存在: " + file.getAbsolutePath());
>                 }
>             }
>
>             if (testFiles.isEmpty()) {
>                 System.out.println("请创建test1.wav到test6.wav测试文件");
>                 return;
>             }
>
>             // 添加到播放列表
>             player.addToPlaylist(testFiles);
>
>             player.play(0);
>
>             // 跳转到第5.0秒
>             boolean result1 = player.seekToTime(5.0);
>             logger.info("跳转结果: " + result1, "main")
> 
>             // 跳转到第1145帧
>             boolean result2 = player.seekToFrame(1145);
>             logger.info("跳转结果: " + result2, "main")
>
>             TimeUnit.SECONDS.sleep(1145);
>         }
>     }

### 6.获取音频元数据

> #### Main.java
>     import cn.ncw.music.stream.AdvancedStreamAudioPlayer;
>     import cn.ncw.logger.log.NCWLoggerFactory;
>     import java.util.concurrent.TimeUnit;
>
>     public class Main {
>
>         public static void main(String[] args) {
>             NCWLoggerFactory logger = new NCWLoggerFactory("Name");
>             AdvancedStreamAudioPlayer player = new AdvancedStreamAudioPlayer(logger);
>             
>             // 创建测试文件列表
>             List<File> testFiles = new ArrayList<>();
>             for (int i = 1; i <= 6; i++) {
>                 File file = new File("test" + i + ".wav");
>                 if (file.exists()) {
>                     testFiles.add(file);
>                 } else {
>                     System.out.println("测试文件不存在: " + file.getAbsolutePath());
>                 }
>             }
>
>             if (testFiles.isEmpty()) {
>                 System.out.println("请创建test1.wav到test6.wav测试文件");
>                 return;
>             }
>
>             // 添加到播放列表
>             player.addToPlaylist(testFiles);
>
>             player.play(0);
>
>             // 获取音频元数据
>             Map<String, Object> metadata = player.getAudioMetadata();
>             metadata.forEach((k, v) -> logger.info(k + ": " + v, "main"));
>
>             TimeUnit.SECONDS.sleep(1145);
>         }
>     }

### 7.音频格式转换

> #### Main.java
>     import cn.ncw.music.method.Converter;
>
>     public class Main {
>
>         public static void main(String[] args) {
>             
>             Converter.convert("input.mp3", "output.wav", "wav");
> 
>         }
>     }