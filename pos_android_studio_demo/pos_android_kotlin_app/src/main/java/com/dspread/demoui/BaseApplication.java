package com.dspread.demoui;

import android.app.Application;
import android.content.Context;

import xcrash.XCrash;

public class BaseApplication extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        //  默认初始化
        XCrash.init(this);
    }

}
