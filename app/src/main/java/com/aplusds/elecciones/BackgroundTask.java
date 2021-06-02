package com.aplusds.elecciones;

import android.app.Activity;
import android.util.Log;

public abstract class BackgroundTask {
    private Activity act;
    public BackgroundTask(Activity act) {
        this.act = act;
    }

    private void startBackground() {
        new Thread(() -> {
            doInBackground();
            act.runOnUiThread(() -> onPostExecute());
        }).start();
    }
    public void execute() {
        startBackground();
    }
    public abstract void doInBackground();
    public abstract void onPostExecute();
}
