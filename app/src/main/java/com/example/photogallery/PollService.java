package com.example.photogallery;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.SystemClock;
import android.util.Log;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class PollService extends IntentService {
    private static final String TAG="PollService";
    private static final long POLL_INTERVALL_MS= TimeUnit.MINUTES.toMillis(1); // zeitintervall betraegt eine minute; zum testen
    // private static final long POLL_INTERVALL_MS= TimeUnit.MINUTES.toMillis(1); // zeitintervall betraegt 15 minute
    
    // Intents eines Services heissen Commands. Jedes Kommando ist eine Anweisung an den Service etwas zu erledigen.
    // Die empfangenen Kommandos kommen in eine Schlange, wo sie Stueck fuer Stueck abgearbeitet werden
    // Ein Service hat seinen eigenen Background-Thread, in dem er den aktuellen Command abarbeitet. Sind alle Commands erledigt, wird der Service zerstoert
    public static Intent newIntent(Context context){
        return new Intent(context,PollService.class);
    }
    
    // schaltet alarm an oder aus
    public static void setServiceAlarm(Context context, boolean isOn){
        Intent i=PollService.newIntent(context);
        // konstruktion eines PendingIntents braucht einen context, einen Request_Code, um pis voneinander zu unterscheiden, das zu sendende intent-objekt i und flags, die bestimmen, wie PendingIntent erzeugt wird
        // PendingIntent lebt im OS, funktioniert daher auch, wenn die app zerstoert ist
        PendingIntent pi=PendingIntent.getService(context, 0, i, 0);
        // jedes PendingIntent kann maximal einen Alarm verwalten
        AlarmManager alarmManager=(AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        // alarmmananger wird mit einem PendingIntent aufgerufen
        
        if(isOn){
            // erster parameter ist die zeit-basis des alarms, dann die zeit, ab der gemessen werden soll, das zeitintervall und schliesslich den zu startenden PendingIntent
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(),POLL_INTERVALL_MS,pi);
        } else{
            alarmManager.cancel(pi);
            pi.cancel();
        }
    }
    
    public static boolean isServiceAlarmOn(Context context){
        Intent i=PollService.newIntent(context);
        PendingIntent pi=PendingIntent.getService(context,0,i,PendingIntent.FLAG_NO_CREATE); // wenn pi nicht existiert, soll er nicht erstellt werden
        // wenn pi==null ist, ist der alarm nicht gesetzt
        return pi!=null;
    }
    
    public PollService(){
        super(TAG);
    }
    
    @Override
    protected void onHandleIntent(Intent intent){
        if(!isNetworkAvailableAndConnected()){
            return;
        }
        Log.i(TAG,"Received an intent: " + intent);
        
        // frage datenbank nach letzter anfrage und erhaltenem foto
        String query=QueryPreferences.getStoredQuery(this);
        String lastResultId=QueryPreferences.getLastResultId(this);
        List<GalleryItem> items;
        
        if(query==null){
            items=new FlickrFetchr().fetchRecentPhotos(1);
        } else{
            items=new FlickrFetchr().searchPhotos(query);
        }
        
        if(items.size()==0){ return;}
        
        // wenn es resultate gibt, nimm das erste
        String resultId=items.get(0).getId();
        if(resultId.equals(lastResultId)){
            Log.i(TAG,"Got an old result: " +resultId);
        } else{
            Log.i(TAG,"Got a new result: " + resultId);
        
        
            // zum handeln des PendingIntents, das wir von PhotoGalleryActivity bekommen, um diese zu starten
            // dieses PendingIntent wird in eine Notification gesteckt
            // PendingIntent bestimmt, was passiert, wenn die Notification gedrueckt wird
            Resources resources=getResources();
            Intent i=PhotoGalleryActivity.newIntent(this);
            PendingIntent pi=PendingIntent.getActivity(this,0,i,0);
            Notification notification=new NotificationCompat.Builder(this).
                                            setTicker(resources.getString(R.string.new_pictures_title)).
                                            setSmallIcon(android.R.drawable.ic_menu_report_image).
                                            setContentTitle(resources.getString(R.string.new_pictures_title)).
                                            setContentText(resources.getString(R.string.new_pictures_text)).
                                            setContentIntent(pi).
                                            setAutoCancel(true). // notification wird aus dem notification-drawer entfernt, wenn sie gedrueckt wird
                                            build();
            NotificationManagerCompat notificationManager=NotificationManagerCompat.from(this);
            notificationManager.notify(0,notification); // erstes argument wird zur unterscheidung verschiedener notifications benutzt
        }
        
        QueryPreferences.setLastResultId(this,resultId);
    }

    // pruefen, ob die hintergrund-app zugriff auf internet hat
    private boolean isNetworkAvailableAndConnected(){
        ConnectivityManager cm=(ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        
        // erst schauen, ob es ein netzwerk gibt (darf die app z.B. im hintergrund nichts runterladen, gibt getActiveNetworkInfo null zurueck
        boolean isNetworkAvailabe=cm.getActiveNetworkInfo()!=null;
        boolean isNetworkConnected=isNetworkAvailabe && cm.getActiveNetworkInfo().isConnected();
        
        return isNetworkConnected;
    }

}