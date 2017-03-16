package com.alack.supervisekinder.daemon;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.alack.supervisekinder.utils.LogManager;

/**
 * Created by taurus on 2017/3/12.
 */

public class KeepliveReceiver extends BroadcastReceiver {
    private static final String     TAG         = "KeepliveReceiver";
    private LogManager              Log         = LogManager.getInstance();


    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.e(TAG, "Receive : - " + action);
        if(action.equals(Intent.ACTION_BOOT_COMPLETED)
                || action.equals(Intent.ACTION_SCREEN_OFF)
                || action.equals(Intent.ACTION_USER_PRESENT)) {
            Intent bootIntent = new Intent(context, KeepliveService.class);
            bootIntent.setAction(action);
            context.startService(bootIntent);
        } else {
            Intent bootIntent = new Intent(context, KeepliveService.class);
            bootIntent.setAction(action);
            context.startService(bootIntent);
        }
    }
}
