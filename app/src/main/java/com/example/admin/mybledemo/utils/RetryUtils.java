package com.example.admin.mybledemo.utils;

import java.util.concurrent.Callable;

/**
 * description $desc$
 * created by jerry on 2019/5/24.
 */
public class RetryUtils {

    /**
     * 带有重试的执行方法
     * @param callable 目标方法
     * @param retryCount 重试次数
     * @param delay 每次重试的间隔时间 ms
     */
    public static void call(Callable<Boolean> callable, int retryCount, long delay){
        /*Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
                .retryIfResult(Predicates.equalTo(false))//返回false需要重试
                .retryIfExceptionOfType(IOException.class)//抛出io异常会重试。
                .retryIfRuntimeException()//抛出runtime异常会重试。
                .withWaitStrategy(WaitStrategies.fixedWait(delay, TimeUnit.MILLISECONDS))//每次重试间隔
                .withStopStrategy(StopStrategies.stopAfterAttempt(retryCount))//尝试次数
                .build();
        try {
            retryer.call(callable);
        } catch (RetryException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }*/
    }
}
