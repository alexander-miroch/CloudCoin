package co.cloudcoin.cc;



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


import android.widget.FrameLayout;
import android.widget.LinearLayout;

import android.view.Gravity;
import android.widget.Toast;
import android.widget.EditText;

import android.util.TypedValue;
import android.content.res.Resources;

public class BankActivity extends Activity  {


	static String TAG = "CLOUDCOIN";

	Bank bank;

	static int IDX_BANK = 0;
	static int IDX_COUNTERFEIT = 1;
	static int IDX_FRACTURED = 2;


	TextView[][] ids;
	int[][] stats;
	int size;


	private void allocId(int idx, String prefix) {
		int resId, i;
		String idTxt;

		stats[idx] = new int[size];
		ids[idx] = new TextView[size];
		for (i = 0; i < size; i++) {
			if (i == size - 1)
				idTxt = prefix + "all";
			else
				idTxt = prefix + Bank.denominations[i];

			resId = getResources().getIdentifier(idTxt, "id", getPackageName());

			ids[idx][i] = (TextView) findViewById(resId);
		}

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		int i;
		super.onCreate(savedInstanceState);

		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.bank);



		size = Bank.denominations.length + 1;
		ids = new TextView[3][];
		stats = new int[3][];
		
		allocId(IDX_BANK, "bs");
		allocId(IDX_COUNTERFEIT, "cs");
		allocId(IDX_FRACTURED, "fs");
	}


	public void onPause() {
		super.onPause();
	}

	public void onResume() {
		CloudCoin[] bankedCoins, frackedCoins, counterfeitCoins;

		super.onResume();

		bank = new Bank(this);

		stats[IDX_BANK] = bank.countCoins("bank");
		//stats[IDX_COUNTERFEIT] = bank.countCoins("counterfeit");
		stats[IDX_FRACTURED] = bank.countCoins("fracked");

		int j;
		for (int i = 0; i < size; i++) {
			if (i == 0)
				j = size - 1;
			else 
				j = i - 1;

			int authCount = stats[IDX_BANK][i] + stats[IDX_FRACTURED][i];

			ids[IDX_BANK][j].setText("" + authCount);
			//ids[IDX_BANK][j].setText("" + stats[IDX_BANK][i]);
			//ids[IDX_COUNTERFEIT][j].setText("" + stats[IDX_COUNTERFEIT][i]);
			//ids[IDX_FRACTURED][j].setText("" + stats[IDX_FRACTURED][i]);
		}

	}


}

