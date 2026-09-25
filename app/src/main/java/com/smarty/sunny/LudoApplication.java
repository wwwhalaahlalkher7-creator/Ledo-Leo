package com.smarty.sunny;

import android.app.Application;

public class LudoApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize TelegramDiceController once at the Application process level
        TelegramDiceController.getInstance().init(this);
    }
}
