package com.ttv.petrecog;

import android.app.AlertDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class DogRecognizeTask extends AsyncTask<String,String,byte[]> {
    AlertDialog progressDialog;
    Context context;

    public DogRecognizeTask (Context context){
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

//        progressDialog = new SpotsDialog.Builder()
//                .setContext(this.context)
//                .setMessage("Processing...")
//                .build();
//        progressDialog.show();
    }

    public static String getBase64FromPath(String path) {
        String base64 = "";
        try {
            File file = new File(path);
            byte[] buffer = new byte[(int) file.length() + 100];
            int length = new FileInputStream(file).read(buffer);
            base64 = Base64.encodeToString(buffer, 0, length,
                    Base64.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return base64;
    }

    @Override
    protected byte[] doInBackground(String... strings) {

        Log.e("TestEngine", "strings: " + strings[0]);
        try {
            // Set header
            HttpPostMultipart multipart = new HttpPostMultipart("http://192.248.148.182:8889/dog_noseprint", "utf-8", null);
            // Add form field
            multipart.addFilePart("image", new File(strings[0]));
            // Print result
            String response = multipart.finish();
//            Log.e("TestEngine", "response: " + response);

            JSONObject parentJson = new JSONObject(response);
            JSONArray dataArr = parentJson.getJSONArray("data");
            if(dataArr.length() > 0) {
                byte[] bytes = new byte[dataArr.length() * 4];
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                buffer.order(ByteOrder.LITTLE_ENDIAN);

                for(int i = 0; i < dataArr.length(); i ++){
                    float feat = (float)dataArr.getDouble(i);
                    buffer.putFloat(feat);
                }

                return bytes;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    @Override
    protected void onPostExecute(byte[] s) {
        super.onPostExecute(s);

//        if (progressDialog.isShowing()) {
//            progressDialog.dismiss();
//        }
    }
}
