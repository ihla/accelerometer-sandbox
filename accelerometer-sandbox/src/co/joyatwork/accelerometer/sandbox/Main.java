package co.joyatwork.accelerometer.sandbox;

import co.joyatwork.pedometer.android.PedometerService;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
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
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.d(TAG, "onCreate()");
		
		setContentView(R.layout.main);
		
		Button quitButton = (Button) findViewById(R.id.quitButton);
		quitButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				cancelAllNotifications();
				// stop service implicitly
				Intent intent = new Intent(Main.this, PedometerService.class);
				stopService(intent);
				setResult(RESULT_OK);
				finish();
				
			}
		});
		
		CheckBox loggingCheckBox = (CheckBox) findViewById(R.id.loggingCheckBox);
		loggingCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				// TODO Auto-generated method stub
				
			}
		});
		
		stepCountUpdateReceiver = new StepCountUpdateReciever();
		
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
		Intent intent = new Intent(Main.this, PedometerService.class);
		startService(intent);
		showNotification();

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

	private void showNotification() {

		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
		.setSmallIcon(R.drawable.notification_icon)
		.setContentTitle("Step Counter")
		.setContentText("press to launch");
	
		Intent launcActivity = new Intent(this, Main.class);
		
		TaskStackBuilder backStackBuilder = TaskStackBuilder.create(this);
		backStackBuilder.addParentStack(Main.class);
		backStackBuilder.addNextIntent(launcActivity);
		PendingIntent launchPendingActivity = backStackBuilder.getPendingIntent(0,
				PendingIntent.FLAG_UPDATE_CURRENT);
		
		notificationBuilder.setContentIntent(launchPendingActivity);
		
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(0, notificationBuilder.build());
		
	}

	private void cancelAllNotifications() {
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancelAll();
	}
	

}
