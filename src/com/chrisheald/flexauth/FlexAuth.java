package com.chrisheald.flexauth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

public class FlexAuth extends Activity {
	static final int MENU_ADD_AUTH = 1;	
	static final int MENU_INFO = 2;
	static final int MENU_RESYNC = 3;
	static final int DIALOG_DATA_MISSING = 2;
	static final int DIALOG_BAD_SERIAL = 3;
	static final int VIEW_ID = 1;
	static final int DELETE_ID = 2;
	static final int NEW_TOKEN = 1;
	static final int REFRESH = 1;
	static final int NOTIFY_SYNCED = 2;
	
	public static final Exception InvalidSerialException = null;
	private Handler mHandler = new Handler();
	private Handler mRefresh = new Handler() {
		public void handleMessage(Message msg) {
			switch(msg.what) {
			case REFRESH:
				updateTokenList();
			case NOTIFY_SYNCED:
				Toast.makeText(FlexAuth.this, "Resync successful!", 4).show();
			}
		}
	};
	
	
	private SQLiteDatabase db, readDb;
	private TokenAdapter tAdapter;
	
	private int lastMod = -1;
	private Runnable mUpdateTimeTask = new Runnable() {
		public void run() {
			int mod = (int)((System.currentTimeMillis() + Token.timeOffset) % 30000L);
			ProgressBar pb = (ProgressBar)findViewById(R.id.ProgressBar01);
			if(lastMod == -1 || mod < lastMod) {
				updateTokenList();
			}
			lastMod = mod;
			if(pb != null) pb.setProgress(mod);
			mHandler.postDelayed(this, 50);
		}
	};

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        AppDb dbHelper = new AppDb(this);
        db = dbHelper.write_db();
        readDb = dbHelper.read_db();
        
        SharedPreferences settings = getSharedPreferences("tokenprefs", 0);
        Token.timeOffset = settings.getLong("offset", 0);        

        new Thread() {
        	public void run() {
            	try {
        			long offset = Token.fetchTimeOffset();
    				SharedPreferences settings = getSharedPreferences("tokenprefs", 0);
    				SharedPreferences.Editor editor = settings.edit();
    				editor.putLong("offset", offset);        			
        		} catch (IOException e) {}
        	}
        }.start();
        
        viewList();
        
        Button addNew = (Button)findViewById(R.id.addNewAccount);
        addNew.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View v) {
        		newToken();
        	}
        });
        
		mHandler.removeCallbacks(mUpdateTimeTask);
		mHandler.postDelayed(mUpdateTimeTask, 50);
    }
    
    public void finalize() {
    	db.close();
    	readDb.close();
    }
    
    private void updateTokenList() {
    	int count = TokenAdapter.GetTokenList(tAdapter, readDb);
		
		findViewById(R.id.tokenList).setVisibility(count == 0 ? View.GONE : View.VISIBLE);
		findViewById(R.id.noTokensLabel).setVisibility(count > 0 ? View.GONE : View.VISIBLE);
		findViewById(R.id.addNewAccount).setVisibility(count > 0 ? View.GONE : View.VISIBLE);
    }
    
    private void viewToken(long tokenId) {
    	Token t = tAdapter.items.get((int)tokenId);
    	Intent intent = new Intent(this, TokenView.class);

    	intent.putExtra("secret", t.secret);
    	intent.putExtra("serial", t.serial);
    	String latestAuthCode = null;
    	try {
    		latestAuthCode = t.getPassword();
		} catch (InvalidKeyException e) {
			latestAuthCode = "<error>";
		} catch (NoSuchAlgorithmException e) {
			latestAuthCode = "<error>";
		}    	
    	intent.putExtra("auth", latestAuthCode);
    	startActivity(intent);
    }
    
    private void viewList() {
        setContentView(R.layout.main);
        ListView lv = (ListView)findViewById(R.id.tokenList);
        tAdapter = new TokenAdapter(this, R.layout.token_row, new ArrayList<Token>());
        lv.setAdapter(tAdapter);
        lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				viewToken(arg3);				
			}
        });        
        registerForContextMenu(lv);
        updateTokenList();
    }
    
    private void destroyToken(long tokenId) {
    	final Token t = tAdapter.items.get((int)tokenId);
    	if(t._id != -1) {
	    	new AlertDialog.Builder(this)	    	
	    	.setIcon(R.drawable.shocked)
	    	.setTitle(R.string.delete_confirm)
	    	.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
	    		public void onClick(DialogInterface dialog, int which) {
	    			Object[] args = {t._id};
	    			db.execSQL("DELETE FROM accounts WHERE id = ?", args);
	    			updateTokenList();
	    		}
	    	})
	    	.setNegativeButton(R.string.no, null)	    	
	    	.show();
    	}
    }
    
	public void resync() {
		final ProgressDialog progress = ProgressDialog.show(this, "", "Synchronizing. Please wait...", true);
        new Thread() {
        	public void run() {
    			long offset;
				try {
					offset = Token.fetchTimeOffset();
					SharedPreferences settings = getSharedPreferences("tokenprefs", 0);
					SharedPreferences.Editor editor = settings.edit();
					editor.putLong("offset", offset); 
					progress.dismiss();
					mRefresh.sendEmptyMessage(REFRESH);
					mRefresh.sendEmptyMessage(NOTIFY_SYNCED);
				} catch (IOException e) {
					progress.dismiss();
	    			new AlertDialog.Builder(FlexAuth.this)
	    			.setMessage("Failed to connect to Blizzard's authentication servers")
	    			.setTitle("Sync failed")
	    			.setNeutralButton("OK", new DialogInterface.OnClickListener() {
	    		           public void onClick(DialogInterface dialog, int id) {
	    		                dialog.cancel();
	    		           }
	    				})
	    			.setIcon(android.R.drawable.stat_sys_warning)
	    			.show();	    		  
				}
        	}
        }.start();
	}    
    
    public boolean onCreateOptionsMenu(Menu menu) {
    	menu.add(0, MENU_ADD_AUTH, 0, "Add Account").setIcon(android.R.drawable.ic_menu_add);
    	menu.add(0, MENU_RESYNC, 0, "Resync").setIcon(android.R.drawable.ic_menu_recent_history);
    	menu.add(0, MENU_INFO, 0, "Info").setIcon(android.R.drawable.ic_menu_info_details);
    	return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) {
    	case MENU_ADD_AUTH:
    		newToken();
    		return true;
    	case MENU_INFO:
    		InputStream is;
    		String msg = "";
			try {
				is = this.getAssets().open("license.txt");
	            if (is != null) {
	                StringBuilder sb = new StringBuilder();
	                String line;
	
	                try {
	                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
	                    while ((line = reader.readLine()) != null) {
	                        sb.append(line).append("\n");
	                    }
	                } finally {
	                    is.close();
	                }
	                msg = sb.toString();
	            }
			} catch (IOException e) {
				msg = "<Unable to open license file>";
			}
	            
            
    		new AlertDialog.Builder(this)
    		.setMessage(msg)
    		.setTitle("License & Information")
    		.setIcon(android.R.drawable.ic_menu_info_details)
    		.show();
    		return true;
    	case MENU_RESYNC:
    		resync();
    		return true;
    	}
    	return false;
    }
    
    protected void newToken() {
    	Intent i = new Intent(this, AddToken.class);
    	startActivityForResult(i, NEW_TOKEN);
    }
    
    protected Dialog onCreateDialog(int id) {
    	Dialog dialog;
    	CharSequence msg;
    	switch(id) {
    	case DIALOG_DATA_MISSING:
    		msg = getResources().getText(R.string.invalid_data);
    		dialog = new AlertDialog.Builder(this).setMessage(msg).create();
    	case DIALOG_BAD_SERIAL:
    		msg = getResources().getText(R.string.invalid_data);
    		dialog = new AlertDialog.Builder(this).setMessage(msg).create();
    	default:
    		dialog = null;
    	}
    	return dialog;
    }
    
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    	super.onCreateContextMenu(menu, v, menuInfo);
    	menu.add(0, VIEW_ID, 0, "View Details");
    	menu.add(0, DELETE_ID, 0,  "Delete Token");
    }
    
    public boolean onContextItemSelected(MenuItem item) {
    	AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    	long id = tAdapter.getItemId(info.position);
    	switch (item.getItemId()) {
    	case VIEW_ID:
    		viewToken(id);
    		return true;
    	case DELETE_ID:
    		destroyToken(id);
    		return true;
    	default:
    		super.onContextItemSelected(item);
    	}
    	return false;
    }
    
    @Override 
    public void onActivityResult(int requestCode, int resultCode, Intent data) {     
      super.onActivityResult(requestCode, resultCode, data);
      switch(requestCode) {
	      case(NEW_TOKEN): {
	    	  if(resultCode == Activity.RESULT_OK) {
	    		  updateTokenList();
	    			new AlertDialog.Builder(FlexAuth.this)
	    			.setMessage("Be sure to back up your token secret to a secure location! If you uninstall the app or delete the entry, the tokens stored here will be erased and you won't be able to generate authenticator codes.\n\nTo restore access, add a new token with your backed up secret.")
	    			.setTitle("Token added!")
	    			.setNeutralButton("OK", new DialogInterface.OnClickListener() {
	    		           public void onClick(DialogInterface dialog, int id) {
	    		                dialog.cancel();
	    		           }
	    				})
	    			.setIcon(android.R.drawable.stat_sys_warning)
	    			.show();	    		  
	    	  }
	      }
      }
    }
}