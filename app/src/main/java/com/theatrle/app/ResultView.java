package com.theatrle.app;

import android.content.Context;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Scrollable win/loss screen: long titles and patterns cannot run off-screen. */
@android.annotation.SuppressLint("ViewConstructor") // Programmatic-only component requiring a game and callback.
public final class ResultView extends ScrollView {
    public ResultView(Context context, GameEngine game, Runnable newGame) {
        super(context); setFillViewport(true);
        LinearLayout content=new LinearLayout(context); content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL); int pad=UiStyle.dp(context,16); content.setPadding(pad,pad,pad,pad);
        addView(content);
        TextView heading=UiStyle.text(context,game.isWon()?"YOU GOT IT!":"GAME OVER",30,UiStyle.TEXT);
        heading.setGravity(Gravity.CENTER); content.addView(heading);
        TextView title=UiStyle.text(context,game.getTitle(),22,UiStyle.TEXT); title.setGravity(Gravity.CENTER);
        title.setPadding(0,pad,0,pad); content.addView(title);
        TextView count=UiStyle.text(context,game.isWon()?"Solved in "+game.getGuessCount()+" of 6 guesses":"6 of 6 guesses used",18,UiStyle.MUTED);
        content.addView(count);
        content.addView(new GuessBoardView(context,game,true),new LinearLayout.LayoutParams(-1,-2));
        Button copy=new Button(context); copy.setText(R.string.copy_results);
        copy.setOnClickListener(v->{
            ClipboardManager clipboard=(ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("Theatrle",SharePattern.create(game)));
            Toast.makeText(context,"Results copied",Toast.LENGTH_SHORT).show();
        });
        content.addView(copy,new LinearLayout.LayoutParams(-1,UiStyle.dp(context,56)));
        Button again=new Button(context); again.setText(R.string.new_game); again.setTextColor(UiStyle.TEXT);
        again.setBackground(UiStyle.background(context,UiStyle.PURPLE)); again.setOnClickListener(v->newGame.run());
        content.addView(again,new LinearLayout.LayoutParams(-1,UiStyle.dp(context,56)));
        // Appending the optional image keeps the existing results and button positions unchanged.
        if(!game.isWon())content.addView(new MoviePosterView(context,game.getTitle()),new LinearLayout.LayoutParams(-1,-2));
    }
}
