package com.example.photogallery;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by merz_konstantin on 6/15/17.
 */

public class PhotoDownloader<T> extends HandlerThread {
    private static final String TAG="PhotoDownloader";
    private static final int MESSAGE_FULL_PICTURE=0;

    private volatile boolean mHasQuit=false;
    private byte[] mBytes=null;
    // Handler fuer die kommunikation innerhalb des threads
    private Handler mRequestHandler;
    private ConcurrentMap<T,String> mRequestMap=new ConcurrentHashMap<>(); // zu jedem photoholder gibts eine url
    // Handler fuer den Hauptthread (wird diesem dann auch zugeordnet) wir koennen jedoch natuerlich hier auf diesen zugreifen und zum kommunizieren verwenden
    private Handler mResponseHandler;
    private PhotoDownloader.PhotoDownloadListener<T> mPhotoDownloadListener;
    private volatile boolean mHasDownloaded=false;

    public interface PhotoDownloadListener<T>{
        void onPhotoDownloaded(T target, byte[] bytes);
    }

    public void setPhotoDownloadListener(PhotoDownloader.PhotoDownloadListener<T> listener){
        mPhotoDownloadListener=listener;
    }

    public PhotoDownloader(Handler responseHandlerPhoto){
        super(TAG);
        mResponseHandler=responseHandlerPhoto;
    }

    @Override
    protected void onLooperPrepared(){ // methode wird vor dem ersten checken der schlange aufgerufen
        mRequestHandler=new Handler(){
            @Override
            public void handleMessage(Message msg){
                T target=(T) msg.obj;
                switch (msg.what){
                    case MESSAGE_FULL_PICTURE:
                        handleRequest(target);
                        break;
                }
            }
        };
    }

    @Override
    public boolean quit(){ // methode wird vom hauptthread aufgerufen, wenn er zerstoert wird. damit wird auch dieser thread zerstoert
        mHasQuit=true;
        return super.quit();
    }


    public void queueFullPhoto(T target,String url){
        mHasDownloaded=false;
        if(url==null){
            mRequestMap.remove(target);
            return;
        } else{
            mRequestMap.put(target,url);
            mRequestHandler.obtainMessage(MESSAGE_FULL_PICTURE,target).sendToTarget();
        }
    }


    public void clearQueue(){ // falls geraet rotiert wird, raeumen wir auf, da ThumbnailDownloader evtl mit falschen PhotoHolders verknuepft sein koennte
        mRequestHandler.removeMessages(MESSAGE_FULL_PICTURE);
        mRequestMap.clear();
    }


    // herunterladen eines ganzen photos
    private void handleRequest(final T target){
        try{
            final String url=mRequestMap.get(target);
            if(url==null){
                return;
            }
            final byte[] bitmapBytes=new FlickrFetchr().getFullPhotoBytes(url);
            mResponseHandler.postAtFrontOfQueue(new Runnable() {
                @Override
                public void run() {
                    if(mRequestMap.get(target)!=url || mHasQuit){
                        return;
                    }
                    mRequestMap.remove(target);
                    mPhotoDownloadListener.onPhotoDownloaded(target,bitmapBytes);
                }
            });
            mBytes= bitmapBytes;
            mHasDownloaded=true;
        } catch (IOException ioe){
            Log.e(TAG,"Error downloading image", ioe);
        }
    }

    public byte[] getBytes(){ return mBytes;}
    public boolean getHasDownloaded(){ return mHasDownloaded;}
}
