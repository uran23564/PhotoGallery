package com.example.photogallery;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;

public class PhotoGalleryActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment(){
        return PhotoGalleryFragment.newInstance();
    }
    
    // gibt einen Intent zum starten der Activity zurueck
    // wenn ein alarm losgeht, wird PollService diese Methode aufrufen und das erhaltene Intent in ein PendingIntent packen, welches in die Notification gesteckt wird. Drueckt der Nutzer auf die Notification, wird dieses PendingIntent verwendet, um die Activity zu starten
    public static Intent newIntent(Context context){
        return new Intent(context,PhotoGalleryActivity.class);
    }
}
