package com.example.photogallery;

import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FlickrFetchr{
    private static final String TAG="FlickrFetchr";
    private static final String API_KEY="0363ad859a7ae1001843f4baa4a865ce";
    private static final String FETCH_RECENTS_METHOD="flickr.photos.getRecent";
    private static final String SEARCH_METHOD="flickr.photos.search";
    private static final Uri ENDPOINT=Uri.parse("https://api.flickr.com/services/rest").buildUpon()
                    .appendQueryParameter("api_key", API_KEY)
                    .appendQueryParameter("format", "json")
                    .appendQueryParameter("nojsoncallback", "1")
                    .appendQueryParameter("extras", "url_s") // sagt Flickr, dass wir auch dir url fuer die kleine version des bildes haben wollen (wenn sie existiert)
                    .build();

    private List<GalleryItem> mItems=new ArrayList<>();
    
    public byte[] getUrlBytes(String urlSpec) throws IOException{ // erzeugt einen rohes URL-Objekt (als Byte-Array) aus dem uebergebenen String
        URL url=new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection(); // erzeugt ein Connection-Objekt, das auf die URL zeigt. Dabei wird das URLConnection-Objekt zu einem spezifischen HttpURLConnection-Objekt gecastet (hat eigene interfaces, methoden etc.)
        
        try{
            ByteArrayOutputStream out = new ByteArrayOutputStream(); // wird mit InputStream der URL gefuettert
            InputStream in=connection.getInputStream(); // Verbindung wird mit Aufruf von getInputStream() schliesslich aufgebaut. in frisst die Daten aus dieser Verbindung.
            if(connection.getResponseCode()!= HttpURLConnection.HTTP_OK){
                throw new IOException(connection.getResponseMessage() + "with: " + urlSpec);
            }
            
            int bytesRead=0;
            byte[] buffer=new byte[1024];
            while((bytesRead=in.read(buffer))>0){ // die von in gefressenen Daten kommen in den buffer
                out.write(buffer,0,bytesRead); // und dann in den ByteArrayOutputStream, der zuletzt gecastet und zurueckgegeben wird.
            }
            out.close();
            return out.toByteArray();
        }
        finally { connection.disconnect();}
    }
    
    public String getUrlString(String urlSpec) throws IOException { // extrahiert String aus dem ByteArray der URL
        return new String(getUrlBytes(urlSpec));
    }
    
    public List<GalleryItem> fetchRecentPhotos(int page){
        String url=buildUrl(FETCH_RECENTS_METHOD,null,page);
        return downloadGalleryItemsFromPage(page,url);
    }
    
    public List<GalleryItem> searchPhotos(String query){
        String url=buildUrl(SEARCH_METHOD,query,0);
        return downloadGalleryItemsFromPage(0,url);
    }
    
    private String buildUrl(String method, String query, int page){
        Uri.Builder uriBuilder=ENDPOINT.buildUpon().appendQueryParameter("method",method);
        if (method.equals(SEARCH_METHOD)){
            uriBuilder.appendQueryParameter("text", query);
        } else if(method.equals(FETCH_RECENTS_METHOD)){
            uriBuilder.appendQueryParameter("page", String.valueOf(page));
        }
        return uriBuilder.build().toString();
    }

    
    private List<GalleryItem> downloadGalleryItemsFromPage(Integer page,String url){
        try{
//             String url= Uri.parse("https://api.flickr.com/services/rest").buildUpon()
//                     .appendQueryParameter("method", "flickr.photos.getRecent")
//                     .appendQueryParameter("api_key", API_KEY)
//                     .appendQueryParameter("format", "json")
//                     .appendQueryParameter("nojsoncallback", "1")
//                     .appendQueryParameter("extras", "url_s") // sagt Flickr, dass wir auch dir url fuer die kleine version des bildes haben wollen (wenn sie existiert)
//                     .appendQueryParameter("page", String.valueOf(page))
//                     .build().toString();
            String jsonString = getUrlString(url);
            Log.i(TAG, "Received JSON: " + jsonString);
            JSONObject jsonBody = new JSONObject(jsonString); // json-konstruktor uebersetzt die json-hierarchie, die im string abgespeichert ist, in ein entsprechendes java-objekt mit der selben hierarchie
            // hiert ist jsonBody das top-level-Objekt, das das JSONArray photo traegt, welches wiederum eine Familie von JSON-Objekten beinhaltet. jedes davon stellt die metadaten eines einzelnen fotos dar
            parseItems(mItems,jsonBody);
            
            // Gson gson=new GsonBuilder.create();
            // mItems=gson.fromJson(jsonString,List<GalleryItem>)
        } catch(IOException ioe){
            Log.e(TAG, "Failed to fetch items", ioe);
        } catch(JSONException je){
            Log.e(TAG, "Failed to parse JSON", je);
        }

        return mItems;
    }

    private void parseItems(List<GalleryItem> items, JSONObject jsonBody) throws IOException, JSONException{ // durchsucht das photos-array des json-objekts photo aus dem ober-json-objekt und steckt jedes foto
        // in ein GalleryItem-Objekt
        JSONObject photosJsonObject=jsonBody.getJSONObject("photos");
        JSONArray photoJsonArray=photosJsonObject.getJSONArray("photo");

        for(int i=0;i<photoJsonArray.length();i++){
            JSONObject photoJsonObject= photoJsonArray.getJSONObject(i);

            GalleryItem item= new GalleryItem();
            item.setId(photoJsonObject.getString("id"));
            item.setCaption(photoJsonObject.getString("title"));

            if(!photoJsonObject.has("url_s")){ // ignoriere bilder, die keine url aufweisen
                continue;
            }

            item.setUrl(photoJsonObject.getString("url_s"));
            items.add(item);
        }
    }
}