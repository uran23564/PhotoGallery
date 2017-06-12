package com.example.photogallery;

public class PollService extends IntentService{
    private static final String TAG="PollService";
    private static final long POLL_INTERVALL_MS=TimeUnit.MINUTES.toMillies(1); // zeitintervall betraegt eine minute
    
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
        PendingIntent pi=PendingIntent.getService(context, 0, i, 0);
        AlarmManager alarmManager=(AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        // alarmmananger wird mit einem PendingIntent aufgerufen
        
        if(isOn){
            // erster parameter ist die zeit-basis des alarms, dann die zeit, ab der gemessen werden soll, das zeitintervall und schliesslich den zu startenden PendingIntent
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME,SystemClock.elapsedRealtime(),POLL_INTERVALL_MS,pi);
        } else{
            alarmManager.cancel(pi);
            pi.cancel();
        }
    }
    
    public PollService(){
        super(TAG);
    }
    
    @Override
    protected void onHandleIntent(Intent intent){
        if(!isNetworkAvailableAndConnected){
            return;
        }
        Log.i(TAG,"Received an intent: " + intent);
        
        // frage datenbank nach letzter anfrage und erhaltenem foto
        String query=QueryPreferences.getStoredQuery(this);
        String lastResultId=QueryPreferences.getLastResultId(this);
        List<GalleryItems> items;
        
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