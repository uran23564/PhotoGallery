package com.example.photogallery;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.GridLayoutManager;
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

import static android.support.v7.widget.RecyclerView.SCROLL_STATE_IDLE;

/**
 * Created by merz_konstantin on 5/28/17.
 */

public class PhotoGalleryFragment extends Fragment {
    private static final String TAG="PhotoGalleryFragment";

    private RecyclerView mPhotoRecyclerView;
    private android.support.v7.widget.GridLayoutManager mGridLayoutManager;
    private TextView mCurrentPageTextView;
    
    private List<GalleryItem> mItems=new ArrayList<>();

    private int mPages=1; // wir starten mit einer seite
    private int mCurrentPage=1; // gerade angesehene Seite
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;
    
    ProgressDialog mDialog = new ProgressDialog(getActivity());

    
    public static PhotoGalleryFragment newInstance(){
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptions(true);
        // new FetchItemsTask().execute(mPages); // startet den async-task
        updateItems(mPages);

        Handler thumbnailResponseHandler=new Handler(); // handler gehoert dem mainthread

        mThumbnailDownloader=new ThumbnailDownloader<>(thumbnailResponseHandler);
        mThumbnailDownloader.setThumbnailDownloadListener(new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
            @Override
            public void onThumbnailDownloaded(PhotoHolder photoHolder, Bitmap bitmap) {
                Drawable drawable=new BitmapDrawable(getResources(), bitmap);
                photoHolder.bindDrawable(drawable);
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

        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(),3));
        mGridLayoutManager=(GridLayoutManager)mPhotoRecyclerView.getLayoutManager();
        // final int itemHeight=mGridLayoutManager.getHeight();

        mPhotoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener(){
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState){
                int first=mGridLayoutManager.findFirstVisibleItemPosition();
                if(newState==SCROLL_STATE_IDLE&&mGridLayoutManager.findFirstVisibleItemPosition()==0){
                    cacheNextPhotos(9,15);
                }
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

                if(dy>5){ // naechsten fotos schon mal in den cache laden
                    int pos=mGridLayoutManager.findLastVisibleItemPosition();
                    // cacheNextPhotos(9,mGridLayoutManager.findLastVisibleItemPosition());
                    cacheNextPhotos(18, pos);
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
        if(isAdded()){ // checkt, ob fragment schon an die activity angehaengt wurde, da fragments theoretisch auch ohne activities leben koennen. dies kann der fall sein, da AsyncTask im Vordergrund sein kann;
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }
    
    private void cacheNextPhotos(int numberOfPhotos, int position){ // laedt naechste numberOfPhotos in den Cache
        // int startIndex=Math.max(position-numberOfPhotos,0);
        int startIndex=Math.max(position,0);
        int endIndex=Math.min(position+numberOfPhotos,mItems.size()-1);
        for(int i=startIndex;i<=endIndex;i++) {
            if (i==position){ continue;}
            String url=mItems.get(i).getUrl();
            mThumbnailDownloader.preloadImage(url);
        }
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
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu,MenuInflator menuInflator){
        super.onCreateOptionsMenu(menu,menuInflator);
        menuInflator.inflate(R.menu.fragment_photo_gallery,menu);
        MenuItem searchItem=menu.findItem(R.id.menu_item_search);
        final SearchView searchView=(SearchView) searchItem.getActionView();
        
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener(){
            @Override
            public boolean onQueryTextSubmit(String s){
                Log.d(TAG,"QueryTextSubmit: "+s);
                QueryPreferences.setStoredQuery(getActivity(),s);
                // keyboard verschwinden lassen 
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
                // searchview einklappen
                searchItem.collapseActionView();
                // loading indicator anzeigen lassen (wirbelnder kreis...)
                mPhotoRecyclerView.clear();
                // TODO ...
                // ProgressDialog mDialog = new ProgressDialog(getActivity());
                mDialog.setMessage("Please wait...");
                // mDialog.setIndeterminate(true);
                mDialog.setCancelable(true);
                mDialog.setProgressStyle(STYLE_SPINNER); // ignoriert setIndeterminate-Setting
                mDialog.show();
                updateItems(1);
                return true;
            }
            
            @Override
            public boolean onQueryTextChange(String s){
                Log.d(TAG,"QueryTextChange: "+s);
                return false;
            }
        });
        
        searchView.setOnSearchClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                String query=QueryPreferences.getStoredQuery(getActivity());
                searchView.setQuery(query,false); // 
            }
        });
        
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId())
        case R.id.menu_item_refresh:
        // adapter des recyclerviews loeschen, cache loeschen und neuen adapter anhaengen
        mThumbnailDownloader.clearCache();
        mItems.clear();
        mPhotoRecyclerView.clear();
        mPages=1;
        mCurrentPage=1;
        updateItems(mPages);
        return true;
        
        case R.id.menu_item_clear:
        QueryPreferences.setStoredQuery(getActivity(),null);
        updateItems;
        return true;
        
        default: return super.onOptionsItemSelected(item);
    }
    
    private void updateItems(int page){
        String query=QueryPreferences.getStoredQuery(getActivity());
        new FetchItemsTask(query).execute(page);
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder>{
        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> galleryItems){
            mGalleryItems=galleryItems;
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
            Drawable placeholder=getResources().getDrawable(R.drawable.myimage);
            Bitmap bitmap=mThumbnailDownloader.getCachedImage(galleryItem.getUrl());

            // Wenn bild noch nicht im cache, dann runterladen
            if(bitmap==null){
                photoHolder.bindDrawable(placeholder);
                mThumbnailDownloader.queueThumbnail(photoHolder,galleryItem.getUrl());
            } else{
                Log.i(TAG,"Image loaded from cache");
                photoHolder.bindDrawable(new BitmapDrawable(getResources(),bitmap));
            }
        }

        @Override
        public int getItemCount(){
            return mGalleryItems.size();
        }
    }


    private class PhotoHolder extends RecyclerView.ViewHolder{
        // private TextView mTitleTextView;
        private ImageView mItemImageView;
        private GalleryItem mGalleryItem;

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
    }


    private class FetchItemsTask extends AsyncTask<Integer,Void,List<GalleryItem>> { // bilder werden in einem neuen thread heruntergeladen -- dazu verwendenen wir die AsyncTask-Klasse
        // erster parameter gibt typ der parameter an, die beim aufruf von AsyncTask.execute() uebergeben werden
        // dritter parameter oben gibt den typ des resultats von doInBackground an. dies ist automatisch auch der typ, der als argument in onPostExecute verwendet werden muss
        private String mQuery;
        
        public FetchItemsTask(String query){
            mQuery=query;
        }
        
        @Override
        protected List<GalleryItem> doInBackground(Integer... params){ // das zeug laeuft im hintergrund ab
            // return new FlickrFetchr().fetchItemsFromPage(params[0]);
            // String query="robot"; // zum testen
            if(mQuery==null){
                return new FlickrFetchr().fetchRecentPhotos(params[0]);
            } else{
                return new FlickrFetchr().searchPhotos(mQuery);
            }
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
            // wenn json-daten geladen wurden, soll dialog verschwinden
            mDialog.onStop();
        }
    }
}
