package matos.csu.group3.notification;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.Settings;

import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import matos.csu.group3.R;
import matos.csu.group3.ui.main.MainActivity;

public class NotificationHelper {
    static int hour = 22, minute = 10, second = 0;

    private static final String CHANNEL_ID = "photo_reminder_channel";
    private static final int NOTIFICATION_ID = 1001;

    public static void checkPhotosAndNotify(Context context) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = sdf.format(new Date());

        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Images.Media.DATE_TAKEN, MediaStore.Images.Media.DATA};
        String selection = MediaStore.Images.Media.DATE_TAKEN + " IS NOT NULL";
        String sortOrder = MediaStore.Images.Media.DATE_TAKEN + " DESC";

        try (Cursor cursor = context.getContentResolver().query(uri, projection, selection, null, sortOrder)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    long dateTakenMillis = cursor.getLong(0);
                    String photoDate = sdf.format(new Date(dateTakenMillis));
                    String photoPath = cursor.getString(1);

                    if (photoDate.substring(5).equals(todayDate.substring(5))) {
                        int yearsAgo = calculateYearsAgo(dateTakenMillis);
                        if (yearsAgo > 0) {
                            sendNotification(context, yearsAgo, photoPath, dateTakenMillis);
                            return;
                        }
                    }
                }
            }
        }
    }

    private static int calculateYearsAgo(long dateTakenMillis) {
        Calendar photoDate = Calendar.getInstance();
        photoDate.setTimeInMillis(dateTakenMillis);

        Calendar today = Calendar.getInstance();
        int yearsAgo = today.get(Calendar.YEAR) - photoDate.get(Calendar.YEAR);

        if (today.get(Calendar.MONTH) < photoDate.get(Calendar.MONTH) ||
                (today.get(Calendar.MONTH) == photoDate.get(Calendar.MONTH) &&
                        today.get(Calendar.DAY_OF_MONTH) < photoDate.get(Calendar.DAY_OF_MONTH))) {
            yearsAgo--;
        }
        return yearsAgo;
    }

    private static void sendNotification(Context context, int yearsAgo, String photoPath, long dateTakenMillis) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("photo_path", photoPath);
        intent.putExtra("date_taken", dateTakenMillis);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String notificationText = (yearsAgo == 1) ? "You took a photo on this date 1 year ago!" :
                "You took a photo on this date " + yearsAgo + " years ago!";

        Bitmap photoBitmap = BitmapFactory.decodeFile(photoPath);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_photo)
                .setContentTitle("Photo Reminder")
                .setContentText(notificationText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        if (photoBitmap != null) {
            builder.setLargeIcon(photoBitmap)
                    .setStyle(new NotificationCompat.BigPictureStyle()
                            .bigPicture(photoBitmap)
                            .bigLargeIcon((Bitmap) null));
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Photo Reminder", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Reminds you of photos taken on the same date in previous years");
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    public static void scheduleDailyNotification(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Cancel any existing alarm to prevent duplicates
        alarmManager.cancel(pendingIntent);

        // Set the time to 10:30 AM GMT+7
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("GMT+7"));
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, second);

        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {  // Android 12+
            if (alarmManager.canScheduleExactAlarms()) {
                try {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent
                    );
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            } else {
                // Request user to allow exact alarm permission
                Intent intentPermission = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                intentPermission.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intentPermission);
            }
        } else {
            // For Android versions below 12, schedule normally
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
            );
        }
    }

}
