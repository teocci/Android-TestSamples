package com.github.teocci.testing.benchmark;

import com.vmlens.executorService.Consumer;
import com.vmlens.executorService.EventSink;
import com.vmlens.executorService.internal.EventBusImpl;
import com.vmlens.executorService.internal.ProzessAllListsRunnable;
import com.vmlens.executorService.internal.ProzessOneList;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import gnu.trove.map.hash.TLongObjectHashMap;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2019-Jun-19
 */
public class BlockingQueueBenchmark
{
    private static final int WRITING_THREAD_COUNT = 5;
    private static final int VMLENS_QUEUE_LENGTH = 1000;
    private static final int JDK_QUEUE_LENGTH = 4000;

    EventBusImpl eventBus;
    Consumer consumer;
    ProzessAllListsRunnable prozess;
    TLongObjectHashMap<ProzessOneList> threadId2ProzessOneRing;
    LinkedBlockingQueue jdkQueue;

    private long jdkCount = 0;
    private long vmlensCount = 0;

    public void setup()
    {
        eventBus = new EventBusImpl();
        consumer = eventBus.newConsumer();
        prozess = new ProzessAllListsRunnable<> (
                new EventSink()
                {
                    public void execute(Object event)
                    {
                        vmlensCount++;
                    }

                    public void close() {}

                    public void onWait() {}
                },
                eventBus
        );
        threadId2ProzessOneRing = new TLongObjectHashMap<>();
        jdkQueue = new LinkedBlockingQueue(JDK_QUEUE_LENGTH);
    }

    public void offerVMLens()
    {
        consumer.accept("event");
    }

    public void takeVMLens()
    {
        prozess.run();
    }

    public void offerJDK()
    {
        try {
            jdkQueue.put("event");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void takeJDK()
    {
        try {
            jdkQueue.poll(100, TimeUnit.SECONDS);
            jdkCount++;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void printCounts()
    {
        System.out.println("jdkCount " + jdkCount);
        System.out.println("vmlensCount " + vmlensCount);
    }
}
