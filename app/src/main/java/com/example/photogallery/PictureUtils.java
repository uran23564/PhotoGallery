package com.example.photogallery;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;

/**
 * Created by merz_konstantin on 5/20/17.
 */

public class PictureUtils {
    // skaliert das erzeugte bitmap aus der originaldatei ausgehend von der groesse des PhotoViews
    public static Bitmap getScaledBitmap(byte[] byteArray, int destWidth, int destHeight){
        // Dimensionen des gespeicherten Bilds auslesen
        BitmapFactory.Options options=new BitmapFactory.Options();
        options.inJustDecodeBounds=true;
        BitmapFactory.decodeByteArray(byteArray,0,byteArray.length,options);

        float srcWidth=options.outWidth;
        float srcHeight=options.outHeight;

        // Algorythmus zum Runterskalieren
        int inSampleSize=1; // wie gross ist jedes "sample" fuer jeden pixel (samplesize=s hat einen horizontalen pixel fuer s horizontale pixel der originaldatei
        if(srcHeight>destHeight || srcWidth>destWidth){
            float heightScale=srcHeight/destHeight;
            float widthScale=srcWidth/destWidth;

            inSampleSize=Math.round(heightScale>widthScale ? heightScale:widthScale);
        }

        options=new BitmapFactory.Options();
        options.inSampleSize=inSampleSize;

        Bitmap bitmap=BitmapFactory.decodeByteArray(byteArray,0,byteArray.length,options);
        // TODO: verfeinern....
        Bitmap bitmap1=Bitmap.createScaledBitmap(bitmap,1000,1000,false);
        // einlesen des bilds und erstellen des finalen bitmap-objekts
        // return BitmapFactory.decodeByteArray(byteArray,0,byteArray.length,options);
        return bitmap1;
    }

    // erzeugt skaliertes bitmap fuer eine bestimmte groesse der activity
    public static Bitmap getScaledBitmap(byte[] byteArray, Activity activity){
        Point size=new Point();
        activity.getWindowManager().getDefaultDisplay().getSize(size); // checked, wie gross der screen ist (brutale abschaetzung)
        return getScaledBitmap(byteArray, size.x,size.y);
    }
}
