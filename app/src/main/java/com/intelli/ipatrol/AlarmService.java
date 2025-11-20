package com.intelli.ipatrol;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class AlarmService extends Service {

    private static final String CHANNEL_ID = "alarm_channel";
    private MediaPlayer player;
    public static boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String file = intent.getStringExtra("sound_file");
        if (file == null) file = "alarm.mp3";

        if (player == null) {
            int resId = getResources().getIdentifier(
                    file.replace(".mp3", ""),
                    "raw",
                    getPackageName()
            );
            player = MediaPlayer.create(this, resId);
            player.setLooping(true);
        }

        if (!player.isPlaying()) {
            player.start();
        }

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Alarm Active")
                .setContentText("Alarm is playing in background")
                .setSmallIcon(R.drawable.stop)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();

        startForeground(1, notification);

        isRunning = true;

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (player != null) {
            if (player.isPlaying()) player.stop();
            player.release();
        }
        isRunning = false;
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Alarm Background Channel",
                    NotificationManager.IMPORTANCE_HIGH
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
}
