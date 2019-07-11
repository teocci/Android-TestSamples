package com.github.teocci.testing.ui;

import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;

import com.github.teocci.testing.R;
import com.github.teocci.testing.ntp.AtomicTime;
import com.github.teocci.testing.ntp.AtomicTimeSingleton;
import com.github.teocci.testing.utils.LogHelper;
import com.github.teocci.testing.views.ClockTextView;

import org.apache.commons.net.ntp.TimeStamp;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2019-Jul-08
 */
public class NTPActivity extends AppCompatActivity
{
    private static String TAG = LogHelper.makeLogTag(NTPActivity.class);

    private long ONE_SECOND = 1_000L;
    private long TENTH_OF_SECOND = ONE_SECOND / 10;

    private long passedMilliseconds = 0;
    private long oldTS, newTS;
    private long diff;

    private final Handler updateHandler = new Handler();

    private AtomicTime atomicTime;
    private ClockTextView clockTextView;

    /**
     * Called when the activity is first created.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ntp);

        atomicTime = AtomicTimeSingleton.getInstance();

        clockTextView = (ClockTextView) findViewById(R.id.clock_time);
        updateCurrentTime();
    }

    private void updateCurrentTime()
    {
        long hours = (passedMilliseconds / 3_600_000) % 24;
        long minutes = (passedMilliseconds / 60_000) % 60;
        long seconds = passedMilliseconds % 60;

        clockTextView.setText(getFormattedTime(passedMilliseconds, true, true));

        if (seconds == 0) {
            if (hours == 0 && minutes == 0) {
                oldTS = System.nanoTime();
                passedMilliseconds = getPassedMilliseconds();

                TimeStamp currentNtpTime = TimeStamp.getNtpTime(passedMilliseconds);
                LogHelper.e(TAG, "Atomic time:\t" + currentNtpTime + " -> " + currentNtpTime.toDateString());
            }
        }

        updateHandler.postDelayed(
                () -> {
                    newTS = System.nanoTime();
                    diff = (newTS - oldTS) / 1_000_000L;
                    if (diff >= ONE_SECOND) {
                        passedMilliseconds += diff;
                        oldTS = newTS;
                    }

                    updateCurrentTime();
                },
                TENTH_OF_SECOND
        );
    }

    public SpannableString getFormattedTime(long passedMilliseconds, boolean showSeconds, boolean use24HourFormat)
    {
        long hours = (passedMilliseconds / 3_600_000) % 24;
        long minutes = (passedMilliseconds / 60_000) % 60;
        long seconds = (passedMilliseconds / 1_000) % 60;

        return new SpannableString(formatTime(showSeconds, use24HourFormat, hours, minutes, seconds));
    }

    public String formatTime(boolean showSeconds, boolean use24HourFormat, long hours, long minutes, long seconds)
    {
        String hoursFormat = use24HourFormat ? "%02d" : "%01d";
        String format = hoursFormat + ":%02d";

        if (showSeconds) {
            format += ":%02d";
            return String.format(Locale.KOREA, format, hours, minutes, seconds);
        } else {
            return String.format(Locale.KOREA, format, hours, minutes);
        }
    }

    public long getPassedMilliseconds()
    {
        boolean inDaylightTime = TimeZone.getDefault().inDaylightTime(new Date());
        TimeZone timeZone = TimeZone.getDefault();
        int rawOffset = timeZone.getRawOffset();
        if (inDaylightTime) {
            rawOffset += timeZone.getDSTSavings();
        }

        return atomicTime.getTime() + rawOffset;
    }
}