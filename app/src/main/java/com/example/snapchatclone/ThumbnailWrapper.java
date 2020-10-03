package com.example.snapchatclone;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;


import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.IOException;

public class ThumbnailWrapper {

    Drawable defaultThumbnail;
    RoundedBitmapDrawable roundedThumbnail;
    boolean isDefault = false;

    public ThumbnailWrapper(Resources resources, File image_file) {
        //grab a bitmap for thumbnail
        try {
            ExifInterface exifInterface = new ExifInterface(image_file);
            //this.thumbnail = exifInterface.getThumbnailBitmap();
            Bitmap original = exifInterface.getThumbnailBitmap();
            Matrix matrix = new Matrix();
            matrix.postRotate(exifInterface.getRotationDegrees(),0,0);
            this.roundedThumbnail = RoundedBitmapDrawableFactory.create(resources,Bitmap.createBitmap(original,0,0,original.getWidth(),original.getHeight(),matrix,true));
            this.roundedThumbnail.setCircular(true);
            original.recycle();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ThumbnailWrapper(Resources resources){
        //now what do we do here
        this.isDefault = true;
        this.defaultThumbnail = ResourcesCompat.getDrawable(resources,R.drawable.ic_profile,null);
    }

    public void updateThumbnail(Resources resources,File image_file){
        this.isDefault = false;
        try {
            ExifInterface exifInterface = new ExifInterface(image_file);
            //this.thumbnail = exifInterface.getThumbnailBitmap();
            Bitmap original = exifInterface.getThumbnailBitmap();
            Matrix matrix = new Matrix();
            matrix.postRotate(exifInterface.getRotationDegrees(),0,0);
            this.roundedThumbnail = RoundedBitmapDrawableFactory.create(resources,Bitmap.createBitmap(original,0,0,original.getWidth(),original.getHeight(),matrix,true));
            this.roundedThumbnail.setCircular(true);
            original.recycle();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateThumbnail(Resources resources) {
        this.isDefault = true;
        if(this.defaultThumbnail == null) {
            this.defaultThumbnail = ResourcesCompat.getDrawable(resources, R.drawable.ic_profile, null);
        }
    }
}
