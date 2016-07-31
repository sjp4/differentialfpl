package com.pennas.fpl.process;

import com.pennas.fpl.App;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class DiffBroadcastReceiver extends BroadcastReceiver {
	public static final String ACTION_ALARM = "com.pennas.fpl.ALARM_PROD";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
			App.log("auto starting from broadcast receiver");
		} else if (intent.getAction().equals(ACTION_ALARM)) {
			App.log("alarm broadcast received");
			// unset alarm (from this app's point of view.. already fired in system point of view)
			UpdateManager.alarmSet = null;
			UpdateManager.alarmTime = 0;
			UpdateManager.nextEvent = 0;
			
			// run stuff
			if (FPLService.service != null) {
				FPLService.service.doServiceStuff();
			}
		}
	}
}