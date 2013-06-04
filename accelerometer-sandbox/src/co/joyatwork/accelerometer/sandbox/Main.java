package co.joyatwork.accelerometer.sandbox;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

public class Main extends Activity {

	private static final String TAG = "Main";

	private final class StepCountUpdateReciever extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			
			Log.d(TAG, "StepCountUpdateReciever.onReceive " + intent.getAction());

			if (intent.hasExtra(getResources().getString(R.string.step_count))) {
				TextView xValueTextView = (TextView) findViewById(R.id.stepsCountTextView);
				xValueTextView.setText(
						intent.getExtras().getCharSequence(getResources().getString(R.string.step_count)));
			}

			if (intent.hasExtra(getResources().getString(R.string.step_axis))) {
				TextView xValueTextView = (TextView) findViewById(R.id.activeAxisValueTextView);
				xValueTextView.setText(
						intent.getExtras().getCharSequence(getResources().getString(R.string.step_axis)));
			}

		}
		
	}
	private BroadcastReceiver stepCountUpdateReceiver;
	private SharedPreferences settings;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.d(TAG, "onCreate()");
		
		setContentView(R.layout.main);
		
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		Button quitButton = (Button) findViewById(R.id.quitButton);
		quitButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// stop service implicitly
				Intent intent = new Intent(Main.this, AppPedometerService.class);
				stopService(intent);
				storeLoggingPreferencesAndCommit(false); //disable logging on quit
				setResult(RESULT_OK);
				finish();
				
			}
		});
		
		CheckBox loggingCheckBox = (CheckBox) findViewById(R.id.loggingCheckBox);
		loggingCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				storeLoggingPreferencesAndCommit(isChecked);
			}

		});
		loggingCheckBox.setChecked(settings.getBoolean("logging", false));
		
		stepCountUpdateReceiver = new StepCountUpdateReciever();
		
	}

	private void storeLoggingPreferencesAndCommit(boolean value) {
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean("logging", value);
		editor.commit();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		
		Log.d(TAG, "onDestroy()");

	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	protected void onResume() {
		super.onResume();

		Log.d(TAG, "onResume()");

		LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
		lbm.registerReceiver(stepCountUpdateReceiver, 
				new IntentFilter(getResources().getString(R.string.step_count_update_action)));

		// start service explicitly
		Intent intent = new Intent(Main.this, AppPedometerService.class);
		startService(intent);

	}

	@Override
	protected void onPause() {
		super.onPause();

		Log.d(TAG, "onPause()");

		LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
		lbm.unregisterReceiver(stepCountUpdateReceiver);

	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);
	}
	

}
