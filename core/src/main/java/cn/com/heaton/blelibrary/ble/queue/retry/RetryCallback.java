package cn.com.heaton.blelibrary.ble.queue.retry;

/**
 * author: jerry
 * date: 21-1-8
 * email: superliu0911@gmail.com
 * des:
 */
interface RetryCallback<T> {
    void retry(T device);
}
