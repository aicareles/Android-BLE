package cn.com.heaton.blelibrary.ble;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;

import static android.os.AsyncTask.THREAD_POOL_EXECUTOR;

/**
 *
 * Created by LiuLei on 16/10/28.
 */

public class TaskExecutor {

    private static Executor mParallelExecutor = AsyncTask.THREAD_POOL_EXECUTOR;//最多为5个线程  可以并发执行(也就是说最多只有5个线程同时运行，超过5个的就要等待)  异步线程池
    private static Executor mSerialExecutor = AsyncTask.SERIAL_EXECUTOR;//按照顺序执行  同步线程池(系统默认使用的)
    private static Handler sHandler = new Handler(Looper.getMainLooper());

    private TaskExecutor() {
        mParallelExecutor = THREAD_POOL_EXECUTOR;
        mSerialExecutor = AsyncTask.SERIAL_EXECUTOR;
    }

    public static void runOnUIThread(Runnable runnable) {
        sHandler.post(runnable);
    }

    public static void executeTask(Runnable task) {
        executeTask(task, true);
    }

    public static void executeTaskSerially(Runnable task) {
        executeTask(task, false);
    }

    public static void executeTask(Runnable task, boolean parallel) {
        if (parallel) {
            mParallelExecutor.execute(task);
            return;
        }
        mSerialExecutor.execute(task);
    }
}
