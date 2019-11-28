package cn.com.heaton.blelibrary.ble.queue;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import cn.com.heaton.blelibrary.ble.BleLog;

public class Task implements Delayed {
    private static final String TAG = "Task";
    /**
     * 到期时间，单位是秒
     */
    private final long timeOut;
    private final long realTime;
    private final RequestTask requestTask;
    /**
     * 产生序列号
     */
    private static final AtomicLong atomic = new AtomicLong(0);
    /**
     * 序列号
     */
    private final long sequenceNum;

    public Task(long realTime, long timeout, RequestTask requestTask) {
        this.realTime = realTime;
        this.timeOut = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(timeout, TimeUnit.MILLISECONDS);
        this.requestTask = requestTask;
        this.sequenceNum = atomic.getAndIncrement();
    }

    public long getRealTime() {
        return realTime;
    }

    public RequestTask getRequestTask() {
        return requestTask;
    }

    /**
     * 返回与此对象相关的剩余延迟时间，以给定的时间单位表示
     */
    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(this.timeOut - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * 元素的先后顺序
     * @param other
     * @return
     */
    @Override
    public int compareTo(Delayed other) {
        if (other == this) // compare zero ONLY if same object
            return 0;
        if (other instanceof Task) {
            Task x = (Task) other;
            long diff = timeOut - x.timeOut;
            if (diff < 0)
                return -1;
            else if (diff > 0)
                return 1;
            else if (sequenceNum < x.sequenceNum)
                return -1;
            else
                return 1;
        }
        long d = (getDelay(TimeUnit.MILLISECONDS) - other.getDelay(TimeUnit.MILLISECONDS));
        return (d == 0) ? 0 : ((d < 0) ? -1 : 1);
    }

}
