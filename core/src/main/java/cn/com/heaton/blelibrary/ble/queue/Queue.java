package cn.com.heaton.blelibrary.ble.queue;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.com.heaton.blelibrary.ble.BleLog;

//TODO  优化 --- 执行完后  自动退出队列  销毁线程
abstract class Queue {

    private ExecutorService executor = Executors.newSingleThreadExecutor();
    protected DelayQueue<Task> delayQueue = new DelayQueue<>();
    //当前剩余总时长
    private long lastTime = 0L;

    protected Queue() {
        executor.execute(queueTask);
    }

    private final Runnable queueTask = new Runnable() {
        @Override
        public void run() {
            while (true) {
                try {
                    //从延迟队列中取值,使用take()函数，如果队列中没有数据，则线程wait释放CPU，而poll()则不会等待，直接返回null；
                    // 同样，空间耗尽时offer()函数不会等待，直接返回false，而put()则会wait，因此如果你使用while(true)来获得队列元素，
                    // 注意千万不能用poll()，CPU会100%的。
                    Task task = delayQueue.take();
                    if (task != null) {
                        //修改问题的状态
                        RequestTask requestTask = task.getRequestTask();
                        if (requestTask != null){
                            execute(requestTask);
                            //减去当前任务时间
                            lastTime-=task.getRealTime();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    };

    public void put(RequestTask requestTask){
        long time = requestTask.getDelay();
        lastTime += time;
        //创建一个任务
        Task k = new Task(time, lastTime, requestTask);
        //将任务放在延迟的队列中
        delayQueue.put(k);
    }

    public void remove(Task task){
        delayQueue.remove(task);
    }

    public abstract void execute(RequestTask requestTask);

    public void clear(){
        delayQueue.clear();
        lastTime = 0L;
    }

    public void shutDown(){
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
            executor = null;
        }
    }

}
