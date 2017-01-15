package co.cloudcoin.cc;

import android.widget.ImageButton;


import android.app.Activity;
import android.os.Bundle;

import android.view.View;
import android.widget.TextView;
import android.widget.ImageView;
import android.content.Context;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.content.Intent;
import android.view.Window;
import android.os.Handler;
import android.graphics.Color;
import android.view.ViewGroup;

import android.graphics.drawable.Drawable;
import android.graphics.Typeface;
import android.util.Log;
import android.view.WindowManager;
import android.view.Display;
import android.util.DisplayMetrics;

import android.view.MotionEvent;
import java.lang.Thread;
import android.os.Handler;
import java.lang.Runnable;

import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;

import java.util.Locale;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import android.app.AlertDialog;
import android.content.DialogInterface;

import android.net.Uri;

import android.content.pm.PackageManager.NameNotFoundException;
import android.content.ActivityNotFoundException;




import android.widget.FrameLayout;
import android.widget.LinearLayout;

import android.view.Gravity;

import android.widget.RelativeLayout;



public class MainActivity extends Activity implements OnClickListener {

	TextView tv;
	Button bt;

	boolean asyncFinished;

	ImageButton bt0, bt1, bt2;



	SharedPreferences mSettings;
	static public String version;



	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);

		asyncFinished = false;

		init();

		Thread myThread = new Thread(new Runnable() {
			public void run() {
				RAIDA.updateRAIDAList(MainActivity.this);

				MainActivity.this.runOnUiThread(new Runnable() {
					public void run() {
						asyncFinished = true;		
					}
				});
			}
		});

		myThread.start();
	}


	public void init() {
		try {  
			version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			version = "";
		}

		bt0 = (ImageButton) findViewById(R.id.iadd);
		bt0.setBackgroundResource(R.drawable.add);
		bt0.setOnClickListener(this);

		bt1 = (ImageButton) findViewById(R.id.ibank);
		bt1.setBackgroundResource(R.drawable.bank);
		bt1.setOnClickListener(this);

		bt2 = (ImageButton) findViewById(R.id.ispend);
		bt2.setBackgroundResource(R.drawable.spend);
		bt2.setOnClickListener(this);

		mSettings = PreferenceManager.getDefaultSharedPreferences(this);
	}


	public void onBackPressed() {
		final Context mContext = this;

		super.onBackPressed();
	}

	public void onPause() {
		super.onPause();
		
	}

	public void onResume() {
		super.onResume();
	}

	public void onDestroy() {
		super.onDestroy();
	}


	public void onClick(View v) {
		final int id;
		Intent intent;
		int state;

		id = v.getId();

		switch (id) {
			case R.id.iadd:
				if (!asyncFinished) 
					return;

				intent = new Intent(this, AddCoinsActivity.class);
				startActivity(intent);
				break;

			case R.id.ibank:
				intent = new Intent(this, BankActivity.class);
				startActivity(intent);
				break;

			case R.id.ispend:
				intent = new Intent(this, SpendCoinsActivity.class);
				startActivity(intent);
				break;
			default:
				break;
				
		}
	}
}
