package matos.csu.group3.notification;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Trigger the notification
        NotificationHelper.checkPhotosAndNotify(context);

        // Get the AlarmManager instance
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Ensure alarmManager is not null before proceeding
        if (alarmManager != null) {
            // Only schedule the notification if permission is granted for Android 12+
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
                NotificationHelper.scheduleDailyNotification(context);
            }
        }
    }
}
