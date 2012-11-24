package com.chrisheald.flexauth;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.http.client.ClientProtocolException;

import com.chrisheald.flexauth.Token.RestoreException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class AddToken extends Activity {
	static final int RECEIVED_TOKEN = 1;
	static final int ALERT_MSG = 2;
	static final int RECEIVED_RESTORE = 3;
	private EditText accountName;
	private EditText serial;
	private EditText secret;
	private EditText rstcode;
	private TextView regionLabel;
	private Spinner region;
	private Button generate;
	private Button restore;
	private SQLiteDatabase db;
	private Context context;
	private DialogInterface.OnClickListener cancel;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.new_account);
		context = this;
		
		findViewById(R.id.tokenForm).setVisibility(View.GONE);
		findViewById(R.id.tokenTypeSelect).setVisibility(View.VISIBLE);

		AppDb dbHelper = new AppDb(context);
        db = dbHelper.write_db();
		
	    region = (Spinner) findViewById(R.id.regionSelect);
	    regionLabel = (TextView) findViewById(R.id.regionLabel);
	    generate = (Button) findViewById(R.id.requestToken);
	    restore = (Button) findViewById(R.id.restoreToken);
	    ArrayAdapter adapter = ArrayAdapter.createFromResource(context,
	    		R.array.regions, android.R.layout.simple_spinner_item);
	    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    region.setAdapter(adapter);
		
		accountName = (EditText)findViewById(R.id.accountName);
		serial = (EditText)findViewById(R.id.tokenSerial);
		secret = (EditText)findViewById(R.id.tokenSecret);
		rstcode = (EditText)findViewById(R.id.tokenRestore);

		accountName.setText("");
		
		cancel = new DialogInterface.OnClickListener() {
           public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
           }
		};
	}


	private Handler messageHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case(RECEIVED_RESTORE): {
					Token t = (Token)msg.obj;
					showEntryForm(t.secret, t.serial);
					break;
				}
				case(RECEIVED_TOKEN): {
					Token t = (Token)msg.obj;
					showEntryForm(t.secret, t.serial);
					break;
				}
				case(ALERT_MSG): {
					String errorMsg = (String)msg.obj;
					new AlertDialog.Builder(context)
					.setMessage(errorMsg)
					.setTitle("Error")
					.setNeutralButton("OK", cancel)
					.setIcon(android.R.drawable.stat_notify_error)
					.show();
					break;
				}
			}
		}

	};
	
	private void showEntryForm(String sSecret, String sSerial) {
		findViewById(R.id.tokenForm).setVisibility(View.VISIBLE);
		findViewById(R.id.tokenTypeSelect).setVisibility(View.GONE);
		EditText serial = (EditText) findViewById(R.id.tokenSerial);
		EditText secret = (EditText) findViewById(R.id.tokenSecret);
		serial.setText(sSerial);
		secret.setText(sSecret);
	}

	public void restoreToken(View target) {
		findViewById(R.id.tokenForm).setVisibility(View.VISIBLE);
		findViewById(R.id.tokenTypeSelect).setVisibility(View.GONE);
		EditText secret = (EditText) findViewById(R.id.tokenSecret);
		EditText serial = (EditText) findViewById(R.id.tokenSerial);
		serial.setText("");
		secret.setText("");
		secret.setEnabled(false);
		rstcode.setText("");
		region.setVisibility(View.GONE);
		regionLabel.setVisibility(View.GONE);
		generate.setVisibility(View.GONE);	
		restore.setVisibility(View.VISIBLE);
		rstcode.setVisibility(View.VISIBLE);
	}

	public void newManualToken(View target) {
		showEntryForm("", "");
		region.setVisibility(View.GONE);
		regionLabel.setVisibility(View.GONE);
		generate.setVisibility(View.GONE);	
		restore.setVisibility(View.GONE);	
		rstcode.setVisibility(View.GONE);
	}
	  
	public void requestNewToken(View target) {
		final ProgressDialog progress = ProgressDialog.show(this, "", "Requesting token. Please wait...", true);
		region.setVisibility(View.VISIBLE);
		regionLabel.setVisibility(View.VISIBLE);
		generate.setVisibility(View.VISIBLE);
		rstcode.setVisibility(View.GONE);
        new Thread() {
        	public void run() {
        		Token t = generate((String) region.getSelectedItem());
           		progress.dismiss();
           		Message msg = messageHandler.obtainMessage(RECEIVED_TOKEN, t);
           		messageHandler.sendMessage(msg); 
        	}
        }.start();
	}

	public void restoreOldToken(View target) {
		final ProgressDialog progress = ProgressDialog.show(this, "", "Requesting token. Please wait...", true);
        new Thread() {
        	public void run() {
        		Token t = restore(serial.getText().toString().trim(), rstcode.getText().toString().trim());
           		progress.dismiss();
           		Message msg = messageHandler.obtainMessage(RECEIVED_RESTORE, t);
           		messageHandler.sendMessage(msg); 
        	}
        }.start();
	}

	public void saveToken(View target) {
		accountName = (EditText)findViewById(R.id.accountName);
		
		String n = accountName.getText().toString().trim();
		String ss = serial.getText().toString().trim();
		String sl = secret.getText().toString().trim();
		
		String error = null;
		if(n.compareTo("") == 0) {
			error = "Please enter a name for this token";
			accountName.setError(error);
		} else if(ss.compareTo("") == 0) {
			error = "Please enter a serial for this token";
			serial.setError(error);
		} else if (sl.length() != 40) {
			error = "Token secret must be exactly 40 characters long";
			secret.setError(error);
		} else if(sl.compareTo("") == 0) {
			error = "Please enter a secret for this token";
			secret.setError(error);
		}
		if(error != null) return;
		String[] args = {n, ss, sl};				
		db.execSQL("INSERT INTO accounts (name, serial, secret) VALUES (?, ?, ?)", args);
		
		Toast.makeText(context, "Token successfully added!", 4).show();
		
		setResult(Activity.RESULT_OK, new Intent());
		finish();
	}

	public void generateToken(View target) {
		requestNewToken(null);
	}

	private Token generate(String region) {
		Token t = new Token(); 
		t.setRegion(region);
		String errMsg = null;
		try {
			t.generate();
		} catch (NoSuchAlgorithmException e) {
			errMsg = "HMAC-SHA1 is not supported on this device.";
		} catch (ClientProtocolException e) {
			errMsg = "Error in client protocol.";
		} catch (IOException e) {
			errMsg = "Couldn't establish a network connection. Please try again later.";
		} catch (InvalidSerialException e) {
			errMsg = "Failed to generate a valid serial. Please try again.";
		}
		if(errMsg != null) {
	   		Message msg = messageHandler.obtainMessage(ALERT_MSG, errMsg);
	   		messageHandler.sendMessage(msg);
		}
		return t;
	}
	
	private Token restore(String serial, String restorecode)
	{
		String errMsg = null;
		Token t = new Token();
		try {
			t.Restore(serial, restorecode);
		} catch (NoSuchAlgorithmException e) {
			errMsg = "HMAC-SHA1 is not supported on this device.";
		} catch (ClientProtocolException e) {
			errMsg = "Error in client protocol.";
		} catch (IOException e) {
			errMsg = "Couldn't establish a network connection. Please try again later.";
		} catch (InvalidSerialException e) {
			errMsg = "Failed to generate a valid serial. Please try again.";
		} catch (InvalidKeyException e) {
			errMsg = "Invalid Key";
		} catch (RestoreException e) {
			errMsg = "Restore Error: " + e.getMessage();
		} catch (NoSuchPaddingException e) {
			errMsg = "RSA Error";
		} catch (NoSuchProviderException e) {
			errMsg = "RSA Error";
		} catch (InvalidKeySpecException e) {
			errMsg = "RSA Error";
		} catch (IllegalBlockSizeException e) {
			errMsg = "RSA Error";
		} catch (BadPaddingException e) {
			errMsg = "RSA Error";
		}
		if(errMsg != null) {
	   		Message msg = messageHandler.obtainMessage(ALERT_MSG, errMsg);
	   		messageHandler.sendMessage(msg);
		}
		return t;
	}
}
