package com.example.studymate.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

public class AirplaneModeReceiver extends BroadcastReceiver {

    public interface AirplaneModeListener {
        void onAirplaneModeChanged(boolean isAirplaneModeOn);
    }

    private final AirplaneModeListener listener;

    public AirplaneModeReceiver() {
        this.listener = null;
    }

    public AirplaneModeReceiver(AirplaneModeListener listener) {
        this.listener = listener;
    }

    public static boolean isAirplaneModeOn(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(intent.getAction())) {
            boolean isModeOn = isAirplaneModeOn(context);
            if (listener != null) {
                listener.onAirplaneModeChanged(isModeOn);
            }
        }
    }
}
