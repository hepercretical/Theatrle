package com.theatrle.app;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Wires independent game, persistence, board, keyboard and result components. */
public final class MainActivity extends Activity implements KeyboardView.Listener {
    private GameEngine game;
    private GameStateStore store;
    private LinearLayout root;
    private GuessBoardView board;
    private ScrollView scroller;
    private KeyboardView keyboard;
    private TextView status;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(UiStyle.BG);
        getWindow().setNavigationBarColor(UiStyle.BG);
        // Handle every inset once, including side navigation bars and landscape cutouts.
        if(Build.VERSION.SDK_INT>=30)getWindow().setDecorFitsSystemWindows(false);
        else getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        game=new GameEngine(FilmRepository.load(this));
        store=new GameStateStore(this); store.restore(game);
        showScreen();
    }
    private void showScreen() {
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(UiStyle.BG);
        root.setOnApplyWindowInsetsListener((view,insets)->{
            int left,top,right,bottom;
            if(Build.VERSION.SDK_INT>=30) {
                android.graphics.Insets safe=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
                left=safe.left; top=safe.top; right=safe.right; bottom=safe.bottom;
            } else {
                left=insets.getSystemWindowInsetLeft(); top=insets.getSystemWindowInsetTop();
                right=insets.getSystemWindowInsetRight(); bottom=insets.getSystemWindowInsetBottom();
                if(Build.VERSION.SDK_INT>=28 && insets.getDisplayCutout()!=null) {
                    left=Math.max(left,insets.getDisplayCutout().getSafeInsetLeft());
                    right=Math.max(right,insets.getDisplayCutout().getSafeInsetRight());
                    top=Math.max(top,insets.getDisplayCutout().getSafeInsetTop());
                    bottom=Math.max(bottom,insets.getDisplayCutout().getSafeInsetBottom());
                }
            }
            int margin=UiStyle.dp(this,8);
            view.setPadding(left+margin,top+margin,right+margin,bottom+margin);
            return insets;
        });
        setContentView(root); root.requestApplyInsets();
        if(game.isFinished()) {
            root.addView(new ResultView(this,game,()->{game.newGame();store.save(game);showScreen();}),new LinearLayout.LayoutParams(-1,-1));
            return;
        }
        LinearLayout heading=new LinearLayout(this); heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand=UiStyle.text(this,"THEATRLE",26,UiStyle.TEXT);
        heading.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        status=UiStyle.text(this,getString(R.string.guess_count,game.getGuessCount()+1),16,UiStyle.MUTED); heading.addView(status);
        root.addView(heading);
        boolean landscape=getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE;
        LinearLayout body=new LinearLayout(this); body.setOrientation(landscape?LinearLayout.HORIZONTAL:LinearLayout.VERTICAL);
        root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout boardColumn=new LinearLayout(this); boardColumn.setOrientation(LinearLayout.VERTICAL);
        TextView hint=UiStyle.text(this,"Green: exact   Yellow: same word   Purple: other word",12,UiStyle.MUTED);
        hint.setPadding(0,UiStyle.dp(this,4),0,UiStyle.dp(this,6)); boardColumn.addView(hint);
        scroller=new ScrollView(this); scroller.setFillViewport(false);
        board=new GuessBoardView(this,game,false); scroller.addView(board);
        boardColumn.addView(scroller,new LinearLayout.LayoutParams(-1,0,1));
        body.addView(boardColumn,landscape?new LinearLayout.LayoutParams(0,-1,1):new LinearLayout.LayoutParams(-1,0,1));
        keyboard=new KeyboardView(this,game,this);
        if(landscape) {
            // Keep the keys usable even on short displays with large system fonts.
            ScrollView keyScroll=new ScrollView(this); keyScroll.addView(keyboard);
            body.addView(keyScroll,new LinearLayout.LayoutParams(0,-1,1));
        } else body.addView(keyboard,new LinearLayout.LayoutParams(-1,-2));
        revealCurrent();
    }
    private void refresh() {
        store.save(game);
        if(game.isFinished()) { showScreen(); return; }
        status.setText(getString(R.string.guess_count,game.getGuessCount()+1));
        board.refresh(); keyboard.refresh(); revealCurrent();
    }
    private void revealCurrent() {
        scroller.post(()->scroller.smoothScrollTo(0,Math.max(board.activeGuessTop(),board.activeInputBottom()-scroller.getHeight())));
    }
    @Override public void letter(char key) { game.addLetter(key); refresh(); }
    @Override public void delete() { game.removeLetter(); refresh(); }
    @Override public void submit() {
        if(!game.submit()) {
            status.setText(getString(R.string.fill_tiles,game.getAnswer().length()));
            status.announceForAccessibility(status.getText());
        } else refresh();
    }
    @Override protected void onStop() { super.onStop(); if(store!=null)store.save(game); }
    @Override public boolean onKeyUp(int code, KeyEvent event) {
        if(!game.isFinished()) {
            if(code==KeyEvent.KEYCODE_ENTER) { submit(); return true; }
            if(code==KeyEvent.KEYCODE_DEL) { delete(); return true; }
            char ch=Character.toUpperCase((char)event.getUnicodeChar());
            if(MovieTitle.isLetter(ch)) { letter(ch); return true; }
        }
        return super.onKeyUp(code,event);
    }
}
