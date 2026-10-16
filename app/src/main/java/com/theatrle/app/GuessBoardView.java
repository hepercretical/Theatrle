package com.theatrle.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;
import java.util.List;

/** One scrollable board, also used for the compact post-game pattern. */
@android.annotation.SuppressLint("ViewConstructor") // Programmatic-only component requiring a game.
public final class GuessBoardView extends View {
    private final GameEngine game;
    private final boolean pattern;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private TitleLayout layout;
    private float tile, gap, lineHeight, guessHeight;
    public GuessBoardView(Context context, GameEngine game, boolean pattern) {
        super(context); this.game = game; this.pattern = pattern;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }
    public void refresh() { requestLayout(); invalidate(); }
    public int activeGuessTop() { return (int)(game.getGuessCount() * guessHeight); }
    public int activeInputBottom() {
        int index=Math.min(game.getCurrent().length(),game.getAnswer().length()-1), row=0;
        if(layout!=null)for(TitleLayout.Cell cell:layout.cells)if(cell.letterIndex==index){row=cell.row;break;}
        return activeGuessTop()+(int)((row+1)*lineHeight)+UiStyle.dp(getContext(),4);
    }
    private int count() { return game.getGuessCount() + (!pattern && !game.isFinished() ? 1 : 0); }
    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        tile = UiStyle.dp(getContext(), pattern ? 18 : 48);
        gap = UiStyle.dp(getContext(), pattern ? 3 : 5);
        float inset = UiStyle.dp(getContext(),8);
        layout = TitleLayout.create(game.getMovie(), Math.max(tile,width-2*inset), tile, gap, UiStyle.dp(getContext(),pattern?10:24));
        lineHeight = tile + UiStyle.dp(getContext(),pattern?4:8);
        guessHeight = layout.rows * lineHeight + UiStyle.dp(getContext(),pattern?12:20);
        setMeasuredDimension(width, (int)(count()*guessHeight + inset));
        setContentDescription(pattern ? SharePattern.create(game) : "Guess " + Math.min(6,game.getGuessCount()+1) + " of 6. " + game.getCurrent());
    }
    @Override protected void onDraw(Canvas canvas) {
        if (layout == null) return;
        List<String> guesses = game.getGuesses();
        paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(UiStyle.dp(getContext(),pattern?10:23));
        for (int row=0; row<count(); row++) {
            String guess = row<guesses.size()?guesses.get(row):game.getCurrent();
            int[] states = row<guesses.size()?game.evaluate(guess):null;
            for (TitleLayout.Cell cell:layout.cells) {
                float x=(getWidth()-layout.widths.get(cell.row))/2+cell.x;
                float y=UiStyle.dp(getContext(),4)+row*guessHeight+cell.row*lineHeight;
                boolean free=cell.letterIndex<0;
                char letter=free?cell.display:cell.letterIndex<guess.length()?guess.charAt(cell.letterIndex):' ';
                if (!free) {
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(states==null?UiStyle.TILE:UiStyle.color(states[cell.letterIndex]));
                    canvas.drawRoundRect(x,y,x+tile,y+tile,UiStyle.dp(getContext(),4),UiStyle.dp(getContext(),4),paint);
                    if (states==null) {
                        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(UiStyle.dp(getContext(),1));
                        paint.setColor(UiStyle.MUTED); canvas.drawRoundRect(x,y,x+tile,y+tile,4,4,paint);
                        paint.setStyle(Paint.Style.FILL);
                    }
                }
                if (letter!=' ' && !pattern) {
                    paint.setColor(free?UiStyle.MUTED:UiStyle.TEXT);
                    canvas.drawText(String.valueOf(letter),x+tile/2,y+tile/2-(paint.ascent()+paint.descent())/2,paint);
                }
            }
        }
    }
}
