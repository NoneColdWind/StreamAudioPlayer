package cn.ncw.music.sample.music_1;

import java.util.concurrent.*;

import static cn.ncw.music.midi.MidiPlayer.playChord;
import static cn.ncw.music.midi.MidiPlayer.playNote;


public class SecondChannel {

    private static void part_0_0() {
        try {
            playNote(61, 64, 315, 0, 1);
            TimeUnit.MILLISECONDS.sleep(18);
            playNote(61, 64, 315, 0, 1);
        } catch (Exception ignored) {

        }
    }

    private static void part_0_1() {
        try {
            playNote(56, 64, 632, 0, 1);
            TimeUnit.MILLISECONDS.sleep(16);
        } catch (Exception ignored) {

        }
    }

    private static void total_0() {
        CountDownLatch countDownLatch = new CountDownLatch(2);
        ExecutorService threadPool = new ThreadPoolExecutor(2, 2, 200, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
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

        try {
            countDownLatch.await();
        } catch (InterruptedException ignored) {

        }
        //关闭线程池
        threadPool.shutdown();

    }

    private static void part_1_0() {
        try {
            playNote(61, 64, 316, 0, 1);
            TimeUnit.MILLISECONDS.sleep(18);
            playNote(61, 64, 315, 0, 1);
        } catch (Exception ignored) {

        }
    }

    private static void part_1_1() {
        try {
            playNote(53, 64, 632, 0, 1);
            TimeUnit.MILLISECONDS.sleep(17);
        } catch (Exception ignored) {

        }
    }

    private static void total_1() {
        CountDownLatch countDownLatch = new CountDownLatch(2);
        ExecutorService threadPool = new ThreadPoolExecutor(2, 2, 200, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
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

        try {
            countDownLatch.await();
        } catch (InterruptedException ignored) {

        }
        //关闭线程池
        threadPool.shutdown();

    }

    private static void part_2_0() {
        try {
            playNote(61, 64, 315, 0, 1);
            TimeUnit.MILLISECONDS.sleep(18);
            playNote(61, 64, 315, 0, 1);
        } catch (Exception ignored) {

        }
    }

    private static void part_2_1() {
        try {
            playNote(56, 64, 632, 0, 1);
            TimeUnit.MILLISECONDS.sleep(17);
        } catch (Exception ignored) {

        }
    }

    private static void total_2() {
        CountDownLatch countDownLatch = new CountDownLatch(2);
        ExecutorService threadPool = new ThreadPoolExecutor(2, 2, 200, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
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

        try {
            countDownLatch.await();
        } catch (InterruptedException ignored) {

        }
        //关闭线程池
        threadPool.shutdown();

    }

    private static void part_3_0() {
        try {
            playNote(42, 49, 1265, 0, 1);
            TimeUnit.MILLISECONDS.sleep(9);
        } catch (Exception ignored) {

        }
    }

    private static void part_3_1() {
        try {
            TimeUnit.MILLISECONDS.sleep(83);
            playNote(46, 49, 1186, 0, 1);
            TimeUnit.MILLISECONDS.sleep(5);
        } catch (Exception ignored) {

        }
    }

    private static void part_3_2() {
        try {
            TimeUnit.MILLISECONDS.sleep(167);
            playNote(49, 49, 1107, 0, 1);
        } catch (Exception ignored) {

        }
    }

    private static void total_3() {
        CountDownLatch countDownLatch = new CountDownLatch(3);
        ExecutorService threadPool = new ThreadPoolExecutor(3, 3, 200, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
        threadPool.submit(()->{
            try {
                part_3_0();
            }finally {
                countDownLatch.countDown();
            }
        });
        threadPool.submit(()->{
            try {
                part_3_1();
            }finally {
                countDownLatch.countDown();
            }
        });
        threadPool.submit(()->{
            try {
                part_3_2();
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
        playNote(54, 64, 1899, 0, 1);
        TimeUnit.MILLISECONDS.sleep(101);
        playNote(56 ,64 ,632, 0, 1);
        TimeUnit.MILLISECONDS.sleep(34);
        playNote(58, 64, 1898, 0, 1);
        TimeUnit.MILLISECONDS.sleep(102);
        playNote(53, 64, 632, 0, 1);
        TimeUnit.MILLISECONDS.sleep(34);
        playNote(54, 64, 1899, 0, 1);
        TimeUnit.MILLISECONDS.sleep(101);
        playNote(56, 64, 632, 0, 1);
        TimeUnit.MILLISECONDS.sleep(35);
        playNote(58, 64, 1899, 0, 1);
        TimeUnit.MILLISECONDS.sleep(101);
        playNote(53, 64, 632, 0, 1);
        TimeUnit.MILLISECONDS.sleep(35);
        playChord(new int[] {54, 61}, 64, 315, 0, 1);
        TimeUnit.MILLISECONDS.sleep(18);
        playChord(new int[] {54, 61}, 64, 315, 0, 1);
        TimeUnit.MILLISECONDS.sleep(18);
        playChord(new int[] {54, 61}, 64, 316, 0, 1);
        TimeUnit.MILLISECONDS.sleep(18);
        playChord(new int[] {54, 61}, 64, 315, 0, 1);
        TimeUnit.MILLISECONDS.sleep(18);
        playChord(new int[] {54, 61}, 64, 315, 0, 1);
        TimeUnit.MILLISECONDS.sleep(18);
        playChord(new int[] {54, 61}, 64, 316, 0, 1);
        TimeUnit.MILLISECONDS.sleep(18);
        total_0();
        TimeUnit.MILLISECONDS.sleep(18);
        playChord(new int[] {58, 61}, 64, 316, 0, 1);
        TimeUnit.MILLISECONDS.sleep(18);
        playChord(new int[] {58, 61}, 64, 315, 0, 1);
        TimeUnit.MILLISECONDS.sleep(18);
        playChord(new int[] {58, 61}, 64, 315, 0, 1);
        TimeUnit.MILLISECONDS.sleep(18);
        playChord(new int[] {58, 61}, 64, 316, 0, 1);
        TimeUnit.MILLISECONDS.sleep(18);
        playChord(new int[] {58, 61}, 64, 315, 0, 1);
        TimeUnit.MILLISECONDS.sleep(18);
        playChord(new int[] {58, 61}, 64, 315, 0, 1);
        TimeUnit.MILLISECONDS.sleep(18);
        total_1();
        TimeUnit.MILLISECONDS.sleep(18);
        playChord(new int[] {54, 61}, 64, 315, 0, 1);
        TimeUnit.MILLISECONDS.sleep(18);
        playChord(new int[] {54, 61}, 64, 316, 0, 1);
        TimeUnit.MILLISECONDS.sleep(18);
        playChord(new int[] {54, 61}, 64, 315, 0, 1);
        TimeUnit.MILLISECONDS.sleep(18);
        playChord(new int[] {54, 61}, 64, 315, 0, 1);
        TimeUnit.MILLISECONDS.sleep(18);
        playChord(new int[] {54, 61}, 64, 316, 0, 1);
        TimeUnit.MILLISECONDS.sleep(18);
        playChord(new int[] {54, 61}, 64, 315, 0, 1);
        TimeUnit.MILLISECONDS.sleep(18);
        total_2();
        TimeUnit.MILLISECONDS.sleep(35);
        playNote(46, 64, 5065, 0, 1);
        TimeUnit.MILLISECONDS.sleep(268);
        playNote(42, 49, 632, 0, 1);
        TimeUnit.MILLISECONDS.sleep(35);
        playChord(new int[] {42, 49}, 49, 632, 0, 1);
        TimeUnit.MILLISECONDS.sleep(34);
        playChord(new int[] {42, 49}, 49, 632, 0, 1);
        TimeUnit.MILLISECONDS.sleep(35);
        playChord(new int[] {42, 49}, 49, 632, 0, 1);
        TimeUnit.MILLISECONDS.sleep(35);
        playChord(new int[] {44, 49}, 49, 632, 0, 1);
        TimeUnit.MILLISECONDS.sleep(34);
        playChord(new int[] {44, 49}, 49, 632, 0, 1);
        TimeUnit.MILLISECONDS.sleep(35);
        playChord(new int[] {44, 49}, 49, 632, 0, 1);
        TimeUnit.MILLISECONDS.sleep(35);
        playChord(new int[] {44, 49}, 49, 632, 0, 1);
        TimeUnit.MILLISECONDS.sleep(34);
        playNote(41, 49, 632, 0, 1);
        TimeUnit.MILLISECONDS.sleep(35);
        playChord(new int[] {41, 49}, 49, 632, 0, 1);
        TimeUnit.MILLISECONDS.sleep(35);
        playChord(new int[] {41, 49}, 49, 632, 0, 1);
        TimeUnit.MILLISECONDS.sleep(34);
        playChord(new int[] {41, 49}, 49, 632, 0, 1);
        TimeUnit.MILLISECONDS.sleep(35);
        playChord(new int[] {41, 49}, 49, 632, 0, 1);
        TimeUnit.MILLISECONDS.sleep(35);
        playChord(new int[] {41, 49}, 49, 632, 0, 1);
        TimeUnit.MILLISECONDS.sleep(34);
        playChord(new int[] {41, 49}, 49, 632, 0, 1);
        TimeUnit.MILLISECONDS.sleep(35);
        playChord(new int[] {41, 49}, 49, 632, 0, 1);
        TimeUnit.MILLISECONDS.sleep(35);
        playNote(42, 43, 632, 0, 1);
        TimeUnit.MILLISECONDS.sleep(34);
        playChord(new int[] {42, 49}, 49, 632, 0, 1);
        TimeUnit.MILLISECONDS.sleep(35);
        playNote(44, 49, 632, 0, 1);
        TimeUnit.MILLISECONDS.sleep(35);
        playChord(new int[] {44, 49}, 49, 632, 0, 1);
        TimeUnit.MILLISECONDS.sleep(34);
        playNote(46, 49, 632, 0, 1);
        TimeUnit.MILLISECONDS.sleep(35);
        playChord(new int[] {46, 53}, 49, 632, 0, 1);
        TimeUnit.MILLISECONDS.sleep(35);
        playNote(46, 49, 157, 0, 1);
        TimeUnit.MILLISECONDS.sleep(9);
        playNote(56, 49, 316, 0, 1);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(48, 49, 315, 0, 1);
        TimeUnit.MILLISECONDS.sleep(18);
        playNote(44, 49, 474, 0, 1);
        TimeUnit.MILLISECONDS.sleep(26);
        total_3();  //16.3.1

    }
}
