package com.example.photogallery;

/**
 * Created by merz_konstantin on 6/5/17.
 */

public class GalleryItem {
    private String mCaption; // Ueberschrift des Bildes
    private String mId; // ID des Bildes
    private String mUrl; // URL des Bildes

    @Override
    public String toString(){
        return mCaption;
    }

    // getter
    public String getCaption(){ return mCaption;}
    public String getId(){ return mId;}
    public String getUrl(){ return mUrl;}

    // setter
    public void setCaption(String caption){ mCaption=caption;}
    public void setId(String id){mId=id;}
    public void setUrl(String url){ mUrl=url;}

}
