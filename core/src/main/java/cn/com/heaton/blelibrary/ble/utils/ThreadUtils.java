package cn.com.heaton.blelibrary.ble.utils;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.RestrictTo;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import static android.os.AsyncTask.THREAD_POOL_EXECUTOR;

/**
 *
 * Created by LiuLei on 16/10/28.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ThreadUtils {

    private static ExecutorService executorService = Executors.newCachedThreadPool();
    private static Executor mParallelExecutor = AsyncTask.THREAD_POOL_EXECUTOR;//最多为5个线程  可以并发执行(也就是说最多只有5个线程同时运行，超过5个的就要等待)  异步线程池
    private static Executor mSerialExecutor = AsyncTask.SERIAL_EXECUTOR;//按照顺序执行  同步线程池(系统默认使用的)
    private static Handler sHandler = new Handler(Looper.getMainLooper());

    private ThreadUtils() {
        mParallelExecutor = THREAD_POOL_EXECUTOR;
        mSerialExecutor = AsyncTask.SERIAL_EXECUTOR;
    }

    public static void ui(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        }else {
            sHandler.post(runnable);
        }
    }

    public static void asyn(Runnable task) {
        asyn(task, true);
    }

    public static void asynQueue(Runnable task) {
        asyn(task, false);
    }

    public static void asyn(Runnable task, boolean parallel) {
        if (parallel) {
            mParallelExecutor.execute(task);
            return;
        }
        mSerialExecutor.execute(task);
    }

    public static void submit(Callable callable){
        executorService.submit(callable);
    }
}
