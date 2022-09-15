package com.ttv.petrecog;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;

import com.ttv.face.FaceResult;

import java.io.IOException;
import java.io.InputStream;

public class FaceEntity {

    public String userName;
    public Bitmap headImg;
    public byte[] feature;

    public FaceEntity() {

    }

    public FaceEntity(String userName, Bitmap headImg, byte[] feature) {
        this.userName = userName;
        this.headImg = headImg;
        this.feature = feature;
    }
}
