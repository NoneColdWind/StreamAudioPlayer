package cn.ncw.music.sample.music_1;

import java.util.concurrent.*;

public class MusicThreadPool {

    public static void start() {
        try {
            TimeUnit.MILLISECONDS.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        CountDownLatch countDownLatch = new CountDownLatch(2);
        ExecutorService musicThreadPool = new ThreadPoolExecutor(2, 2, 200, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(100));
        musicThreadPool.submit(()->{
            try {
                FirstChannel.init();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                countDownLatch.countDown();
            }
        });
        musicThreadPool.submit(()->{
            try {
                SecondChannel.init();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                countDownLatch.countDown();
            }
        });
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        //关闭线程池
        musicThreadPool.shutdown();
    }

    public static void main(String[] args) {

       start();

    }

}
