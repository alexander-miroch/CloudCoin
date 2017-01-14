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


public class AddCoinsActivity extends Activity implements OnClickListener {


	static String TAG = "CLOUDCOIN";

	static int STATE_INIT = 1;
	static int STATE_DONE = 2;

	int state;

	Typeface tf;
	TextView tvt, mainText;
	Button button;
	//EditText et;

	Bank bank;

	int count = 0;


	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.addcoins);

		tf = Typeface.createFromAsset(getAssets(), "fonts/font.ttf");

		//et = (EditText) findViewById(R.id.importtag);
		tvt = (TextView) findViewById(R.id.title);

		mainText = (TextView) findViewById(R.id.text);

		button = (Button) findViewById(R.id.button);
		button.setOnClickListener(this);

		
	}

	public void onPause() {
		super.onPause();
	}

	public void onResume() {
		super.onResume();
		int totalIncomeLength;

		state = STATE_INIT;

		hideControls();

		bank = new Bank();

		String importDir = bank.getRelativeImportDirPath();
		if (importDir == null) {
			mainText.setText("Can not locate your Import directory. Make sure that SD card is installed into the device.");
			return;
		}
		
		if (!bank.examineImportDir()) {
			mainText.setText("Error occured while doing search in your Import directory");
			return;
		}

		totalIncomeLength = bank.getLoadedIncomeLength();
		if (totalIncomeLength == 0) {
			mainText.setText("There were no CloudCoins found in your income folder. Please put your CloudCoins (.jpg or .stack) files in your income folder and try again. Your Income folder can be found on your primary storage at:\n\n" + importDir); 
			return;
		}

		mainText.setText("Import will import all CloudCoin files from your Import folder. Please put all your CloudCoin files (.jpg and .stack) into the Import folder located on your primary storage at:\n\n" + importDir + "\n\nWe are going to import " + Integer.toString(totalIncomeLength) + " files. Tap OK to continue");

		showControls();

	}

	private String getImportResultString() {
		String resultString;

		StringBuilder sb = new StringBuilder();
		
		sb.append("Total Moved to Bank: ");
		sb.append(bank.getImportStats(Bank.STAT_VALUE_MOVED_TO_BANK));
		sb.append(" coins\n");

		sb.append("\nResults of import:\n\n");		
		sb.append("Authentic and moved to bank: ");
		sb.append(bank.getImportStats(Bank.STAT_AUTHENTIC));
		
		sb.append("\nCounterfeit or failed and moved to Trash: ");
		//sb.append(bank.getImportStats(Bank.STAT_COUNTERFEIT));
		sb.append(bank.getImportStats(Bank.STAT_FAILED));

		if (bank.getImportStats(Bank.STAT_FAILED) != 0)
			sb.append("\n\nPlease check your CloudCoin Trash\nof a note about what happened");

		//sb.append("\nFractured and will be repaired: ");
		//sb.append(bank.getImportStats(Bank.STAT_FRACTURED));

		//sb.append("\nFailed and left untouched: ");
		//sb.append(bank.getImportStats(Bank.STAT_FAILED));
		
	
		return sb.toString();
	
	}
	private String getStatusString(int progressCoins) {
		String statusString;

		String totalIncomeLength = Integer.toString(bank.getLoadedIncomeLength());
		String importedIncomeLength = Integer.toString(progressCoins + 1);
	
		statusString = "Authenticating " + importedIncomeLength + " of " + totalIncomeLength + " Coins\n";
		
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
			for (int i = 0; i < bank.getLoadedIncomeLength(); i++) {
				bank.importLoadedItem(i);	
				publishProgress(i);
			}

			bank.fixFracked();
			return "OK";
		}


		protected void onPostExecute(String result) {
			mainText.setText(getImportResultString());
			mainText.setGravity(Gravity.LEFT);
			button.setVisibility(View.VISIBLE);
			state = STATE_DONE;
		}
		protected void onPreExecute() {
			mainText.setText(getStatusString(0));
		}
		protected void onProgressUpdate(Integer... values) {
			mainText.setText(getStatusString(values[0]));
	        }

	}
}

