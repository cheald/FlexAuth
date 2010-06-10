package com.chrisheald.flexauth;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AppDb {
	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "tokens.db";
	private Context context;
	
	private SQLiteDatabase db;
	private SQLiteDatabase rdb;
	
	public AppDb(Context context) {
		this.context = context;
		AppDbHelper openHelper = new AppDbHelper(context);
		db = openHelper.getWritableDatabase();
		rdb = openHelper.getReadableDatabase();
	}
	
	public SQLiteDatabase write_db() {
		return this.db;		
	}
	
	public SQLiteDatabase read_db() {
		return this.rdb;		
	}
	
	private class AppDbHelper extends SQLiteOpenHelper {
		AppDbHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}
	
		@Override
		public void onCreate(SQLiteDatabase db) {
			String sql = context.getResources().getText(R.string.create_table).toString();
			db.execSQL(sql);
		}
		
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			String sql = context.getResources().getText(R.string.drop_table).toString();
			db.execSQL(sql);
			onCreate(db);
		}
	}
}