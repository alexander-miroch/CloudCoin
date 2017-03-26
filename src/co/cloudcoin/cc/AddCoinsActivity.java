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
import android.content.pm.ActivityInfo;
import android.view.Surface;
import android.graphics.Point;
import android.os.Build;

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

import android.net.ConnectivityManager;
import android.net.NetworkInfo;


import java.util.Date;
import java.util.Calendar;
import java.util.ArrayList;

import android.content.SharedPreferences;

public class AddCoinsActivity extends Activity implements OnClickListener {


	static String TAG = "CLOUDCOIN";

	static int STATE_INIT = 1;
	static int STATE_IMPORT = 2;
	static int STATE_FIX = 3;
	static int STATE_DONE = 4;

	ArrayList<String> files;
	int state;

	Typeface tf;
	TextView tvt, mainText, dotsView, headerText;
	Button button, emailButton;
	//EditText et;

	ImageButton ib;
	Bank bank;

	int count = 0;
	int raidaStatus = 0;
	int coinActive = 0;
	int coinTotal = 0;

	Handler mHandler;
	LinearLayout ll;

	final static int REQUEST_CODE_IMPORT_DIR = 1;

	final static int COINS_CNT = 1;

	public static final String APP_PREFERENCES_IMPORTDIR = "pref_importdir";

	SharedPreferences mSettings;

	ImportTask iTask;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.addcoins);

		tf = Typeface.createFromAsset(getAssets(), "fonts/font.ttf");

		tvt = (TextView) findViewById(R.id.title);
		dotsView = (TextView) findViewById(R.id.dots);

		mainText = (TextView) findViewById(R.id.text);
		headerText = (TextView) findViewById(R.id.importtext);

		button = (Button) findViewById(R.id.button);
		button.setOnClickListener(this);

		emailButton = (Button) findViewById(R.id.emailbutton);
		emailButton.setOnClickListener(this);		

		ib = (ImageButton) findViewById(R.id.changebutton);
		ib.setBackgroundResource(R.drawable.ic_chooser);
		ib.setOnClickListener(this);		

		this.files = null;
		this.iTask = null;

		mHandler = new Handler(Looper.getMainLooper()) {
			public void handleMessage(Message inputMessage) {
				int what = inputMessage.what;

				if (what == 0) {
					raidaStatus++;
					setDots();
				} else if (what == COINS_CNT) {
					raidaStatus = 0;
					coinActive = inputMessage.arg1 + 1;
					coinTotal = inputMessage.arg2;
					setDots();
				}
			
			}
		};

		ll = (LinearLayout) findViewById(R.id.fwrapper);

		mSettings = PreferenceManager.getDefaultSharedPreferences(this);

		// save state. not now
		//state = mSettings.getInt("state", STATE_INIT);
		state = STATE_INIT;
	}

	private void setDots() {
		String s;

		if (coinTotal == 0) {
			s = "";
		} else {
			s = getResources().getString(R.string.coin) + " " + coinActive + "/" + coinTotal + ":\n";
		}

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

	public void onDestroy() {
                super.onDestroy();
		if (iTask != null) {
			iTask.doCancel();
			iTask = null;
		}
	}

	public void onResume() {
		super.onResume();
		int totalIncomeLength;
		String result;
		String importDir = "";

		if (state != STATE_INIT)
			return;


		headerText.setVisibility(View.GONE);
		emailButton.setVisibility(View.GONE);
		hideControls();

		if (!isOnline()) {
			mainText.setText(R.string.errconnection);
			return;
		}

		bank = new Bank(this);

		if (this.files != null && this.files.size() > 0) {
			bank.loadIncomeFromFiles(this.files);
		} else {
			String savedImportDir = mSettings.getString(APP_PREFERENCES_IMPORTDIR, "");	

			if (savedImportDir == "") {
				importDir = bank.getDefaultRelativeImportDirPath();
				if (importDir == null) {
					mainText.setText(R.string.errmnt);
					return;
				}
			} else {
				importDir = savedImportDir;
				bank.setImportDirPath(importDir);
			}

		
			if (!bank.examineImportDir()) {
				mainText.setText(R.string.errimport);
				return;
			}
		}

		totalIncomeLength = bank.getLoadedIncomeLength();
		if (totalIncomeLength == 0) {
			result = String.format(getResources().getString(R.string.erremptyimport), importDir);
			mainText.setText(result); 
			return;
		}

		if (this.files != null && this.files.size() > 0) {
			result = String.format(getResources().getString(R.string.importfiles), totalIncomeLength);
		} else {
			result = String.format(getResources().getString(R.string.importwarn), importDir, totalIncomeLength);		}
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
		sb.append(res.getString(R.string.importresultsauth));
		sb.append(bank.getImportStats(Bank.STAT_AUTHENTIC));
		sb.append("\n");
		
		sb.append(res.getString(R.string.importresultstrash));
		sb.append(bank.getImportStats(Bank.STAT_FAILED));

		if (bank.getImportStats(Bank.STAT_FAILED) != 0) {
			sb.append("\n\n");
			sb.append(res.getString(R.string.trashnote));
		}

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
		iTask = new ImportTask();
		iTask.execute();
	}

	private void hideControls() {
		button.setVisibility(View.GONE);
	}

	private void showControls() {
		button.setVisibility(View.VISIBLE);
	}

	public void doEmailReceipt() {
		StringBuilder sb = new StringBuilder();
		Resources res = getResources();

		Date date = new Date();
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);

		int day = cal.get(Calendar.DAY_OF_MONTH);
		int month = cal.get(Calendar.MONTH) + 1;
		int year = cal.get(Calendar.YEAR);
		

		String dayStr = Integer.toString(day);
		String monthStr = Integer.toString(month);

		if (day < 10)
			dayStr = "0" + dayStr;
		if (day < 10)
			monthStr = "0" + monthStr;

		String dateStr = monthStr + "/" + dayStr + "/" + year;

		sb.append(res.getString(R.string.paymentreceived));
		sb.append(" " + dateStr + "\n");
		sb.append(res.getString(R.string.totalreceived));
		sb.append(": " + bank.getImportStats(Bank.STAT_VALUE_MOVED_TO_BANK) + "\n");
		sb.append("\n");
		
		sb.append(res.getString(R.string.serialnumber));
		sb.append("   |   ");
		sb.append(res.getString(R.string.importresult));
		sb.append("\n");
		sb.append("------------------------------------------------------\n");

		String[][] report = bank.getReport();
		for (int i = 0; i < report.length; i++) {
			String[] item = report[i];
			sb.append(String.format("%1$-15s", item[0]));
			sb.append(" ");
			sb.append(String.format("%1$-15s", item[1]));
			sb.append(" ");
			sb.append(item[2]);
			sb.append("CC\n");
		}

		EmailSender email = new EmailSender(this, "", "Import Receipt");
		email.setBody(sb.toString());
		email.openDialog();
	}

	public void doChangeImportFolder() {
		Intent i = new Intent((Context) this, DirPickerActivity.class);
		startActivityForResult(i, REQUEST_CODE_IMPORT_DIR);
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode != REQUEST_CODE_IMPORT_DIR) 
			return;

		if(resultCode == RESULT_OK) {
			this.files = data.getStringArrayListExtra(DirPickerActivity.returnParameter);
        	} else {
			//showError("Internal error");
		}
	
	}

	public void setState(int newState) {
		SharedPreferences.Editor ed = mSettings.edit();
		ed.putInt("state", newState);
		ed.commit();

		state = newState;
	}

	public void onClick(View v) {
		int id = v.getId();
		String importTag; 

                switch (id) {
                        case R.id.button:
				if (state == STATE_DONE) {
					setState(STATE_INIT);
					finish();
					return;
				}

				hideControls();

				doImport();
				break;	
			case R.id.emailbutton:
				
				doEmailReceipt();

				if (state == STATE_DONE) {
					setState(STATE_INIT);
					finish();
					return;
				}

				break;
			case R.id.changebutton:
				doChangeImportFolder();
				break;
		}
	}

	public boolean isOnline() {
		ConnectivityManager cm =
			(ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();

		return netInfo != null && netInfo.isConnectedOrConnecting();
	}

	private void lockOrientation() {
		Display display = getWindowManager().getDefaultDisplay();
		int rotation = display.getRotation();
		int height, width;

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR2) {
			height = display.getHeight();
			width = display.getWidth();
		} else {
			Point size = new Point();
			display.getSize(size);
			height = size.y;
			width = size.x;
		}

		switch (rotation) {
			case Surface.ROTATION_90:
				if (width > height)
					setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
				else
					setRequestedOrientation(9/* reversePortait */);
				break;
			case Surface.ROTATION_180:
				if (height > width)
					setRequestedOrientation(9/* reversePortait */);
				else
					setRequestedOrientation(8/* reverseLandscape */);
				break;          
			case Surface.ROTATION_270:
				if (width > height)
					setRequestedOrientation(8/* reverseLandscape */);
				else
					setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				break;
			default:
				if (height > width)
					setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				else
					setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		}
	}

	class ImportTask extends AsyncTask<String, Integer, String> {

		public void doCancel() {
			bank.cancel();
			cancel(true);
		}

		protected String doInBackground(String... params) {

			setState(STATE_IMPORT);

			bank.initReport();

			for (int i = 0; i < bank.getLoadedIncomeLength(); i++) {
				if (isCancelled()) 	
					return "CANCELLED";		

				publishProgress(i);
				bank.importLoadedItem(i);	
				
			}

			if (isCancelled()) 
				return "CANCELLED";

			setState(STATE_FIX);
			bank.loadFracked();

			for (int i = 0; i < bank.getFrackedCoinsLength(); i++) {
				if (isCancelled()) 		
					return "CANCELLED";

				publishProgress(i);
				bank.fixFracked(i);
			}

			return "OK";
		}

		protected void onPostExecute(String result) {
			mainText.setText(getImportResultString());
			mainText.setGravity(Gravity.LEFT);
			button.setVisibility(View.VISIBLE);
			emailButton.setVisibility(View.VISIBLE);
			dotsView.setVisibility(View.GONE);
			headerText.setVisibility(View.VISIBLE);
			setState(STATE_DONE);
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
		}
		protected void onPreExecute() {
			lockOrientation();
			ll.setVisibility(View.GONE);
			mainText.setText(getStatusString(0));
		}

		protected void onProgressUpdate(Integer... values) {
			if (state == STATE_FIX)
				mainText.setText(getFrackedStatusString(values[0]));
			else if (state == STATE_IMPORT)
				mainText.setText(getStatusString(values[0]));

			raidaStatus = 0;
			coinActive = 0;
			coinTotal = 0;
			setDots();
	        }

	}
}

