package com.alack.supervisekinder.daemon;

import android.app.Service;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.alack.supervisekinder.KinderApplication;
import com.alack.supervisekinder.utils.Const;
import com.alack.supervisekinder.utils.LogManager;


/**
 * Created by taurus on 2017/3/12.
 */

public class KeepliveService extends Service {

    private static final String     TAG         = "KeepliveService";
    private LogManager              Log         = LogManager.getInstance();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand() - action : " + intent.getAction());
        int notificationID = -1;
        String action = intent.getAction();

        // foreground notification
        if(action.equals(Const.getNotificationHideMonitor()))
            notificationID = Const.getScrennRecordNotificationMonitor();
        if(action.equals(Const.getNotificationHideRecord()))
            notificationID = Const.getScrennRecordNotificationRecord();

        if(notificationID != -1) {
            KinderApplication.getInstance().startForeground(notificationID, action, this);
        }

        // system broadcast
        switch (action) {
            case Intent.ACTION_USER_PRESENT:  // unlock screen
                //Log.i(TAG, "onStartCommand() - present go on keepliveactivity  ");
                KinderApplication.getInstance().stopKeeplive();
                break;
            case Intent.ACTION_SCREEN_OFF:  // lock screen
                KinderApplication.getInstance().startKeeplive();
                break;
            case Intent.ACTION_TIME_TICK:  // start MainActivity for RecordMedia
                KinderApplication.getInstance().checkApp();
                break;
            case Intent.ACTION_MAIN:  // start MainActivity for RecordMedia
                KinderApplication.getInstance().startMain();
                break;
            case Intent.ACTION_BOOT_COMPLETED: // boot up
                KinderApplication.getInstance().startMain();
                break;
            case ConnectivityManager.CONNECTIVITY_ACTION: // wifi change
                KinderApplication.getInstance().checkWifi();
                break;
            default:
                KinderApplication.getInstance().checkApp();
                break;
        }
        return Service.START_STICKY;
    }


}
