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
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Utils {

    public static Rect getBestRect(int width, int height, Rect srcRect) {
        if (srcRect == null) {
            return null;
        }
        Rect rect = new Rect(srcRect);

        int maxOverFlow = Math.max(-rect.left, Math.max(-rect.top, Math.max(rect.right - width, rect.bottom - height)));
        if (maxOverFlow >= 0) {
            rect.inset(maxOverFlow, maxOverFlow);
            return rect;
        }

        int padding = rect.height() / 2;

        if (!(rect.left - padding > 0 && rect.right + padding < width && rect.top - padding > 0 && rect.bottom + padding < height)) {
            padding = Math.min(Math.min(Math.min(rect.left, width - rect.right), height - rect.bottom), rect.top);
        }
        rect.inset(-padding, -padding);
        return rect;
    }

    public static Bitmap crop(final Bitmap src, final int srcX, int srcY, int srcCroppedW, int srcCroppedH, int newWidth, int newHeight) {
        final int srcWidth = src.getWidth();
        final int srcHeight = src.getHeight();
        float scaleWidth = ((float) newWidth) / srcCroppedW;
        float scaleHeight = ((float) newHeight) / srcCroppedH;

        final Matrix m = new Matrix();

        m.setScale(1.0f, 1.0f);
        m.postScale(scaleWidth, scaleHeight);
        final Bitmap cropped = Bitmap.createBitmap(src, srcX, srcY, srcCroppedW, srcCroppedH, m,
                true /* filter */);
        return cropped;
    }

    public static File bitmapToFile(Bitmap bitmap, String fileNameToSave) { // File name like "image.png"
        //create a file to write bitmap data
        File file = null;
        try {
            file = new File(fileNameToSave);
            file.createNewFile();

//Convert bitmap to byte array
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 0 , bos); // YOU can also save it in JPEG
            byte[] bitmapdata = bos.toByteArray();

//write the bytes in file
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(bitmapdata);
            fos.flush();
            fos.close();
            return file;
        }catch (Exception e){
            e.printStackTrace();
            return file; // it will return null
        }
    }

    public static File ensureDirExists(File dir) throws IOException {
        if (!(dir.isDirectory() || dir.mkdirs())) {
            throw new IOException("Couldn't create directory '" + dir + "'");
        }
        return dir;
    }

    public static double getSimilarity(byte[] feat1, byte[] feat2) {
        if(feat1.length != feat2.length) return 0;

        ByteBuffer buffer1 = ByteBuffer.wrap(feat1);
        buffer1.order(ByteOrder.LITTLE_ENDIAN);

        ByteBuffer buffer2 = ByteBuffer.wrap(feat2);
        buffer2.order(ByteOrder.LITTLE_ENDIAN);

        int size = feat1.length / 4;
        double ret = 0.0, mod1 = 0.0, mod2 = 0.0;
        for(int i = 0; i < size; i ++) {
            float featVal1 = buffer1.getFloat();
            float featVal2 = buffer2.getFloat();

            ret += featVal1 * featVal2;
            mod1 += featVal1 * featVal1;
            mod2 += featVal2 * featVal2;
        }

        return (ret / Math.sqrt(mod1) / Math.sqrt(mod2) + 1) / 2.0;
    }
}
