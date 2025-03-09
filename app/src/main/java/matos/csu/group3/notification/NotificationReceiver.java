package matos.csu.group3.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Trigger the notification
        NotificationHelper.checkPhotosAndNotify(context);
    }
}
