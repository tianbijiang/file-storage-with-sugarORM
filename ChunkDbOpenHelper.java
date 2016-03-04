package com.spectocor.micor.android.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

import com.spectocor.micor.dummysender.Chunk;

import java.io.File;

public class ChunkDbOpenHelper extends SQLiteOpenHelper {

    private static String DbName = "CHUNK_DB";

    private static final String TbName = "chunks";
    private static final int DbVer = 1;
    private static final String[] Cols = {"pid", "chunk_id", "lead", "chunk_blob", "enc_key_id", "starttimestamp_utc"};
    private static final String[] ColsTypes = {"INTEGER", "TEXT PRIMARY KEY", "INTEGER", "BLOB", "TEXT", "DATETIME"};

    /**
     * The constructor.
     *
     * @param context Android application context.
     */
    ChunkDbOpenHelper(Context context) {
        super(context, DbName, null, DbVer);
    }

    /**
     * Statically set database storage mode.
     *
     * @param mode EXTERNAL for sdcard, INTERNAL for internal memory.
     * @param path Sdcard path if EXTERNAL is selected.
     */
    public static void switchDbMode(String mode, String path) {
        switch (mode) {
            case "EXTERNAL":
                if (isExternalStorageWritable()) {
                    DbName = path + "CHUNK_DB";
                    break;
                }
            case "INTERNAL":
                DbName = "CHUNK_DB";
                break;
            default:
                break;
        }
    }

    /**
     * Check if the sdcard writable.
     *
     * @return True for writable, false for not.
     */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }


    /**
     * Called when the database is created for the first time.
     *
     * @param db The database.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_COLUMNS = Cols[0] + " " + ColsTypes[0];
        if (Cols.length > 1) {
            for (int i = 1; i < Cols.length; i++) {
                CREATE_COLUMNS += "," + Cols[i] + " " + ColsTypes[i];
            }
        }
        String CREATE_TABLE = "CREATE TABLE " + TbName + " (" + CREATE_COLUMNS + ");";
        db.execSQL(CREATE_TABLE);
    }

    /**
     * Called when the database needs to be upgraded. This method is used to upgrade schema.
     *
     * @param db         The database.
     * @param oldVersion The old database version.
     * @param newVersion The new database version.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String UPGRADE_TABLE = "DROP TABLE IF EXISTS " + TbName;
        db.execSQL(UPGRADE_TABLE);
        onCreate(db);
    }

    /**
     * Insert a chunk.
     *
     * @param chunk The chunk to be inserted.
     */
    public void addChunk(Chunk chunk) {
        SQLiteDatabase db = this.getReadableDatabase();

        ContentValues values = new ContentValues();
        values.put("pid", chunk.getpId());
        values.put("chunk_id", chunk.getChunkId());
        values.put("lead", chunk.getLead());
        values.put("chunk_blob", chunk.getChunkBlob());
        values.put("enc_key_id", chunk.getKeyId());
        values.put("starttimestamp_utc", chunk.getChunkStartTime());

        db.insert(TbName, null, values);

        db.close();
    }

    /**
     * Print the number of chunks stored.
     */
    public void getCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        System.out.println("======total count========");
        System.out.println(DatabaseUtils.queryNumEntries(db, TbName));
    }

    /**
     * For testing: retrieve the blob data by chunk_id.
     *
     * @param cId The chunk_id.
     * @return The blob.
     */
    public byte[] findBlobById(String cId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from " + TbName + " where chunk_id=?", new String[]{cId});
        if (cursor.moveToNext()) {
            byte[] blob = cursor.getBlob(3);
            cursor.close();
            db.close();
            return blob;
        }
        cursor.close();
        db.close();
        return null;
    }
}
