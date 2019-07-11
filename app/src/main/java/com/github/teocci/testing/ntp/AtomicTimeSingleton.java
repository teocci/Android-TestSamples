package com.github.teocci.testing.ntp;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2019-Jul-08
 */
public class AtomicTimeSingleton
{
    private static volatile AtomicTime instance;
    private static final Object mutex = new Object();

    private AtomicTimeSingleton() {}

    public static AtomicTime getInstance()
    {
        AtomicTime result = instance;
        if (result == null) {
            synchronized (mutex) {
                result = instance;
                if (result == null)
                    instance = result = new AtomicTime();
            }
        }

        return result;
    }
}