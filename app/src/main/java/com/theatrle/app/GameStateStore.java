package com.theatrle.app;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Arrays;

/** Keeps the puzzle and in-progress input through rotation and process recreation. */
public final class GameStateStore {
    private final SharedPreferences preferences;
    public GameStateStore(Context context) { preferences=context.getSharedPreferences("game",Context.MODE_PRIVATE); }
    public void save(GameEngine game) {
        StringBuilder history=new StringBuilder();
        for(String guess:game.getGuesses()) { if(history.length()>0)history.append(','); history.append(guess); }
        preferences.edit().putString("title",game.getTitle()).putString("history",history.toString())
                .putString("input",game.getCurrent()).apply();
    }
    public void restore(GameEngine game) {
        String title=preferences.getString("title","");
        if(!title.isEmpty())game.restore(title,Arrays.asList(preferences.getString("history","").split(",")),preferences.getString("input",""));
    }
}
