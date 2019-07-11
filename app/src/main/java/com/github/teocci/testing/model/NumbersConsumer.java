package com.github.teocci.testing.model;

import com.github.teocci.testing.utils.LogHelper;

import java.util.concurrent.BlockingQueue;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2019-Jun-17
 */
public class NumbersConsumer implements Runnable
{
    private static String TAG = LogHelper.makeLogTag(NumbersConsumer.class);

    private BlockingQueue<Integer> queue;
    private final int poisonPill;

    public NumbersConsumer(BlockingQueue<Integer> queue, int poisonPill)
    {
        this.queue = queue;
        this.poisonPill = poisonPill;
    }

    public void run()
    {
        try {
            while (true) {
                Integer number = queue.take();
                if (number.equals(poisonPill)) {
                    return;
                }
                LogHelper.e(TAG, Thread.currentThread().getName() + " result: " + number);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}