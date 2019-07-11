package com.github.teocci.testing.views;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2019-Jul-09
 */
public class ClockTextView extends AppCompatTextView
{
    public ClockTextView(Context context)
    {
        super(context);
    }

    public ClockTextView(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
    }

    public ClockTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    public void setColors(int textColor, int accentColor, int backgroundColor)
    {
        setTextColor(textColor);
        setLinkTextColor(accentColor);
    }
}
