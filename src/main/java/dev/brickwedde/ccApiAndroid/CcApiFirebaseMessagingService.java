package dev.brickwedde.ccApiAndroid;

import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class CcApiFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Intent intent=new Intent();
        intent.setAction("dev.brickwedde.ccApiAndroid.newMessage");
        intent.putExtra("message",remoteMessage);
        sendBroadcast(intent);
    }

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        Intent intent=new Intent();
        intent.setAction("dev.brickwedde.ccApiAndroid.newToken");
        intent.putExtra("token",s);
        sendBroadcast(intent);
    }
}
