package com.example.photogallery;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by merz_konstantin on 5/28/17.
 */

public class PhotoGalleryFragment extends Fragment {
    private static final String TAG="PhotoGalleryFragment";

    private RecyclerView mPhotoRecyclerView;
    private List<GalleryItem> mItems=new ArrayList<>();
    // private RecyclerView.OnScrollListener mScrollListener;
    private int pages=1; // wir starten mit der ersten seite

    public static PhotoGalleryFragment newInstance(){
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        new FetchItemsTask().execute(pages); // startet den async-task
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View v=inflater.inflate(R.layout.fragment_photo_gallery,container,false);
        mPhotoRecyclerView=(RecyclerView) v.findViewById(R.id.photo_recycler_view);
        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(),3));
        // mPhotoRecyclerView.addOnScrollListener(mScrollListener);
        mPhotoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener{
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState){
                if(newState==SCROLL_STATE_SETTLING){
                    pages++;
                    FetchItemsTask().execute(pages);
                }
            }
            
            @Override void onScrolled(RecyclerView recyclerView, int dx, int dy){
                // TODO
            }
        });
        
        setupAdapter();
        
        return v;
    }

    private void setupAdapter(){
        if(isAdded()){ // checkt, ob fragment schon an die activity angehaengt wurde, da fragments theoretisch auch ohne activities leben koennen. dies kann der fall sein, da AsyncTask im Vordergrund sein kann
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder>{
        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> galleryItems){
            mGalleryItems=galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup viewGroup, int viewType){
            TextView textView=new TextView((getActivity()));
            return new PhotoHolder(textView);
        }

        @Override
        public void onBindViewHolder(PhotoHolder photoHolder, int position){
            GalleryItem galleryItem=mGalleryItems.get(position);
            photoHolder.bindGalleryItem(galleryItem);
        }

        @Override
        public int getItemCount(){
            return mGalleryItems.size();
        }
    }


    private class PhotoHolder extends RecyclerView.ViewHolder{
        private TextView mTitleTextView;

        public PhotoHolder(View itemView){
            super(itemView);
            mTitleTextView=(TextView) itemView;
        }

        public void bindGalleryItem(GalleryItem item){
            mTitleTextView.setText(item.toString()); // stellt nur die caption dar
        }
    }


    private class FetchItemsTask extends AsyncTask<int,Void,List<GalleryItem>> { // bilder werden in einem neuen thread heruntergeladen -- dazu verwendenen wir die AsyncTask-Klasse
        // erster parameter gibt typ der parameter an, die beim aufruf von AsyncTask.execute() uebergeben werden
        // dritter parameter oben gibt den typ des resultats von doInBackground an. dies ist automatisch auch der typ, der als argument in onPostExecute verwendet werden muss
        @Override
        protected List<GalleryItem> doInBackground(Int... params){ // das zeug laeuft im hintergrund ab
            return new FlickrFetchr().fetchItemsFromPage(params);
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items){
            mItems=items; // schreibt die in doInBackground heruntergeladenen items endlich in das entsprechende objekt
            setupAdapter();
        }
    }
}
