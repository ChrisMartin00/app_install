package com.christmartin.appinstall;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent sync = new Intent(context, SyncService.class);
        context.startService(sync);
    }
}
