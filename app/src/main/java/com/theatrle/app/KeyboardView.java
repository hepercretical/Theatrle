package com.theatrle.app;

import android.content.Context;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import java.util.HashMap;

/** Native buttons supply accurate hit regions, press feedback and accessibility. */
@android.annotation.SuppressLint("ViewConstructor") // Programmatic-only component requiring an input listener.
public final class KeyboardView extends LinearLayout {
    public interface Listener { void letter(char key); void delete(); void submit(); }
    private final GameEngine game;
    private final Listener listener;
    private final HashMap<Character,Button> buttons=new HashMap<>();
    private boolean numbers;
    public KeyboardView(Context context, GameEngine game, Listener listener) {
        super(context); this.game=game; this.listener=listener;
        setOrientation(VERTICAL); setPadding(UiStyle.dp(context,3),0,UiStyle.dp(context,3),UiStyle.dp(context,4));
        rebuild();
    }
    private void rebuild() {
        removeAllViews(); buttons.clear();
        String[] rows=numbers?new String[]{"1234567890"}:new String[]{"QWERTYUIOP","ASDFGHJKL","ZXCVBNM"};
        for (String letters:rows) {
            LinearLayout row=new LinearLayout(getContext()); row.setGravity(Gravity.CENTER);
            addView(row,new LayoutParams(LayoutParams.MATCH_PARENT,UiStyle.dp(getContext(),56)));
            for (char key:letters.toCharArray()) {
                Button button=button(String.valueOf(key)); buttons.put(key,button);
                button.setOnClickListener(v->listener.letter(key));
                LayoutParams params=new LayoutParams(0,LayoutParams.MATCH_PARENT,1);
                params.setMargins(UiStyle.dp(getContext(),2),UiStyle.dp(getContext(),3),UiStyle.dp(getContext(),2),UiStyle.dp(getContext(),3));
                row.addView(button,params);
            }
        }
        LinearLayout actions=new LinearLayout(getContext());
        addView(actions,new LayoutParams(LayoutParams.MATCH_PARENT,UiStyle.dp(getContext(),56)));
        Button toggle=button(numbers?"ABC":"123"), enter=button("ENTER"), delete=button("DELETE");
        toggle.setOnClickListener(v->{numbers=!numbers; rebuild();});
        enter.setOnClickListener(v->listener.submit()); delete.setOnClickListener(v->listener.delete());
        for (Button button:new Button[]{toggle,enter,delete}) {
            LayoutParams params=new LayoutParams(0,LayoutParams.MATCH_PARENT,1);
            params.setMargins(UiStyle.dp(getContext(),2),UiStyle.dp(getContext(),3),UiStyle.dp(getContext(),2),UiStyle.dp(getContext(),3));
            actions.addView(button,params);
        }
        refresh();
    }
    private Button button(String text) {
        Button button=new Button(getContext()); button.setText(text); button.setTextSize(text.length()>1?14:20);
        button.setTextColor(UiStyle.TEXT); button.setMinWidth(0); button.setMinimumWidth(0);
        button.setMinHeight(0); button.setMinimumHeight(0); button.setPadding(0,0,0,0);
        button.setBackground(UiStyle.background(getContext(),UiStyle.TILE)); button.setContentDescription(text);
        return button;
    }
    public void refresh() {
        for (java.util.Map.Entry<Character,Button> entry:buttons.entrySet()) {
            Integer state=game.getKeyState(entry.getKey());
            entry.getValue().setBackground(UiStyle.background(getContext(),state==null?UiStyle.TILE:UiStyle.color(state)));
        }
    }
}
