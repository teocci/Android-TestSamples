package com.github.teocci.testing.ntp;

import com.github.teocci.testing.utils.LogHelper;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.apache.commons.net.ntp.TimeStamp;

import java.io.IOException;
import java.net.InetAddress;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2019-Jul-08
 */
public class AtomicTime extends Thread
{
    private static String TAG = LogHelper.makeLogTag(AtomicTime.class);

    private static final String SERVER_NAME = "time.bora.net";

    private volatile TimeInfo timeInfo;

    private volatile Long offset;

    public AtomicTime()
    {
        start();
    }

    public void run()
    {
        NTPUDPClient client = new NTPUDPClient();
        // We want to timeout if a response takes longer than 2 seconds
        client.setDefaultTimeout(2_000);

        try {
            InetAddress inetAddress = InetAddress.getByName(SERVER_NAME);
            TimeInfo timeInfo = client.getTime(inetAddress);
            timeInfo.computeDetails();
            if (timeInfo.getOffset() != null) {
                this.timeInfo = timeInfo;
                this.offset = timeInfo.getOffset();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            client.close();
            LogHelper.e(TAG, "Has been initialized? " + (isComputed() ? "true | Offset: " + offset : "false"));
        }
    }

    /**
     * Gets a NTP timestamp object based on a Java time.
     * Note that Java time (milliseconds) by definition has less precision
     * then NTP time (picoseconds) so converting Ntptime to Javatime and back
     * to Ntptime loses precision. For example, Tue, Dec 17 2002 09:07:24.810
     * is represented by a single Java-based time value of f22cd1fc8a, but its
     * NTP equivalent are all values from c1a9ae1c.cf5c28f5 to c1a9ae1c.cf9db22c.
     */
    public long getNTPTime()
    {
        long currentTime = System.currentTimeMillis();
        return isComputed() ? TimeStamp.getNtpTime(currentTime + offset).getTime() : System.nanoTime();
    }

    /**
     * Gets a Java time (milliseconds)
     */
    public long getTime()
    {
        long currentTime = System.currentTimeMillis();
        return currentTime + offset;
    }

    public boolean isComputed()
    {
        return timeInfo != null && offset != null;
    }
}