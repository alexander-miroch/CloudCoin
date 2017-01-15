package co.cloudcoin.cc;

import android.widget.ImageButton;

import android.content.res.Resources;
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

import android.os.AsyncTask;

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
import android.widget.Toast;
import android.widget.EditText;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;


public class AddCoinsActivity extends Activity implements OnClickListener {


	static String TAG = "CLOUDCOIN";

	static int STATE_INIT = 1;
	static int STATE_IMPORT = 2;
	static int STATE_FIX = 3;
	static int STATE_DONE = 4;

	int state;

	Typeface tf;
	TextView tvt, mainText, dotsView;
	Button button;
	//EditText et;

	Bank bank;

	int count = 0;
	int raidaStatus = 0;

	Handler mHandler;
	

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.addcoins);

		tf = Typeface.createFromAsset(getAssets(), "fonts/font.ttf");

		//et = (EditText) findViewById(R.id.importtag);
		tvt = (TextView) findViewById(R.id.title);
		dotsView = (TextView) findViewById(R.id.dots);

		mainText = (TextView) findViewById(R.id.text);

		button = (Button) findViewById(R.id.button);
		button.setOnClickListener(this);

		mHandler = new Handler(Looper.getMainLooper()) {
			public void handleMessage(Message inputMessage) {
				int what = inputMessage.what;

				raidaStatus++;
				setDots();
			
			}
		};
	}

	private void setDots() {

		String s = "";
		for (int i = 0; i < raidaStatus; i++)
			s += ".";

		dotsView.setText(s);
	}

	Handler getHandler() {
		return mHandler;
	}

	public void onPause() {
		super.onPause();
	}

	public void onResume() {
		super.onResume();
		int totalIncomeLength;

		state = STATE_INIT;

		hideControls();

		bank = new Bank(this);

		String importDir = bank.getRelativeImportDirPath();
		if (importDir == null) {
			mainText.setText(R.string.errmnt);
			return;
		}
		
		if (!bank.examineImportDir()) {
			mainText.setText(R.string.errimport);
			return;
		}

		String result;
		totalIncomeLength = bank.getLoadedIncomeLength();
		if (totalIncomeLength == 0) {
			result = String.format(getResources().getString(R.string.erremptyimport), importDir);

			mainText.setText(result); 
			return;
		}

		result = String.format(getResources().getString(R.string.importwarn), importDir, totalIncomeLength);
		mainText.setText(result);

		showControls();

	}

	private String getImportResultString() {
		String result;
		String resultString;
		Resources res = getResources();

		StringBuilder sb = new StringBuilder();
		
		result = String.format(res.getString(R.string.movedtobank), bank.getImportStats(Bank.STAT_VALUE_MOVED_TO_BANK));
		sb.append(result);
		
		sb.append("\n");
		sb.append(res.getString(R.string.importresults));
		sb.append("\n\n");

		sb.append(res.getString(R.string.importresultsauth));
		sb.append(bank.getImportStats(Bank.STAT_AUTHENTIC));
		sb.append("\n");
		
		sb.append(res.getString(R.string.importresultstrash));
		//sb.append(bank.getImportStats(Bank.STAT_COUNTERFEIT));
		sb.append(bank.getImportStats(Bank.STAT_FAILED));

		if (bank.getImportStats(Bank.STAT_FAILED) != 0) {
			sb.append("\n\n");
			sb.append(res.getString(R.string.trashnote));
		}

		//sb.append("\nFractured and will be repaired: ");
		//sb.append(bank.getImportStats(Bank.STAT_FRACTURED));

		//sb.append("\nFailed and left untouched: ");
		//sb.append(bank.getImportStats(Bank.STAT_FAILED));
		
	
		return sb.toString();
	
	}

	private String getFrackedStatusString(int progressCoins) {
		String statusString;

		int totalFrackedLength = bank.getFrackedCoinsLength();
		int processedFrackedLength = progressCoins + 1;
		
		statusString = String.format(getResources().getString(R.string.authfrackedstring), processedFrackedLength, totalFrackedLength);

		return statusString;
	}

	private String getStatusString(int progressCoins) {
		String statusString;

		int totalIncomeLength = bank.getLoadedIncomeLength();
		int importedIncomeLength = progressCoins + 1;
		
		statusString = String.format(getResources().getString(R.string.authstring), importedIncomeLength, totalIncomeLength);
		
		return statusString;
	}

	private void showError(String msg) {
		Toast toast = Toast.makeText(getBaseContext(), msg, Toast.LENGTH_LONG);
                toast.show();
	}

	private void doImport() {
		
		new ImportTask().execute();
	}

	private void hideControls() {
		button.setVisibility(View.GONE);
	}

	private void showControls() {
		button.setVisibility(View.VISIBLE);
	}

	public void onClick(View v) {
		int id = v.getId();
		String importTag; 


                switch (id) {
                        case R.id.button:
				if (state == STATE_DONE) {
					finish();
					return;
				}

				hideControls();

				doImport();
				break;	
		}
	}

	class ImportTask extends AsyncTask<String, Integer, String> {
		protected String doInBackground(String... params) {

			state = STATE_IMPORT;

			for (int i = 0; i < bank.getLoadedIncomeLength(); i++) {
				publishProgress(i);
				bank.importLoadedItem(i);	
				
			}

			state = STATE_FIX;

			bank.loadFracked();

			for (int i = 0; i < bank.getFrackedCoinsLength(); i++) {
				publishProgress(i);
				bank.fixFracked(i);
			}

			return "OK";
		}


		protected void onPostExecute(String result) {
			mainText.setText(getImportResultString());
			mainText.setGravity(Gravity.LEFT);
			button.setVisibility(View.VISIBLE);
			dotsView.setVisibility(View.GONE);
			state = STATE_DONE;
		}
		protected void onPreExecute() {
			mainText.setText(getStatusString(0));
		}
		protected void onProgressUpdate(Integer... values) {
			if (state == STATE_FIX)
				mainText.setText(getFrackedStatusString(values[0]));
			else if (state == STATE_IMPORT)
				mainText.setText(getStatusString(values[0]));

			raidaStatus = 0;
			setDots();
	        }

	}
}

