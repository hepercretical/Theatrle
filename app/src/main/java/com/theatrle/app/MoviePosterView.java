package com.theatrle.app;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Optional loss-screen section; stays invisible unless a verified poster loads. */
@android.annotation.SuppressLint("ViewConstructor")
public final class MoviePosterView extends LinearLayout {
    private static final ExecutorService DOWNLOADS=Executors.newFixedThreadPool(2,runnable->{
        Thread thread=new Thread(runnable,"theatrle-posters"); thread.setDaemon(true); return thread;
    });
    private final String title;
    private Future<?> request;
    private int generation;
    private boolean loaded;

    public MoviePosterView(Context context,String title) {
        super(context); this.title=title;
        setOrientation(VERTICAL); setGravity(Gravity.CENTER_HORIZONTAL); setVisibility(View.GONE);
    }
    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if(loaded)return;
        int token=++generation;
        Context application=getContext().getApplicationContext();
        request=DOWNLOADS.submit(()->{
            PosterRepository.Poster poster=new PosterRepository().find(application,title);
            if(poster!=null)post(()->{if(isAttachedToWindow()&&generation==token)show(poster);});
        });
    }
    @Override protected void onDetachedFromWindow() {
        generation++;
        if(request!=null)request.cancel(true);
        super.onDetachedFromWindow();
    }
    private void show(PosterRepository.Poster poster) {
        if(loaded)return;
        loaded=true;
        int pad=UiStyle.dp(getContext(),16); setPadding(0,pad,0,0);
        ImageView image=new ImageView(getContext());
        image.setImageBitmap(poster.bitmap); image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setContentDescription(getContext().getString(R.string.movie_poster,title));
        addView(image,new LayoutParams(LayoutParams.MATCH_PARENT,UiStyle.dp(getContext(),280)));
        TextView source=UiStyle.text(getContext(),getContext().getString(R.string.poster_source),13,UiStyle.MUTED);
        source.setGravity(Gravity.CENTER); source.setPadding(0,UiStyle.dp(getContext(),8),0,UiStyle.dp(getContext(),8));
        source.setOnClickListener(view->{
            try { getContext().startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(poster.sourceUrl))); }
            catch(ActivityNotFoundException unavailable) { /* The poster remains usable without a browser. */ }
        });
        addView(source,new LayoutParams(LayoutParams.MATCH_PARENT,UiStyle.dp(getContext(),48)));
        setVisibility(View.VISIBLE);
    }
}
