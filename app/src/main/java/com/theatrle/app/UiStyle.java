package com.theatrle.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.widget.TextView;

final class UiStyle {
    static final int BG = Color.rgb(16,19,29), TEXT = Color.rgb(246,247,251), MUTED = Color.rgb(173,182,202);
    static final int TILE = Color.rgb(43,48,65), PURPLE = Color.rgb(139,75,214);
    static int dp(Context c, float value) { return Math.round(value * c.getResources().getDisplayMetrics().density); }
    static int color(int state) {
        return state == GameEngine.CORRECT ? Color.rgb(34,139,83)
                : state == GameEngine.YELLOW ? Color.rgb(163,122,17)
                : state == GameEngine.PRESENT ? PURPLE : Color.rgb(66,73,89);
    }
    static GradientDrawable background(Context c, int color) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color); shape.setCornerRadius(dp(c,6)); return shape;
    }
    static TextView text(Context c, String text, int size, int color) {
        TextView view = new TextView(c); view.setText(text); view.setTextSize(size);
        view.setTextColor(color); view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }
    private UiStyle() { }
}
