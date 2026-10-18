package com.theatrle.app;

import android.app.Instrumentation;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import java.util.Collections;

/** Small device-side regression runner with no external testing dependencies. */
public final class PosterChecks extends Instrumentation {
    private boolean offline;
    @Override public void onCreate(Bundle args) {
        super.onCreate(args); offline=args!=null&&"true".equals(args.getString("offline")); start();
    }
    @Override public void onStart() {
        Bundle results=new Bundle();
        try {
            GameEngine loss=new GameEngine(Collections.singletonList("The Shawshank Redemption"));
            for(int attempt=0;attempt<6;attempt++) {
                for(int i=0;i<loss.getAnswer().length();i++)loss.addLetter('Z');
                check(loss.submit(),"Loss setup failed");
            }
            GameEngine win=new GameEngine(Collections.singletonList("The Shawshank Redemption"));
            for(char letter:win.getAnswer().toCharArray())win.addLetter(letter);
            check(win.submit()&&win.isWon(),"Win setup failed");
            runOnMainSync(()->{
                ResultView lossView=new ResultView(getTargetContext(),loss,()->{});
                ResultView winView=new ResultView(getTargetContext(),win,()->{});
                check(countPosters(lossView)==1,"Loss must contain one optional poster section");
                check(countPosters(winView)==0,"Win screen must remain unchanged");
            });
            check(loss.getGuessCount()==6&&!loss.isWon()&&loss.isFinished(),"Poster changed the loss state");
            check(win.getGuessCount()==1&&win.isWon(),"Poster changed the win state");
            PosterRepository repository=new PosterRepository();
            check(repository.find(getTargetContext(),"NOT IN THE CATALOGUE")==null,"Unknown title should have no poster");
            PosterRepository.Poster poster=repository.find(getTargetContext(),loss.getTitle());
            if(offline)check(poster==null,"Offline lookup must fail gracefully");
            else {
                check(poster!=null,"Expected an online Shawshank Redemption poster");
                check(poster.bitmap.getWidth()>0&&poster.bitmap.getHeight()>0,"Invalid image");
                check(poster.sourceUrl.contains("ShawshankRedemptionMoviePoster"),"Wrong film poster");
                check(repository.find(getTargetContext(),loss.getTitle())==poster,"Successful poster was not cached");
                PosterRepository.Poster genericName=repository.find(getTargetContext(),"THE GODFATHER");
                check(genericName!=null&&genericName.sourceUrl.contains("Godfather_ver1"),"Poster category fallback failed");
            }
            results.putString("stream","Poster checks passed ("+(offline?"offline":"online")+"): loss-only UI, unchanged game state, unknown title, network fallback/image verification.\n");
            finish(-1,results);
        } catch(Throwable failure) {
            results.putString("stream","Poster checks FAILED: "+failure+"\n"); finish(0,results);
        }
    }
    private static int countPosters(View view) {
        int total=view instanceof MoviePosterView?1:0;
        if(view instanceof ViewGroup) {
            ViewGroup group=(ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++)total+=countPosters(group.getChildAt(i));
        }
        return total;
    }
    private static void check(boolean condition,String message) { if(!condition)throw new AssertionError(message); }
}
