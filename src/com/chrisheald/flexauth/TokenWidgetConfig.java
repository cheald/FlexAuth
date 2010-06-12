package com.chrisheald.flexauth;

import java.util.ArrayList;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.view.KeyEvent;
import android.widget.RemoteViews;

public class TokenWidgetConfig extends PreferenceActivity {
	ArrayList<CharSequence> tokenIDs = new ArrayList<CharSequence>();
	ArrayList<CharSequence> tokenLabels = new ArrayList<CharSequence>();
	private static String CONFIGURE_ACTION = "android.appwidget.action.APPWIDGET_CONFIGURE";

	private void populateTokenEntries() {
		AppDb dbHelper = new AppDb(this);
		SQLiteDatabase readDb = dbHelper.read_db();

		Cursor c = null;
		try {
			tokenIDs.clear();
			tokenLabels.clear();
			c = readDb.rawQuery("select * from accounts order by id asc", null);
			c.moveToFirst();
			while (!c.isAfterLast()) {
				tokenIDs.add(c.getString(c.getColumnIndexOrThrow("secret")));
				tokenLabels.add(c.getString(c.getColumnIndexOrThrow("name")));
				c.moveToNext();
			}
		} finally {
			if (c != null)
				c.close();
		}
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.widget_prefs);

		ListPreference token = (ListPreference) findPreference("token");
		populateTokenEntries();

		CharSequence[] labels = (CharSequence[]) tokenIDs
				.toArray(new CharSequence[tokenLabels.size()]);
		CharSequence[] vals = (CharSequence[]) tokenIDs
				.toArray(new CharSequence[tokenIDs.size()]);

		token.setEntryValues(vals);
		token.setEntries(labels);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK
				&& Integer.parseInt(Build.VERSION.SDK) < 5) {
			onBackPressed();
		}

		return (super.onKeyDown(keyCode, event));
	}

	@Override
	public void onBackPressed() {
		if (CONFIGURE_ACTION.equals(getIntent().getAction())) {
			Intent intent = getIntent();
			Bundle extras = intent.getExtras();

			if (extras != null) {
				int id = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
						AppWidgetManager.INVALID_APPWIDGET_ID);
				AppWidgetManager mgr = AppWidgetManager.getInstance(this);
				RemoteViews views = new RemoteViews(getPackageName(),
						R.layout.widget);

				mgr.updateAppWidget(id, views);

				Intent result = new Intent();

				result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
				setResult(RESULT_OK, result);
				sendBroadcast(new Intent(this, TwitterWidget.class));
			}
		}

		super.onBackPressed();
	}

}
