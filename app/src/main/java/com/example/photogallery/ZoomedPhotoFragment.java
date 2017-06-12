package com.example.photogallery;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;

/**
 * Created by merz_konstantin on 5/20/17.
 */

public class ZoomedPhotoFragment extends DialogFragment{
    private static final String ARG_BYTES="bytes";


    private Bitmap mBitmap;
    private ImageView mPhotoView;
    private byte[] mByte;


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        View v= LayoutInflater.from(getContext()).inflate(R.layout.dialog_zoomed_photo,null);

        mPhotoView=(ImageView) v.findViewById(R.id.zoomed_photo);
        mByte=(byte[]) getArguments().getSerializable(ARG_BYTES);

        mBitmap=PictureUtils.getScaledBitmap(mByte,getActivity());
        AlertDialog.Builder builder=new AlertDialog.Builder(getContext());

        mPhotoView.setImageBitmap(mBitmap);
        // Drawable placeholder=getResources().getDrawable(R.drawable.myimage);
        // mPhotoView.setImageDrawable(placeholder);
        builder.setView(v).setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sendResult(Activity.RESULT_OK);
                // onDestroy();
            }
        });
        return builder.create();

    }


    public static ZoomedPhotoFragment newInstance(byte[] b) {
        Bundle args = new Bundle();
        args.putByteArray(ARG_BYTES,b);
        ZoomedPhotoFragment fragment = new ZoomedPhotoFragment();
        fragment.setArguments(args);
        return fragment;
    }

    // wird nur aufgerufen, falls foto geloescht werden soll
    private void sendResult(int resultCode){
        getTargetFragment().onActivityResult(getTargetRequestCode(),resultCode,null);
    }
}
