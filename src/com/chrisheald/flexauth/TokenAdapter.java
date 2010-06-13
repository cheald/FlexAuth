package com.chrisheald.flexauth;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class TokenAdapter extends ArrayAdapter<Token> {
	public ArrayList<Token> items;
	private Context context;
	
	public TokenAdapter(Context context, int textViewResourceId, ArrayList<Token> items) {
        super(context, textViewResourceId, items);
        this.items = items;
        this.context = context;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		if (v == null) {
			LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = vi.inflate(R.layout.token_row, null);
		}
		Token o = items.get(position);
		if (o != null) {
			TextView tt = (TextView) v.findViewById(R.id.toptext);
			TextView bt = (TextView) v.findViewById(R.id.bottomtext);
			if (tt != null) {
				tt.setText(o.name);
			}
			if (bt != null) {
				try {
					bt.setText(o.getPassword());
				} catch (InvalidKeyException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return v;
	}
	
	public static int GetTokenList(TokenAdapter tAdapter, SQLiteDatabase readDb) {
    	Cursor c = null;
    	int count = 0;
		try {
			c = readDb.rawQuery(
					"select * from accounts order by id asc", null);
			c.moveToFirst();
			tAdapter.clear();
			tAdapter.notifyDataSetChanged();
			while (!c.isAfterLast()) {
				String name = c.getString(c.getColumnIndexOrThrow("name"));
				String secret = c.getString(c.getColumnIndexOrThrow("secret"));
				String serial = c.getString(c.getColumnIndexOrThrow("serial"));
				Token t = new Token(name, secret, serial);
				t._id = c.getInt(c.getColumnIndexOrThrow("id"));
				c.moveToNext();
				tAdapter.add(t);
				count += 1;
			}
			tAdapter.notifyDataSetChanged();
		} finally {
			if(c != null) c.close();
    	}
		return count;
	}
}