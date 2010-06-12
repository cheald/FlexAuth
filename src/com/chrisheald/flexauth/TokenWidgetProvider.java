package com.chrisheald.flexauth;

import java.util.List;

import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

public class TokenWidgetProvider extends AppWidgetProvider {
	@Override
	public void onReceive(Context ctxt, Intent intent) {
		if (intent.getAction() == null) {
			ctxt.startService(new Intent(ctxt, UpdateService.class));
		} else {
			super.onReceive(ctxt, intent);
		}
	}

	@Override
	public void onUpdate(Context ctxt, AppWidgetManager mgr, int[] appWidgetIds) {
		ctxt.startService(new Intent(ctxt, UpdateService.class));
	}

	public static class UpdateService extends IntentService {
		private SharedPreferences prefs = null;

		public UpdateService() {
			super("TokenWidgetProvider$UpdateService");
		}

		@Override
		public void onCreate() {
			super.onCreate();

			prefs = PreferenceManager.getDefaultSharedPreferences(this);
		}

		@Override
		public void onHandleIntent(Intent intent) {
			/*
			ComponentName me = new ComponentName(this, TwitterWidget.class);
			AppWidgetManager mgr = AppWidgetManager.getInstance(this);

			mgr.updateAppWidget(me, buildUpdate(this));
			*/
		}

		private RemoteViews buildUpdate(Context context) {
			RemoteViews updateViews = new RemoteViews(context.getPackageName(),
					R.layout.widget);
			String user = prefs.getString("user", null);
			String password = prefs.getString("password", null);

			/*
			if (user != null && password != null) {
				Twitter client = new Twitter(user, password);
				List<Twitter.Status> timeline = client.getFriendsTimeline();

				if (timeline.size() > 0) {
					Twitter.Status s = timeline.get(0);

					updateViews.setTextViewText(R.id.friend, s.user.screenName);
					updateViews.setTextViewText(R.id.status, s.text);

					Intent i = new Intent(this, TwitterWidget.class);
					PendingIntent pi = PendingIntent.getBroadcast(context, 0,
							i, 0);

					updateViews.setOnClickPendingIntent(R.id.refresh, pi);

					i = new Intent(this, TWPrefs.class);
					pi = PendingIntent.getActivity(context, 0, i, 0);
					updateViews.setOnClickPendingIntent(R.id.configure, pi);
				}
			}
			*/

			return (updateViews);
		}
	}

}
