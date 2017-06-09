package com.example.photogallery;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by merz_konstantin on 5/28/17.
 */

public class PhotoGalleryFragment extends Fragment {
    private static final String TAG="PhotoGalleryFragment";
    private static final int CACHE_SIZE=4*1024*2014; // groesse des caches=4MB

    private RecyclerView mPhotoRecyclerView;
    private android.support.v7.widget.GridLayoutManager mGridLayoutManager;
    private TextView mCurrentPageTextView;
    
    private List<GalleryItem> mItems=new ArrayList<>();
    // private RecyclerView.OnScrollListener mScrollListener;
    private int mPages=1; // wir starten mit einer seite
    private int mCurrentPage=1; // gerade angesehene Seite
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;
    private LruCache<PhotoHolder,Bitmap> mBitmapCache=new LruCache<PhotoHolder,Bitmap>(CACHE_SIZE){
        protected int sizeOf(PhotoHolder key, Bitmap value){
            return value.getByteCount();
        }
    }
    
    public static PhotoGalleryFragment newInstance(){
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        new FetchItemsTask().execute(mPages); // startet den async-task

        Handler responseHandler=new Handler(); // handler gehoert dem mainthread

        mThumbnailDownloader=new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setThumbnailDownloadListener(new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
            @Override
            public void onThumbnailDownloaded(PhotoHolder photoHolder, Bitmap bitmap) {
                Drawable drawable=new BitmapDrawable(getResources(), bitmap);
                photoHolder.bindDrawable(drawable);
                mBitmapCache.put(photoHolder,drawable); // aktuelle fotos sollen schon mal gecached werden
            }
        });
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper(); // sichergehen, dass wir alles noetige beisammen haben
        Log.i(TAG, "Background thread started");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View v=inflater.inflate(R.layout.fragment_photo_gallery,container,false);
        mPhotoRecyclerView=(RecyclerView) v.findViewById(R.id.photo_recycler_view);
        mCurrentPageTextView=(TextView) v.findViewById(R.id.current_page_text_view);
        updatePageNumber();
        
        int itemHeight=mPhotoRecyclerView.getAdapter().getItemHeight();
        
        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(),3));
        mGridLayoutManager=(GridLayoutManager)mPhotoRecyclerView.getLayoutManager();
        mPhotoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener(){
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState){
                // passt so
            }
            
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy){
                if(dy>0){
                    if(mGridLayoutManager.findLastVisibleItemPosition()>=(mItems.size()-1)){
                        if(mItems.size()<=mPages*100 && (mPages-1)*100<=mItems.size()+(mPages%3)) { // erst dann zeug laden, wenn die vorigen fotos alle heruntergeladen wurden (sinnvoll??)
                        mPages++;
                        mCurrentPage++;
                            new FetchItemsTask().execute(mCurrentPage);
                        }
                    }
                    else if(mGridLayoutManager.findLastVisibleItemPosition()>=(mCurrentPage)*100){
                        mCurrentPage++;
                    }
                }
                if(dy>itemHeight){ // naechsten 3 fotos schon mal in den cache laden
                    cacheNextPhotos(3,mGridLayoutManager.findLastVisibleItemPosition());
                }

                else if(dy<0 && mGridLayoutManager.findFirstVisibleItemPosition()<=(mCurrentPage-1)*100 && mGridLayoutManager.findFirstVisibleItemPosition()!=0){ // stellt sicher, dass wir auf der richtigen seite sind
                    mCurrentPage--;
                }
                updatePageNumber();
            }
        });
        
        setupAdapter();
        
        return v;
    }

    private void setupAdapter(){
        if(isAdded()){ // checkt, ob fragment schon an die activity angehaengt wurde, da fragments theoretisch auch ohne activities leben koennen. dies kann der fall sein, da AsyncTask im Vordergrund sein kann
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems,mBitmapCache));
            // ausserdem sollen die naechsten 9 photos schon mal in den cache runtergeladen werden
            cacheNextPhotos(9,9);
        }
    }
    
    private void cacheNextPhotos(int numberOfPhotos, int position){
            GalleryItem galleryItem=mGalleryItems.get(position);
            mThumbnailDownloader.queueThumbnail(photoHolder,galleryItem.getUrl());
            mBitmapCache.put(photoHolder,);
        }

    private void updatePageNumber(){
        mCurrentPageTextView.setText("Current Page: " + String.valueOf(mCurrentPage) + "/" + String.valueOf(mPages)+ " -- Number of loaded pictures: " + (mItems.size()));
    }

    @Override
    public void onDestroyView(){
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        mThumbnailDownloader.quit(); // thread muss separat zerstoert werden -- andernfalls wird er zum zombie, sprich er hat keinen mutterprozess
        Log.i(TAG,"Background thread destroyed");
        mBitmapCache.evictAll(); // cache leeren
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder>{
        private List<GalleryItem> mGalleryItems;
        // private LruCache<PhotoHolder,Bitmap> mBitmapCache;
//         private LruCache<PhotoHolder,Bitmap> mBitmapCache=new LruCache<PhotoHolder,Bitmap>(CACHE_SIZE){
//             protected int sizeOf(PhotoHolder key, Bitmap value){
//                 return value.getByteCount();
//             }
//         }

        public PhotoAdapter(List<GalleryItem> galleryItems, LruCache<PhotoHolder,Bitmap> bitmapCache){
            mGalleryItems=galleryItems;
            mBitmapCache=bitmapCache;
//             // ausserdem sollen die naechsten 9 photos schon mal in den cache runtergeladen werden
//             cacheNextPhotos(9,9);
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup viewGroup, int viewType){
            // TextView textView=new TextView((getActivity()));
            // return new PhotoHolder(textView);
            LayoutInflater inflater=LayoutInflater.from(getActivity());
            View view=inflater.inflate(R.layout.list_item_gallery,viewGroup,false);
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(PhotoHolder photoHolder, int position){
            GalleryItem galleryItem=mGalleryItems.get(position);
            // photoHolder.bindGalleryItem(galleryItem);
            Drawable placeholder=getResources().getDrawable(R.drawable.myimage);
            // schauen, ob das bild schon im cache ist
            if(mBitmapCache.get(photoHolder)!=null){
                photoHolder.bindDrawable(mBitmapCache.get(photoHolder));
            } else{
                photoHolder.bindDrawable(placeholder);
                mThumbnailDownloader.queueThumbnail(photoHolder,galleryItem.getUrl());
            }
        }

        @Override
        public int getItemCount(){
            return mGalleryItems.size();
        }
        
        public int getItemHeight(){
            return PhotoHolder.getItemHeight();
        }
        
//         public void cacheNextPhotos(int numberOfPhotos, int position){
//             GalleryItem galleryItem=mGalleryItems.get(position);
//             mThumbnailDownloader.queueThumbnail(photoHolder,galleryItem.getUrl());
//             mBitmapCache.put(photoHolder,);
//         }
        
        // public LruCache<PhotoHolder,Bitmap> getLruCache(){ return mBitmapCache;}
        
    }


    private class PhotoHolder extends RecyclerView.ViewHolder{
        // private TextView mTitleTextView;
        private ImageView mItemImageView;

        public PhotoHolder(View itemView){
            super(itemView);
            // mTitleTextView=(TextView) itemView;
            mItemImageView=(ImageView) itemView.findViewById(R.id.item_image_view);
        }

        /*public void bindGalleryItem(GalleryItem item){
            // mTitleTextView.setText(item.toString()); // stellt nur die caption dar
        }*/

        public void bindDrawable(Drawable drawable){
            mItemImageView.setImageDrawable(drawable);
        }
        
        public int getItemHeight(){
            return mItemImageView.getHeight();
        }
    }


    private class FetchItemsTask extends AsyncTask<Integer,Void,List<GalleryItem>> { // bilder werden in einem neuen thread heruntergeladen -- dazu verwendenen wir die AsyncTask-Klasse
        // erster parameter gibt typ der parameter an, die beim aufruf von AsyncTask.execute() uebergeben werden
        // dritter parameter oben gibt den typ des resultats von doInBackground an. dies ist automatisch auch der typ, der als argument in onPostExecute verwendet werden muss
        @Override
        protected List<GalleryItem> doInBackground(Integer... params){ // das zeug laeuft im hintergrund ab
            return new FlickrFetchr().fetchItemsFromPage(params[0]);
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items){
            mItems.addAll(items); // schreibt die in doInBackground heruntergeladenen items endlich in das entsprechende objekt
            if(mItems.size()==0){
                setupAdapter();
            }
            else{
                mPhotoRecyclerView.getAdapter().notifyDataSetChanged();
            }
        }
    }
}
