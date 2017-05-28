package com.example.photogallery;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by merz_konstantin on 5/13/17.
 */

// Abstrakte Activity-Klasse, die ein einzelnes Fragment hostet; WAS fuer ein fragment das sein wird, wird durch die
// Funktion createFragment() festgelegt.

public abstract class SingleFragmentActivity extends AppCompatActivity {
    protected abstract Fragment createFragment(); // initialisiert fragment; subklassen von SingleFragmentActivity werden diese
    // Methode fuer sich massgeschneidert selbst definieren

    // Unterklassen koennen diese Methode ueberschreiben (je nach Gusto)
    @LayoutRes // sagt AndroidStudio, dass eine gueltige Layout resource ID zurueckgegeben werden muss
    protected int getLayoutResId(){
        return R.layout.activity_fragment;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResId());

        FragmentManager fm=getSupportFragmentManager(); // neuer fragmentmanager -- dieser managed eine liste von fragments und einen back stack von fragment transactions
        // ausserdem fuegt er die views der fragments in die hierarchie der activity ein

        Fragment fragment=fm.findFragmentById(R.id.fragment_container); // zeiger auf unser fragment, falls es schon existiert. wenn nicht, wird "null" zurueckgegeben
        // fragments werden mit den resource-ids der container, wo sie leben, identifiziert

        if(fragment==null){
            fragment = createFragment(); // wenn das fragment noch leer ist -> initialisiere es mit dem entsprechenden Konstruktor
            fm.beginTransaction().add(R.id.fragment_container,fragment).commit(); // fragment-transaction
            /* fragment-transaction werden zum hinzufuegen, loeschen, anhaengen, aushaengen oder ersetzen von fragments in der liste der fragments benutzt
               hier: neue fragment-transaction erzeugen -> addiere das erzeugte fragment in den container der activity und uebergieb das kommando.
               des weiteren ist dieses fragment (durch identifikation mit R.id.fragment_container) in der fragment-liste des fragment-managers
               R.id.fragment_container wird neben dem ort, wo das fragment angezeigt werden soll auch zum eindeutigen identifizieren des fragments in der liste
               der fragments benutzt */

            /* auch, wenn die activity zerstoert wird, speichert der zugehoerige fragmentmanager die liste der fragments. wird die activity neu erzeugt, holt sich der
               neue fragment-manager diese alte liste und arbeitet weiter wie zuvor */
        }
    }
}
