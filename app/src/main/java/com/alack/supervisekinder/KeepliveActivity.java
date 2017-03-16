package com.alack.supervisekinder;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import com.alack.supervisekinder.utils.LogManager;

/**
 * Created by Administrator on 2017/3/13.
 */

public class KeepliveActivity extends Activity {
    private static final String TAG = "KeepliveActivity";
    private LogManager          Log = LogManager.getInstance();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate() - ");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_keeplive);

        Window win = getWindow();
        win.setGravity(Gravity.START | Gravity.TOP);
        WindowManager.LayoutParams params = win.getAttributes();
        params.x = 0;
        params.y = 0;
        params.height = 300;
        params.width = 300;
        //params.setTitle("AKinder...");
        win.setAttributes(params);

        KinderApplication.getInstance().addActivity(this);
        Log.i(TAG, "onCreate() - end");
    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart() - ");
        //finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy() - ");
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
