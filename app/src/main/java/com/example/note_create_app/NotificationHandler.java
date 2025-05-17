package com.example.note_create_app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class NotificationHandler {
    private static final String CHANNEL_ID= "note_box_notification_channel";
    private final int NOTIFICATION_ID=0;
    private NotificationManager mManager;
    private Context mcontext;
    public NotificationHandler(Context context) {
        this.mcontext=context;
        this.mManager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

        createChannel();
    }

    private void createChannel(){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel=new NotificationChannel(
                CHANNEL_ID, "NoteBox Notification", NotificationManager.IMPORTANCE_DEFAULT);

        channel.enableVibration(true);
        channel.enableLights(true);
        channel.setDescription("NoteBox Notification");
        this.mManager.createNotificationChannel(channel);
    }

    public void send(String message){
        Intent intent = new Intent(mcontext, Note_Create_MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(mcontext, 0, intent, PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);


        NotificationCompat.Builder builder=new NotificationCompat.Builder(mcontext, CHANNEL_ID)
                .setContentTitle("NoteBox")
                .setContentText(message)
                .setSmallIcon(R.drawable.note_add)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message));

        this.mManager.notify(NOTIFICATION_ID, builder.build());
    }

//AUTO CANCEL VAN (47.SOR)
//    public void cancel(){
//        this.mManager.cancel(NOTIFICATION_ID);
//    }
}
