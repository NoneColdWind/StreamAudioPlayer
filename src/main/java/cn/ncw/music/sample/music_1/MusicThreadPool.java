package cn.ncw.music.sample.music_1;

import java.util.concurrent.*;

public class MusicThreadPool {

    public static void start() {
        try {
            TimeUnit.MILLISECONDS.sleep(5000);
        } catch (Exception ignored) {

        }
        CountDownLatch countDownLatch = new CountDownLatch(3);
        ExecutorService musicThreadPool = new ThreadPoolExecutor(2, 2, 200, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(100));
        musicThreadPool.submit(()->{
            try {
                FirstChannel.init();
            } catch (InterruptedException ignored) {

            } finally {
                countDownLatch.countDown();
            }
        });
        musicThreadPool.submit(()->{
            try {
                SecondChannel.init();
            } catch (InterruptedException ignored) {

            } finally {
                countDownLatch.countDown();
            }
        });
        try {
            countDownLatch.await();
        } catch (InterruptedException ignored) {

        }
        //关闭线程池
        musicThreadPool.shutdown();
    }

    public static void main(String[] args) {

       start();

    }

}
