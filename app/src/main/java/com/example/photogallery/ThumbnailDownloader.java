package com.example.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.LruCache;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by merz_konstantin on 6/8/17.
 */

public class ThumbnailDownloader<T> extends HandlerThread { // klasse hat einzelnes generisches argument -- wird ein PhotoHolder sein
    private static final String TAG="ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD=0; // das 'what'-feld der nachricht (zum sortieren fuer den handler, was gemacht werden muss (naemlich was runterladen)
    private static final int MESSAGE_PRELOAD=1;
    private static final int CACHE_SIZE=16*1024*2014; // groesse des caches=16MB

    private boolean mHasQuit=false;
    // Handler fuer die kommunikation innerhalb des threads
    private Handler mRequestHandler;
    private ConcurrentMap<T,String> mRequestMap=new ConcurrentHashMap<>(); // zu jedem photoholder gibts eine url
    private ConcurrentMap<T,String> mRequestMap2=new ConcurrentHashMap<>();
    // Handler fuer den Hauptthread (wird diesem dann auch zugeordnet) wir koennen jedoch natuerlich hier auf diesen zugreifen und zum kommunizieren verwenden
    private Handler mResponseHandler;
    private ThumbnailDownloadListener<T> mThumbnailDownloadListener;
    private LruCache<String,Bitmap> mLruCache;

    public interface ThumbnailDownloadListener<T>{
        void onThumbnailDownloaded(T target, Bitmap thumbnail); // wird aufgerufen, wenn ein bild vollstaendig runtergeladen wurde
    }

    public void setThumbnailDownloadListener(ThumbnailDownloadListener<T> listener){
        mThumbnailDownloadListener=listener;
    }

    public ThumbnailDownloader(Handler responseHandlerThumbnail){
        super(TAG);
        mResponseHandler=responseHandlerThumbnail;
        mLruCache=new LruCache<>(CACHE_SIZE);
    }

    @Override
    protected void onLooperPrepared(){ // methode wird vor dem ersten checken der schlange aufgerufen
        mRequestHandler=new Handler(){
            @Override
            public void handleMessage(Message msg){
                T target=(T) msg.obj; // extrahieren des "anhangs" der nachricht (das target, der PhotoHolder)
                switch (msg.what){
                    case MESSAGE_DOWNLOAD:
                        Log.i(TAG, "Got a request for URL: " + mRequestMap.get(target));
                        handleRequest(target);
                        break;

                    case MESSAGE_PRELOAD:
                        String url=(String) msg.obj;
                        downloadImage(url);
                        break;
                }
            }
        };
    }

    @Override
    public boolean quit(){ // methode wird vom hauptthread aufgerufen, wenn er zerstoert wird. damit wird auch dieser thread zerstoert
        mHasQuit=true;
        clearCache();
        return super.quit();
    }

    // wird vom hauptthread aufgerufen, verbindet die url mit dem photoholder und schickt eine nachricht innerhalb des threads zum ausfuehren von weiteren massnahmen
    public void queueThumbnail(T target, String url){ // erwartet ein Objekt vom Typ T, welches als identifier fuer den download benutzt wird
        Log.i(TAG, "Got a url: " + url);

        if(url==null){
            mRequestMap.remove(target);
        } else{
            mRequestMap.put(target,url);
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD,target).sendToTarget(); // neue nachricht fuer unseren handler wird erstellt. auftrag lautet runterladen.
            // mitgeschickt wird das ziel-objekt, der identifier des downloads. hier wird der photoholder mitgeschickt, in den was gespeichert wird
            // obtainMessage legt auch das ziel der nachricht fest, naemlich das aufrufende objekt, sprich mRequestHandler
            // in der nachricht steckt nicht die url, sondern der entsprechende photoholder (dafuer haben wir mRequestMap)
        }
    }


    // wird vom hauptthread aufgerufen, cached ein foto und schickt eine nachricht innerhalb des threads zum ausfuehren von weiteren massnahmen
    public void preloadImage(String url){
        mRequestHandler.obtainMessage(MESSAGE_PRELOAD,url).sendToTarget();
    }

    public Bitmap getCachedImage(String url){
        return mLruCache.get(url);
    }

    public void clearQueue(){ // falls geraet rotiert wird, raeumen wir auf, da ThumbnailDownloader evtl mit falschen PhotoHolders verknuepft sein koennte
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
        mRequestHandler.removeMessages(MESSAGE_PRELOAD);
        mRequestMap.clear();
    }

    public void clearCache(){
        mLruCache.evictAll();
    }

    private void handleRequest(final T target){ // methode, die der handler ausfuehrt, wenn er eine nachricht erhaelt
        try{
            final String url=mRequestMap.get(target); // url des entsprechenden photoholders wird extrahiert
            if(url==null){
                return;
            }

            byte[] bitmapBytes=new FlickrFetchr().getUrlBytes(url);
            final Bitmap bitmap= BitmapFactory.decodeByteArray(bitmapBytes,0,bitmapBytes.length);
            mLruCache.put(url,bitmap);
            Log.i(TAG,"Bitmap created");

            mResponseHandler.post(new Runnable() { // es wird eine nachricht an den handler des mainthreads geschickt, der den folgenden code (im mainthread!) ohne weiteres ausfuehrt
                @Override
                public void run() {
                    if(mRequestMap.get(target)!=url || mHasQuit){
                        return;
                    }
                    mRequestMap.remove(target);
                    mThumbnailDownloadListener.onThumbnailDownloaded(target,bitmap);
                }
            });
        } catch (IOException ioe){
            Log.e(TAG,"Error downloading image", ioe);
        }
    }

    private Bitmap downloadImage(String url){
        if(url==null){ return null;}
        Bitmap bitmap=mLruCache.get(url);
        // wenn bitmap im cache ist, einfach zurueckgeben
        if(bitmap!=null){
            return bitmap;
        }

        try{
            byte[] bitmapBytes=new FlickrFetchr().getUrlBytes(url);
            bitmap=BitmapFactory.decodeByteArray(bitmapBytes,0,bitmapBytes.length);
            mLruCache.put(url,bitmap);
            Log.i(TAG,"Downloaded and cached image: " + url);
            return bitmap;
        } catch (IOException ioe){
            Log.e(TAG,"Error downloading image", ioe);
            return null;
        }
    }
}
