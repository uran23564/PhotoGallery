package com.example.photogallery;

import android.graphics.Bitmap;

/**
 * Created by merz_konstantin on 6/5/17.
 */

public class GalleryItem {
    private String mCaption; // Ueberschrift des Bildes
    private String mId; // ID des Bildes
    private String mUrl; // URL des Bildes
    private Bitmap mBitmap; // nur fuer die fetten Bilder



    @Override
    public String toString(){
        return mCaption;
    }

    // getter
    public String getCaption(){ return mCaption;}
    public String getId(){ return mId;}
    public String getUrl(){ return mUrl;}
    public Bitmap getBitmap(){ return mBitmap;}

    // setter
    public void setCaption(String caption){ mCaption=caption;}
    public void setId(String id){mId=id;}
    public void setUrl(String url){ mUrl=url;}
    public void setBitmap(Bitmap bitmap){ mBitmap=bitmap;}

    public void copyItem(GalleryItem item){
        mCaption=item.getCaption();
        mId=item.getId();
        mUrl=item.getUrl();
        mBitmap=item.getBitmap();
    }

}
