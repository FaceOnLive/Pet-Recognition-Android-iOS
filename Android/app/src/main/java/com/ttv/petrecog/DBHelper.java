package com.ttv.petrecog;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class DBHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "users.db";
    public static final String CONTACTS_TABLE_NAME = "users";
    public static final String CONTACTS_COLUMN_NAME = "name";
    public static final String CONTACTS_COLUMN_FACE = "face";
    public static final String CONTACTS_COLUMN_FEATURE = "feature";
    private HashMap hp;

    public DBHelper(Context context) {
        super(context, DATABASE_NAME , null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // TODO Auto-generated method stub
        db.execSQL(
                "create table users " +
                        "(name text primary key, face blob, feature blob)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
        db.execSQL("DROP TABLE IF EXISTS users");
        onCreate(db);
    }

    public boolean insertUser (String name, Bitmap faceImg, byte[] feature) {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        faceImg.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] face = byteArrayOutputStream.toByteArray();

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("name", name);
        contentValues.put("face", face);
        contentValues.put("feature", feature);
        db.insert("users", null, contentValues);
        return true;
    }

    public Cursor getData(String name) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from users where name="+name, null );
        return res;
    }

    public int numberOfRows(){
        SQLiteDatabase db = this.getReadableDatabase();
        int numRows = (int) DatabaseUtils.queryNumEntries(db, CONTACTS_TABLE_NAME);
        return numRows;
    }

    public Integer deleteUser (String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete("users",
                "name = ? ",
                new String[] { name });
    }

    public Integer deleteAllUser () {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("delete from "+ CONTACTS_TABLE_NAME);
        return 0;
    }

    public void getAllUsers() {
        MainActivity.userLists.clear();

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from users", null );
        res.moveToFirst();

        while(res.isAfterLast() == false){
            String userName = res.getString(res.getColumnIndex(CONTACTS_COLUMN_NAME));
            byte[] faceData = res.getBlob(res.getColumnIndex(CONTACTS_COLUMN_FACE));
            byte[] featureData = res.getBlob(res.getColumnIndex(CONTACTS_COLUMN_FEATURE));
            Bitmap faceImg = BitmapFactory.decodeByteArray(faceData, 0, faceData.length);

            FaceEntity face = new FaceEntity(userName, faceImg, featureData);
            MainActivity.userLists.add(face);
            res.moveToNext();
        }
    }
}
