package com.theatrle.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Optional background-only poster lookup, isolated from game state and scoring. */
public final class PosterRepository {
    public static final class Poster {
        public final Bitmap bitmap;
        public final String sourceUrl;
        Poster(Bitmap bitmap, String sourceUrl) { this.bitmap=bitmap; this.sourceUrl=sourceUrl; }
    }
    private static final String WIKIDATA="https://www.wikidata.org/w/api.php?format=json&maxlag=5&";
    private static final String WIKIPEDIA="https://en.wikipedia.org/w/api.php?format=json&formatversion=2&maxlag=5&";
    private static final LruCache<String,Poster> CACHE=new LruCache<String,Poster>(4*1024*1024) {
        @Override protected int sizeOf(String key,Poster value) { return value.bitmap.getByteCount(); }
    };

    /** Returns null for unavailable posters, bad responses, cancellation or no internet. */
    public Poster find(Context context,String title) {
        try {
            String imdbId=findImdbId(context,title);
            if(imdbId==null)return null;
            Poster cached=CACHE.get(imdbId);
            if(cached!=null)return cached;
            // Resolve by stable ID, rather than matching a title to an arbitrary search result.
            JSONObject search=json(WIKIDATA+"action=query&list=search&srnamespace=0&srlimit=1&srsearch="+encode("haswbstatement:P345="+imdbId));
            JSONObject query=search.optJSONObject("query");
            JSONArray matches=query==null?null:query.optJSONArray("search");
            if(matches==null||matches.length()==0)return null;
            String entityId=matches.getJSONObject(0).optString("title");
            if(!entityId.matches("Q[0-9]+"))return null;
            JSONObject data=json(WIKIDATA+"action=wbgetentities&ids="+entityId+"&props=sitelinks&sitefilter=enwiki");
            JSONObject entities=data.optJSONObject("entities");
            JSONObject entity=entities==null?null:entities.optJSONObject(entityId);
            JSONObject links=entity==null?null:entity.optJSONObject("sitelinks");
            JSONObject article=links==null?null:links.optJSONObject("enwiki");
            if(article==null)return null;
            JSONObject pageData=json(WIKIPEDIA+"action=query&prop=pageimages&redirects=1&piprop=name%7Cthumbnail&pilicense=any&pithumbsize=400&titles="+encode(article.getString("title")));
            JSONObject pageQuery=pageData.optJSONObject("query");
            JSONArray pages=pageQuery==null?null:pageQuery.optJSONArray("pages");
            if(pages==null||pages.length()!=1)return null;
            JSONObject page=pages.getJSONObject(0),thumbnail=page.optJSONObject("thumbnail");
            String filename=page.optString("pageimage");
            if(thumbnail==null||filename.isEmpty())return null;
            // PageImages can also select logos or stills. Only display an identified poster.
            if(!filename.toLowerCase(Locale.ROOT).contains("poster")&&!describesPoster(filename))return null;
            byte[] bytes=download(thumbnail.getString("source"),4*1024*1024);
            BitmapFactory.Options options=new BitmapFactory.Options(); options.inJustDecodeBounds=true;
            BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
            if(options.outWidth<=0||options.outHeight<=0)return null;
            options.inSampleSize=1;
            while(Math.max(options.outWidth,options.outHeight)/options.inSampleSize>600)options.inSampleSize*=2;
            options.inJustDecodeBounds=false;
            Bitmap bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
            if(bitmap==null)return null;
            Poster result=new Poster(bitmap,"https://en.wikipedia.org/wiki/"+encode("File:"+filename));
            CACHE.put(imdbId,result);
            return result;
        } catch(Exception unavailable) {
            // Posters are optional: a network/parser/resource error must never affect the game.
            return null;
        }
    }

    private String findImdbId(Context context,String title) throws IOException {
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(
                context.getResources().openRawResource(R.raw.film_metadata),StandardCharsets.UTF_8))) {
            reader.readLine(); String line;
            while((line=reader.readLine())!=null) {
                int tab=line.indexOf('\t');
                if(tab>0&&new MovieTitle(line.substring(tab+1)).display.equals(title)) {
                    String id=line.substring(0,tab);
                    return id.matches("tt[0-9]+")?id:null;
                }
            }
        }
        return null;
    }

    private boolean describesPoster(String filename) throws Exception {
        JSONObject data=json(WIKIPEDIA+"action=query&prop=imageinfo%7Ccategories&iiprop=extmetadata&cllimit=max&titles="+encode("File:"+filename));
        JSONObject query=data.optJSONObject("query");
        JSONArray pages=query==null?null:query.optJSONArray("pages");
        if(pages==null||pages.length()!=1)return false;
        JSONArray categories=pages.getJSONObject(0).optJSONArray("categories");
        if(categories!=null)for(int i=0;i<categories.length();i++) {
            String category=categories.getJSONObject(i).optString("title").toLowerCase(Locale.ROOT);
            if(category.contains("film posters")||category.contains("movie posters"))return true;
        }
        JSONArray info=pages.getJSONObject(0).optJSONArray("imageinfo");
        if(info==null||info.length()==0)return false;
        JSONObject metadata=info.getJSONObject(0).optJSONObject("extmetadata");
        JSONObject description=metadata==null?null:metadata.optJSONObject("ImageDescription");
        return description!=null&&description.optString("value").toLowerCase(Locale.ROOT).contains("poster");
    }

    private JSONObject json(String url) throws Exception {
        return new JSONObject(new String(download(url,1024*1024),StandardCharsets.UTF_8));
    }
    private static String encode(String value) throws IOException {
        return URLEncoder.encode(value,"UTF-8").replace("+","%20");
    }
    private byte[] download(String address,int limit) throws IOException {
        for(int redirects=0;redirects<4;redirects++) {
            if(Thread.currentThread().isInterrupted())throw new IOException("Poster lookup cancelled");
            URL url=new URL(address);
            String host=url.getHost();
            if(!url.getProtocol().equals("https")||!(host.equals("www.wikidata.org")||host.equals("en.wikipedia.org")||host.equals("upload.wikimedia.org")))
                throw new IOException("Unexpected poster host");
            HttpURLConnection connection=(HttpURLConnection)url.openConnection();
            connection.setConnectTimeout(5000); connection.setReadTimeout(5000); connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("User-Agent","Theatrle/1.0 (Android educational movie game; poster lookup)");
            try {
                int status=connection.getResponseCode();
                if(status==301||status==302||status==303||status==307||status==308) {
                    String location=connection.getHeaderField("Location");
                    if(location==null)throw new IOException("Missing poster redirect");
                    address=new URL(url,location).toExternalForm(); continue;
                }
                if(status!=200||connection.getContentLength()>limit)throw new IOException("Poster unavailable");
                try(InputStream input=connection.getInputStream();ByteArrayOutputStream output=new ByteArrayOutputStream()) {
                    byte[] buffer=new byte[8192]; int count;
                    while((count=input.read(buffer))!=-1) {
                        if(Thread.currentThread().isInterrupted()||output.size()+count>limit)throw new IOException("Poster cancelled or too large");
                        output.write(buffer,0,count);
                    }
                    return output.toByteArray();
                }
            } finally { connection.disconnect(); }
        }
        throw new IOException("Too many poster redirects");
    }
}
