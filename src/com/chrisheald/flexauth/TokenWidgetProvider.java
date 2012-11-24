package com.chrisheald.flexauth;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.SystemClock;
import android.widget.RemoteViews;

public class TokenWidgetProvider extends AppWidgetProvider {
	public static String URI_SCHEME = "flexauth";

	public static void UpdateWidget(Context context, AppWidgetManager mgr, String code, String name, boolean valid, int id) {
		RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.widget);
		updateViews.setTextViewText(R.id.widgetCode, code);
		updateViews.setTextViewText(R.id.widgetName, name);
		updateViews.setTextColor(R.id.widgetCode, valid?Color.GREEN:Color.RED);
		
		
		Intent intent = new Intent(context, FlexAuth.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        // Get the layout for the App Widget and attach an on-click listener to the button
        updateViews.setOnClickPendingIntent(R.id.widgetBody, pendingIntent);		
		mgr.updateAppWidget(id, updateViews);
	}
	
	@Override
	public void onUpdate(Context context, AppWidgetManager mgr, int[] appWidgetIds) {
		for (int mAppWidgetId : appWidgetIds) {
	        SharedPreferences config = context.getSharedPreferences(TokenWidgetConfig.PREFS_NAME, 0);
	        String secret = config.getString(String.format(TokenWidgetConfig.PREFS_SECRET_PATTERN, mAppWidgetId), null);
	        String name = config.getString(String.format(TokenWidgetConfig.PREFS_NAME_PATTERN, mAppWidgetId), "<no name>");
	        boolean expire = ((System.currentTimeMillis() + Token.timeOffset) % 30000) < 24000;
	        
			Token t = new Token();
			String code = "<no token>";
			if(secret != null) {
				t.secret = secret;
				try {
					code = t.getPassword();
				} catch (InvalidKeyException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			UpdateWidget(context, mgr, code, name, expire, mAppWidgetId);
		}
	}
	
	// Fix SDK 1.5 Bug per note here:
	// http://developer.android.com/guide/topics/appwidgets/index.html#AppWidgetProvider
	// linking to this post:
	// http://groups.google.com/group/android-developers/msg/e405ca19df2170e2
	@Override
	public void onReceive(Context context, Intent intent) {
		final String action = intent.getAction();
		if (AppWidgetManager.ACTION_APPWIDGET_DELETED.equals(action)) {
			final int appWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
			if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
				this.onDeleted(context, new int[] { appWidgetId });
			}
		} else if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)) {
			if (!URI_SCHEME.equals(intent.getScheme())) {
				final int[] appWidgetIds = intent.getExtras().getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS);
				for (int appWidgetId : appWidgetIds) {
					Intent widgetUpdate = new Intent();
					widgetUpdate.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
					widgetUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] { appWidgetId });
	
					widgetUpdate.setData(
						Uri.withAppendedPath(
							Uri.parse(TokenWidgetProvider.URI_SCHEME + "://widget/id/"),
							String.valueOf(appWidgetId)
						)
					);
					PendingIntent newPending = PendingIntent.getBroadcast(context, 0, widgetUpdate,
							PendingIntent.FLAG_UPDATE_CURRENT);
	
					// schedule the updating
					AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
					alarms.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), 15000, newPending);
				}
			}
			super.onReceive(context, intent);
		} else {
			super.onReceive(context, intent);
		}
	}	
	
	@Override
	public void onDisabled(Context context) {
		Intent widgetUpdate = new Intent();
        widgetUpdate.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        widgetUpdate.setData(Uri.parse("flexauth://widget"));
        PendingIntent newPending = PendingIntent.getBroadcast(context, 0, widgetUpdate, PendingIntent.FLAG_UPDATE_CURRENT);

        // schedule the updating
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarms.cancel(newPending);
	}
	
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            // stop alarm
            Intent widgetUpdate = new Intent();
            widgetUpdate.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            widgetUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            widgetUpdate.setData(Uri.withAppendedPath(Uri.parse(URI_SCHEME + "://widget/id/"), String.valueOf(appWidgetId)));
            PendingIntent newPending = PendingIntent.getBroadcast(context, 0, widgetUpdate, PendingIntent.FLAG_UPDATE_CURRENT);

            AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarms.cancel(newPending);

            // remove preference
            SharedPreferences config = context.getSharedPreferences(TokenWidgetConfig.PREFS_NAME, 0);
            SharedPreferences.Editor configEditor = config.edit();
            configEditor.remove(String.format(TokenWidgetConfig.PREFS_SECRET_PATTERN, appWidgetId));
            configEditor.commit();
        }

        super.onDeleted(context, appWidgetIds);
    }	
}
