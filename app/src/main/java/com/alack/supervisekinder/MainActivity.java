package com.alack.supervisekinder;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.KeyEvent;

import com.alack.supervisekinder.utils.Const;
import com.alack.supervisekinder.utils.LogManager;

public class MainActivity extends Activity {

    private static final String TAG     = "MainActivity";
    private LogManager          Log     = LogManager.getInstance();

    //private MediaProjectionManager  mProjectionManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i(TAG, "onCreate() - this :) " + this.toString());

        KinderApplication.getInstance().init(this);

        // create recorder
        KinderApplication.getInstance().permissionStorageAudio();
        MediaProjectionManager mProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        if(mProjectionManager == null) {
            Log.e(TAG, "mProjectionManager is NULL, why???");
        } else
        startActivityForResult(mProjectionManager.createScreenCaptureIntent(), Const.getRequestCodeScreenRecord());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, "onActivityResult() -(" + requestCode + ":" + resultCode + ":" + data + ")");

        if(resultCode == RESULT_CANCELED && requestCode == Const.getRequestCodeScreenRecord()) {
            Log.e(TAG, "Screen Recording Permission Denied : " + getIntent().getAction());
            if(getIntent().getAction().equals(Intent.ACTION_MAIN))
                this.finish();
            return;
        }

        if(resultCode == RESULT_OK && requestCode == Const.getRequestCodeScreenRecord()) {

            KinderApplication.getInstance().startRecService(data, resultCode);
            KinderApplication.getInstance().startMonitorService(null, null);
            //finish();
            moveTaskToBack(false);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.i(TAG, "onRequestPermissionsResult() - code:permissions:grant " +
                requestCode);// + " : " + permissions + " : " + grantResults);
        if(requestCode == Const.getRequestCodeAudio() || requestCode == Const.getRequestCodeStorage()) {
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "onRequestPermissionsResult() - not PERMISSION_GRANTED");
                finish();
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.i(TAG, "onKeyDown() - Key " + keyCode + ", Event " + event);
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            //Toast.makeText(getApplicationContext(), "KEYCODE_BACK", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onKeyDown() - KEYCODE_BACK down");
            // 1. background : moveTaskToBack(false);
            // 2.
            //moveTaskToBack(false);
            //return true;
        }
        if(keyCode == KeyEvent.KEYCODE_HOME) {
            //Toast.makeText(getApplicationContext(), "KEYCODE_HOME", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onKeyDown() - KEYCODE_HOME down");
        }
        return super.onKeyDown(keyCode, event);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy() - ");
        KinderApplication.getInstance().uninit();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause() - ");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume() - ");
    }
}
