package com.github.teocci.testing.model;

import android.os.Build;
import androidx.annotation.RequiresApi;

import com.github.teocci.testing.utils.LogHelper;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2019-Jun-17
 */
public class NumbersProducer implements Runnable
{
    private static String TAG = LogHelper.makeLogTag(NumbersProducer.class);

    private BlockingQueue<Integer> numbersQueue;
    private final int poisonPill;
    private final int poisonPillPerProducer;

    public NumbersProducer(BlockingQueue<Integer> numbersQueue, int poisonPill, int poisonPillPerProducer)
    {
        this.numbersQueue = numbersQueue;
        this.poisonPill = poisonPill;
        this.poisonPillPerProducer = poisonPillPerProducer;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void run()
    {
        try {
            generateNumbers();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void generateNumbers() throws InterruptedException
    {
        for (int i = 0; i < 100; i++) {
            int number = ThreadLocalRandom.current().nextInt(100);
            numbersQueue.put(number);
            LogHelper.e(TAG, Thread.currentThread().getName() + " generated: " + number);
        }
        for (int j = 0; j < poisonPillPerProducer; j++) {
            numbersQueue.put(poisonPill);
        }
    }
}