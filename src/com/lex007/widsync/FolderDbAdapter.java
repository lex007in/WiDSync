package com.lex007.widsync;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class FolderDbAdapter {
	
	public static final String KEY_FOLDER_ID = "folder_id";
    public static final String KEY_PATH = "path";
    public static final String KEY_TIMESTAMP = "timestamp";
    public static final String KEY_TYPE_SYNC = "type_sync";
    public static final String KEY_TYPE_SYNC_TXT = "type_sync_txt";
    public static final String KEY_ID = "_id";
    
    public static final int TYPE_SRC = 1;
    public static final int TYPE_DST = 2;
    public static final String TYPE_SRC_TXT = "Source";
    public static final String TYPE_DST_TXT = "Destenation";
    
    private static final String TAG = "FolderDbAdapter";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    
    private static final String DATABASE_NAME = "data";
    private static final String DATABASE_TABLE = "folders";
    private static final int DATABASE_VERSION = 2;
    
    private static final String DATABASE_CREATE =
            "create table " + DATABASE_TABLE + " (" +
            KEY_ID + " integer primary key autoincrement, " +
            KEY_FOLDER_ID + " text not null, " +
            KEY_PATH + " text not null, " +
            KEY_TIMESTAMP + " text not null, " +
            KEY_TYPE_SYNC_TXT + " text not null, " +
            KEY_TYPE_SYNC + " integer not null);";

    
    private final Context mCtx;
    
    /**
     * Database creation sql statement
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
            onCreate(db);
        }
    }
    
    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param ctx the Context within which to work
     */
    public FolderDbAdapter(Context ctx) {
        this.mCtx = ctx;
    }
    
    /**
     * Open database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     * 
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    public FolderDbAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        mDbHelper.close();
    }
    
    
    public long createFolder(String folderId, String path, String timestamp, int typeSync) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_FOLDER_ID, folderId);
        initialValues.put(KEY_PATH, path);
        initialValues.put(KEY_TIMESTAMP, timestamp);
        initialValues.put(KEY_TYPE_SYNC, typeSync);
        initialValues.put(KEY_TYPE_SYNC_TXT, typeSync == TYPE_SRC?TYPE_SRC_TXT:TYPE_DST_TXT);

        return mDb.insert(DATABASE_TABLE, null, initialValues);
    }
    
    public boolean deleteFolder(long rowId) {

        return mDb.delete(DATABASE_TABLE, KEY_ID + "=" + rowId, null) > 0;
    }
    
    public Cursor fetchAllFolders() {

        return mDb.query(DATABASE_TABLE, new String[] {KEY_ID, KEY_FOLDER_ID, KEY_PATH,
        		KEY_TIMESTAMP, KEY_TYPE_SYNC, KEY_TYPE_SYNC_TXT}, null, null, null, null, null);
    }
    
    public Cursor fetchDstFolders() {

        return mDb.query(DATABASE_TABLE, new String[] {KEY_ID, KEY_FOLDER_ID, KEY_PATH,
        		KEY_TIMESTAMP, KEY_TYPE_SYNC, KEY_TYPE_SYNC_TXT}, KEY_TYPE_SYNC + "=" + TYPE_DST, null, null, null, null);
    }
    
    public Cursor fetchSrcFolders() {

        return mDb.query(DATABASE_TABLE, new String[] {KEY_ID, KEY_FOLDER_ID, KEY_PATH,
        		KEY_TIMESTAMP, KEY_TYPE_SYNC, KEY_TYPE_SYNC_TXT}, KEY_TYPE_SYNC + "=" + TYPE_SRC, null, null, null, null);
    }
    
    public Cursor fetchFolder(long rowId) throws SQLException {

        Cursor mCursor =

            mDb.query(true, DATABASE_TABLE, new String[] {KEY_ID, KEY_FOLDER_ID, KEY_PATH,
            		KEY_TIMESTAMP, KEY_TYPE_SYNC, KEY_TYPE_SYNC_TXT}, KEY_ID + "=" + rowId, null,
                    null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;

    }
    
    public boolean updateFolder(long rowId, String folderId, String path, String timestamp, int typeSync) {
        ContentValues args = new ContentValues();
        args.put(KEY_FOLDER_ID, folderId);
        args.put(KEY_PATH, path);
        args.put(KEY_TIMESTAMP, timestamp);
        args.put(KEY_TYPE_SYNC, typeSync);
        args.put(KEY_TYPE_SYNC_TXT, typeSync == TYPE_SRC?TYPE_SRC_TXT:TYPE_DST_TXT);

        return mDb.update(DATABASE_TABLE, args, KEY_ID + "=" + rowId, null) > 0;
    }
    
    public String getSrcFolderById(String folderId) {
    	Cursor mCursor =
    	mDb.query(true, DATABASE_TABLE, new String[] {KEY_ID, KEY_FOLDER_ID, KEY_PATH,
        		KEY_TIMESTAMP, KEY_TYPE_SYNC, KEY_TYPE_SYNC_TXT}, KEY_TYPE_SYNC + "=" + TYPE_SRC_TXT + " and " + KEY_FOLDER_ID + "=" + folderId, null,
                null, null, null, null);
    	mCursor.moveToFirst();
    	mCursor.getString(mCursor.getColumnIndex(KEY_PATH));
		return mCursor.getString(mCursor.getColumnIndex(KEY_PATH));
    }
}
