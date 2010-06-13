package com.chrisheald.flexauth;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class TokenWidgetConfig extends Activity {
	private TokenAdapter tAdapter;
	public static final String PREFS_NAME = "FlexAuthWidgetPrefs";
	public static final String PREFS_SECRET_PATTERN = "Secret-%d";
	public static final String PREFS_NAME_PATTERN = "Name-%d";

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        AppDb dbHelper = new AppDb(this);
        final SQLiteDatabase readDb = dbHelper.read_db();
        
        Intent launchIntent = getIntent();
		Bundle extras = launchIntent.getExtras();
		if (extras == null) {
			finish();
		}
        final int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
		Intent cancelResultValue = new Intent();
		cancelResultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		setResult(RESULT_CANCELED, cancelResultValue);
		
		setContentView(R.layout.widget_config);
		final SharedPreferences config = getSharedPreferences(PREFS_NAME, 0);
		
        ListView lv = (ListView)findViewById(R.id.tokenSelector);
        tAdapter = new TokenAdapter(this, R.layout.token_row, new ArrayList<Token>());
        lv.setAdapter(tAdapter);
        
        lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                SharedPreferences.Editor configEditor = config.edit();
                Token t = tAdapter.items.get((int)arg3);
                configEditor.putString(String.format(PREFS_SECRET_PATTERN, appWidgetId), t.secret);
                configEditor.putString(String.format(PREFS_NAME_PATTERN, appWidgetId), t.name);
                configEditor.commit();
				
                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
				setResult(RESULT_OK, resultValue);
				
				Context context = TokenWidgetConfig.this;
				AppWidgetManager mgr = AppWidgetManager.getInstance(context);
				String code = "<unknown>";
				try {
					code = t.getPassword();
				} catch (InvalidKeyException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				TokenWidgetProvider.UpdateWidget(context, mgr, code, t.name, appWidgetId);				
				
				finish();
			}
		});
        
        TokenAdapter.GetTokenList(tAdapter, readDb);
	}
}
