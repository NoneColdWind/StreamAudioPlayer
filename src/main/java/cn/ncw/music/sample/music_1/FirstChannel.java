package cn.ncw.music.sample.music_1;

import java.util.concurrent.*;

import static cn.ncw.music.midi.MidiPlayer.playChord;
import static cn.ncw.music.midi.MidiPlayer.playNote;


public class FirstChannel {

    private static void part_0_0() {
        playNote(61, 64, 2453 ,0, 0);
    }

    private static void part_0_1() {
        try {
            TimeUnit.MILLISECONDS.sleep(83);
            playNote(68, 64, 2532, 0, 0);
        } catch (Exception ignored) {

        }
    }
    private static void part_0_2() {
        try {
            TimeUnit.MILLISECONDS.sleep(166);
            playNote(72, 64, 78, 0, 0);
            TimeUnit.MILLISECONDS.sleep(6);
            playNote(73, 64, 2294, 0, 0);
        } catch (Exception ignored) {

        }
    }

    private static void total_0() {
        CountDownLatch countDownLatch = new CountDownLatch(3);
        ExecutorService threadPool = new ThreadPoolExecutor(3, 3, 200, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
        threadPool.submit(()->{
            try {
                part_0_0();
            }finally {
                countDownLatch.countDown();
            }
        });
        threadPool.submit(()->{
            try {
                part_0_1();
            }finally {
                countDownLatch.countDown();
            }
        });
        threadPool.submit(()->{
            try {
                part_0_2();
            }finally {
                countDownLatch.countDown();
            }
        });

        try {
            countDownLatch.await();
        } catch (InterruptedException ignored) {

        }
        //关闭线程池
        threadPool.shutdown();

    }

    private static void part_1_0() {
        try {
            playNote(63, 49, 632, 0, 0);
            TimeUnit.MILLISECONDS.sleep(8);
        } catch (Exception ignored) {

        }
    }

    private static void part_1_1() {
        try {
            TimeUnit.MILLISECONDS.sleep(83);
            playNote(68, 49, 553, 0, 0);
            TimeUnit.MILLISECONDS.sleep(4);
        } catch (Exception ignored) {

        }
    }

    private static void part_1_2() {
        try {
            TimeUnit.MILLISECONDS.sleep(166);
            playNote(75, 49, 474, 0, 0);
        } catch (Exception ignored) {

        }
    }

    private static void total_1() {
        CountDownLatch countDownLatch = new CountDownLatch(3);
        ExecutorService threadPool = new ThreadPoolExecutor(3, 3, 200, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
        threadPool.submit(()->{
            try {
                part_1_0();
            }finally {
                countDownLatch.countDown();
            }
        });
        threadPool.submit(()->{
            try {
                part_1_1();
            }finally {
                countDownLatch.countDown();
            }
        });
        threadPool.submit(()->{
            try {
                part_1_2();
            }finally {
                countDownLatch.countDown();
            }
        });

        try {
            countDownLatch.await();
        } catch (InterruptedException ignored) {

        }
        //关闭线程池
        threadPool.shutdown();

    }

    private static void part_2_0() {
        try {
            playNote(75, 49, 3165, 0, 0);
            TimeUnit.MILLISECONDS.sleep(9);
        } catch (Exception ignored) {

        }
    }

    private static void part_2_1() {
        try {
            TimeUnit.MILLISECONDS.sleep(83);
            playNote(80, 49, 3086, 0, 0);
            TimeUnit.MILLISECONDS.sleep(5);
        } catch (Exception ignored) {

        }
    }

    private static void part_2_2() {
        try {
            TimeUnit.MILLISECONDS.sleep(167);
            playNote(87, 49, 3007, 0, 0);
        } catch (Exception ignored) {

        }
    }

    private static void total_2() {
        CountDownLatch countDownLatch = new CountDownLatch(3);
        ExecutorService threadPool = new ThreadPoolExecutor(3, 3, 200, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
        threadPool.submit(()->{
            try {
                part_2_0();
            }finally {
                countDownLatch.countDown();
            }
        });
        threadPool.submit(()->{
            try {
                part_2_1();
            }finally {
                countDownLatch.countDown();
            }
        });
        threadPool.submit(()->{
            try {
                part_2_2();
            }finally {
                countDownLatch.countDown();
            }
        });

        try {
            countDownLatch.await();
        } catch (InterruptedException ignored) {

        }
        //关闭线程池
        threadPool.shutdown();

    }

    public static void init() throws InterruptedException {

        TimeUnit.MILLISECONDS.sleep(10);
        playChord(new int[] {61, 68, 73}, 64, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(68, 64, 157 ,0, 0);
        TimeUnit.MILLISECONDS.sleep(10);
        playNote(73 ,64 ,315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(68, 64, 157 ,0, 0);
        TimeUnit.MILLISECONDS.sleep(10);
        playNote(73 ,64 ,315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(68, 64, 157 ,0, 0);
        TimeUnit.MILLISECONDS.sleep(10);
        playNote(73 ,64 ,315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(68, 64, 157 ,0, 0);
        TimeUnit.MILLISECONDS.sleep(10);
        playChord(new int[] {68, 72}, 64, 632 ,0, 0);
        TimeUnit.MILLISECONDS.sleep(35);
        playChord(new int[] {61, 68, 73}, 64, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(68, 64, 157 ,0, 0);
        TimeUnit.MILLISECONDS.sleep(10);
        playNote(73 ,64 ,315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(68, 64, 157 ,0, 0);
        TimeUnit.MILLISECONDS.sleep(10);
        playNote(73 ,64 ,315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(68, 64, 157 ,0, 0);
        TimeUnit.MILLISECONDS.sleep(10);
        playNote(73 ,64 ,315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(68, 64, 157 ,0, 0);
        TimeUnit.MILLISECONDS.sleep(10);
        playChord(new int[] {68, 75}, 64, 632 ,0, 0);
        TimeUnit.MILLISECONDS.sleep(35);
        playChord(new int[] {61, 68, 73}, 64, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(68, 64, 157 ,0, 0);
        TimeUnit.MILLISECONDS.sleep(10);
        playNote(73 ,64 ,315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(68, 64, 157 ,0, 0);
        TimeUnit.MILLISECONDS.sleep(10);
        playNote(73 ,64 ,315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(68, 64, 157 ,0, 0);
        TimeUnit.MILLISECONDS.sleep(10);
        playNote(73 ,64 ,315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(68, 64, 157 ,0, 0);
        TimeUnit.MILLISECONDS.sleep(10);
        playChord(new int[] {68, 75}, 64, 632 ,0, 0);
        TimeUnit.MILLISECONDS.sleep(35);
        playChord(new int[] {73, 77}, 64, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(68 ,64, 157, 0, 0);
        TimeUnit.MILLISECONDS.sleep(10);
        playNote(77 ,64, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(68 ,64, 157, 0, 0);
        TimeUnit.MILLISECONDS.sleep(10);
        playNote(77 ,64, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(68 ,64, 157, 0, 0);
        TimeUnit.MILLISECONDS.sleep(10);
        playNote(73 ,64, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(68 ,64, 157, 0, 0);
        TimeUnit.MILLISECONDS.sleep(10);
        playChord(new int[] {65, 72}, 64, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(68, 64, 316, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playChord(new int[] {68, 73}, 64, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(68 ,64, 157, 0, 0);
        TimeUnit.MILLISECONDS.sleep(10);
        playNote(80 ,64, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(68 ,64, 157, 0, 0);
        TimeUnit.MILLISECONDS.sleep(10);
        playNote(73 ,64, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(68 ,64, 157, 0, 0);
        TimeUnit.MILLISECONDS.sleep(10);
        playNote(80 ,64, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(68 ,64, 157, 0, 0);
        TimeUnit.MILLISECONDS.sleep(10);
        playChord(new int[] {68, 72}, 64, 632, 0, 0);
        TimeUnit.MILLISECONDS.sleep(34);
        playChord(new int[] {68, 73}, 64, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(68 ,64, 157, 0, 0);
        TimeUnit.MILLISECONDS.sleep(10);
        playNote(80 ,64, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(68 ,64, 157, 0, 0);
        TimeUnit.MILLISECONDS.sleep(10);
        playNote(73 ,64, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(68 ,64, 157, 0, 0);
        TimeUnit.MILLISECONDS.sleep(10);
        playNote(80 ,64, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(68 ,64, 157, 0, 0);
        TimeUnit.MILLISECONDS.sleep(10);
        playChord(new int[] {68, 72}, 64, 632, 0, 0);
        TimeUnit.MILLISECONDS.sleep(34);
        playChord(new int[] {68, 73}, 64, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(68 ,64, 157, 0, 0);
        TimeUnit.MILLISECONDS.sleep(10);
        playChord(new int[] {80, 92}, 64, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(68 ,64, 157, 0, 0);
        TimeUnit.MILLISECONDS.sleep(10);
        playChord(new int[] {73, 85}, 64, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(68 ,64, 157, 0, 0);
        TimeUnit.MILLISECONDS.sleep(10);
        playChord(new int[] {80, 92}, 64, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(68 ,64, 157, 0, 0);
        TimeUnit.MILLISECONDS.sleep(10);
        playChord(new int[] {68, 72}, 64, 632, 0, 0);
        TimeUnit.MILLISECONDS.sleep(35);
        total_0();
        TimeUnit.MILLISECONDS.sleep(122);
        playNote(85 ,64, 78, 0, 0);
        TimeUnit.MILLISECONDS.sleep(6);
        playNote(84 ,64, 77, 0, 0);
        TimeUnit.MILLISECONDS.sleep(6);
        playNote(80 ,64, 78, 0, 0);
        TimeUnit.MILLISECONDS.sleep(5);
        playNote(77 ,63, 78, 0, 0);
        TimeUnit.MILLISECONDS.sleep(6);
        playNote(73 ,63, 1582, 0, 0);
        TimeUnit.MILLISECONDS.sleep(84);
        playNote(56 ,53, 632, 0, 0);
        TimeUnit.MILLISECONDS.sleep(35);
        playNote(61 ,49, 632, 0, 0);
        TimeUnit.MILLISECONDS.sleep(35);
        playNote(68 ,49, 473, 0, 0);
        TimeUnit.MILLISECONDS.sleep(27);
        playNote(63 ,49, 473, 0, 0);
        TimeUnit.MILLISECONDS.sleep(27);
        playNote(61 ,49, 473, 0, 0);
        TimeUnit.MILLISECONDS.sleep(27);
        playNote(56 ,49, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(61 ,49, 474, 0, 0);
        TimeUnit.MILLISECONDS.sleep(26);
        playNote(61 ,49, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(63 ,49, 316, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(65 ,49, 157, 0, 0);
        TimeUnit.MILLISECONDS.sleep(9);
        playNote(63 ,49, 316, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(61 ,49, 157, 0, 0);
        TimeUnit.MILLISECONDS.sleep(9);
        playNote(65,49, 474, 0, 0);
        TimeUnit.MILLISECONDS.sleep(26);
        playNote(56,49, 474, 0, 0);
        TimeUnit.MILLISECONDS.sleep(26);
        playNote(61,49, 632, 0, 0);
        TimeUnit.MILLISECONDS.sleep(35);
        playNote(68,49, 474, 0, 0);
        TimeUnit.MILLISECONDS.sleep(26);
        playNote(63,49, 474, 0, 0); //12.3.1
        TimeUnit.MILLISECONDS.sleep(26);
        playNote(61,49, 157, 0, 0);
        TimeUnit.MILLISECONDS.sleep(10);
        playNote(66,49, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(65,49, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(63,49, 474, 0, 0);
        TimeUnit.MILLISECONDS.sleep(26);
        playNote(61,49, 316, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(63,49, 315, 0, 0); //13.2.1
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(65,49, 157, 0, 0);
        TimeUnit.MILLISECONDS.sleep(10);
        playNote(66,49, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(65,49, 157, 0, 0);
        TimeUnit.MILLISECONDS.sleep(10);
        playNote(61,49, 473, 0, 0);
        TimeUnit.MILLISECONDS.sleep(27);
        playNote(63,49, 473, 0, 0);
        TimeUnit.MILLISECONDS.sleep(27);
        playNote(61,49, 632, 0, 0);
        TimeUnit.MILLISECONDS.sleep(34);
        playNote(68,49, 474, 0, 0);
        TimeUnit.MILLISECONDS.sleep(26);
        playNote(63,49, 632, 0, 0);
        TimeUnit.MILLISECONDS.sleep(26);
        playNote(61,49, 474, 0, 0);
        TimeUnit.MILLISECONDS.sleep(26);
        playNote(56,49, 316, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(61,49, 473, 0, 0);
        TimeUnit.MILLISECONDS.sleep(27);
        playNote(61,49, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(63,49, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(65,49, 157, 0, 0);
        TimeUnit.MILLISECONDS.sleep(10);
        playNote(63,49, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(61,49, 316, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(60,49, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(56,49, 474, 0, 0);
        TimeUnit.MILLISECONDS.sleep(26);
        playChord(new int[] {73, 80}, 49, 632, 0, 0);
        TimeUnit.MILLISECONDS.sleep(35);
        playNote(77,49, 632, 0, 0);
        TimeUnit.MILLISECONDS.sleep(34);
        playNote(73,49, 316, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(56,49, 157, 0, 0);
        TimeUnit.MILLISECONDS.sleep(9);
        playNote(61,49, 316, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(65,49, 473, 0, 0);
        TimeUnit.MILLISECONDS.sleep(27);
        total_1();
        TimeUnit.MILLISECONDS.sleep(693);
        total_2();
        TimeUnit.MILLISECONDS.sleep(826);
        playNote(68, 49, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(73, 50, 316, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playChord(new int[] {68, 75}, 52, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(80, 54, 157, 0, 0);
        TimeUnit.MILLISECONDS.sleep(10);
        playNote(75, 55, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(73, 57, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(68, 57, 157, 0, 0);
        TimeUnit.MILLISECONDS.sleep(10);
        playNote(75, 60, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(73, 62, 316, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(68, 64, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(73, 64, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(75, 64, 316, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(80, 64, 157, 0, 0);
        TimeUnit.MILLISECONDS.sleep(9);
        playNote(75, 64, 316, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(73, 64, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(68, 64, 157, 0, 0);
        TimeUnit.MILLISECONDS.sleep(10);
        playNote(73, 64, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(75, 64, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(68, 64, 316, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(73, 64, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(75, 64, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(80, 64, 157, 0, 0);
        TimeUnit.MILLISECONDS.sleep(10);
        playNote(75, 64, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(73, 64, 316, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(68, 64, 157, 0, 0);
        TimeUnit.MILLISECONDS.sleep(9);
        playNote(75, 64, 316, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(73, 64, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playChord(new int[] {73, 77}, 64, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playChord(new int[] {75, 78}, 64, 316, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(77, 64, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(78, 65, 157, 0, 0);
        TimeUnit.MILLISECONDS.sleep(10);
        playChord(new int[] {77, 80}, 65, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(73, 65, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playChord(new int[] {68, 75}, 66, 316, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(77, 66, 157, 0, 0);
        TimeUnit.MILLISECONDS.sleep(9);
        playNote(75, 66, 316, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(68, 67, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(73, 67, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playChord(new int[] {68, 75}, 68, 316, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(80, 68, 157, 0, 0);
        TimeUnit.MILLISECONDS.sleep(9);
        playNote(75, 68, 316, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(73, 69, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(68, 69, 157, 0, 0);
        TimeUnit.MILLISECONDS.sleep(10);
        playNote(75, 69, 315, 0, 0); //23.4.1
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(73, 70, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playChord(new int[] {63, 68}, 70, 316, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(73, 70, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(75, 71, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(80, 71, 157, 0, 0);
        TimeUnit.MILLISECONDS.sleep(10);
        playNote(75, 71, 315, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(73, 72, 316, 0, 0);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(68, 72, 157, 0, 0); //24.4.1


    }

    public static void main(String[] args) throws InterruptedException {
        init();
    }

}
