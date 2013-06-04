package co.joyatwork.accelerometer.sandbox;

import android.app.PendingIntent;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import co.joyatwork.pedometer.android.LoggingPedometerService;

public class AppPedometerService extends LoggingPedometerService {
	
	protected void startForeground() {
		
		Log.d("AppPedometerService", "startForeground()");
		
		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
		.setSmallIcon(R.drawable.notification_icon)
		.setContentTitle("Step Counter")
		.setContentText("press to launch")
		.setOngoing(true)
		;
	
		Intent launcActivity = new Intent(this, Main.class);
		
		TaskStackBuilder backStackBuilder = TaskStackBuilder.create(this);
		backStackBuilder.addParentStack(Main.class);
		backStackBuilder.addNextIntent(launcActivity);
		PendingIntent launchPendingActivity = backStackBuilder.getPendingIntent(0,
				PendingIntent.FLAG_UPDATE_CURRENT);
		
		notificationBuilder.setContentIntent(launchPendingActivity);
		
		startForeground(1, notificationBuilder.build());

	}

}
