package com.chrisheald.flexauth;

import android.app.Activity;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class TokenView extends Activity {
	private final static int COPY_ID = 1;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.view_token);
    	TextView secret = (TextView)findViewById(R.id.tokenSecretText);
    	TextView serial = (TextView)findViewById(R.id.tokenSerialText);
    	TextView authCode = (TextView)findViewById(R.id.authCode);
		secret.setText(getIntent().getStringExtra("secret"));
		serial.setText(getIntent().getStringExtra("serial"));
		authCode.setText(getIntent().getStringExtra("auth"));
	}
	
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    	super.onCreateContextMenu(menu, v, menuInfo);
    	menu.add(0, COPY_ID, 0, "Copy");
    }
    
    public boolean onContextItemSelected(MenuItem item) {
    	AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    	switch (item.getItemId()) {
    	case COPY_ID:
    		ClipboardManager c = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
    		c.setText(((TextView)info.targetView).getText());
    		Toast.makeText(this, "Text copied to clipboard", 3);
    		return true;
    	default:
    		super.onContextItemSelected(item);
    	}
    	return false;
    }	
}
