package com.example.photogallery;

public class FlickrFetchr{
    public byte[] getUrlBytes(String urlSpec) throws IOException{ // erzeugt einen rohes URL-Objekt (als Byte-Array) aus dem uebergebenen String
        URL url=new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection(); // erzeugt ein Connection-Objekt, das auf die URL zeigt. Dabei wird das URLConnection-Objekt zu einem spezifischen HttpURLConnection-Objekt gecastet (hat eigene interfaces, methoden etc.)
        
        try{
            ByteArrayOutputStream out = new ByteArrayOutputStream(); // wird mit InputStream der URL gefuettert
            InputStream in=connection.getInputStream(); // Verbindung wird mit Aufruf von getInputStream() schliesslich aufgebaut. in frisst die Daten aus dieser Verbindung.
            if(connection.getResponseCode()!=HttpURLConnection.HTTP_OK){
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
        finally { connection.disconnect()}
    }
    
    public String getUrlString(String urlSpec) throws IOException{ // extrahiert die URL als String
        return new String(getUrlBytes(urlSpec));
    }

}