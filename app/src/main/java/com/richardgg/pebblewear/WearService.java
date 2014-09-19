package com.richardgg.pebblewear;

import android.content.Context;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.util.UUID;

/**
 * Based on a project created by RichardGG on 1/09/2014.
 */
public class WearService extends NotificationListenerService {

    public final static String TAG = "WEAR";
    public final static UUID PEBBLE_APP_UUID = UUID.fromString("69fbcd85-91ae-4fbf-8d71-a6321dca8b28");

    private StatusBarNotification[] mWatchNotifications;
    private MessageInterface mMessageInterface;
    private NotificationInterface mNotificationInterface;

    @Override
    public void onCreate() {
        Log.d(TAG, "WearService.onCreate()");
        mWatchNotifications = new StatusBarNotification[5];
        mMessageInterface = new MessageInterface(PEBBLE_APP_UUID);
        mNotificationInterface = new NotificationInterface(mMessageInterface);
        final NotificationListenerService service = this;

        PebbleKit.registerReceivedDataHandler(this, new PebbleKit.PebbleDataReceiver(PEBBLE_APP_UUID) {
            @Override
            public void receiveData(final Context context, final int transactionId, final PebbleDictionary data) {
                Log.d(TAG, "PebbleDataReceiver.receiveData() " + data.getInteger(1));
                StatusBarNotification[] activeNotifications = getActiveNotifications();
                if (data.getInteger(1) == 0)
                    mNotificationInterface.listRequest(context, activeNotifications);
                if (data.getInteger(1) == 1)
                    NotificationInterface.removeNotification(service, activeNotifications, data.getInteger(2).intValue());
                if (data.getInteger(1) == 2)
                    mNotificationInterface.sendActions(context, activeNotifications, data.getInteger(2).intValue());
            }
        });

        PebbleKit.registerReceivedNackHandler(this, new PebbleKit.PebbleNackReceiver(PEBBLE_APP_UUID) {
            @Override
            public void receiveNack(final Context context, final int transactionId){
                Log.d(TAG, "PebbleNackReceiver.receiveData()");
                mMessageInterface.cancel();
                mMessageInterface.setReady();
                mMessageInterface.send(context, null);
            }
        });

        PebbleKit.registerReceivedAckHandler(this, new PebbleKit.PebbleAckReceiver(PEBBLE_APP_UUID) {
            @Override
            public void receiveAck(final Context context, final int transactionId) {
                Log.d(TAG, "PebbleAckReceiver.receiveData()");
                mMessageInterface.success();
                mMessageInterface.setReady();
                mMessageInterface.send(context, null);
            }
        });
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "WearService.onDestroy()");
    }

    @Override
    public void onNotificationPosted(StatusBarNotification notification) {
        Log.d(TAG, "WearService.onNotificationPosted()");

        int position = 6;
        StatusBarNotification[] topNotifications = new StatusBarNotification[5];
        for(StatusBarNotification activeNotification : getActiveNotifications()) {
            boolean found = false;
            for (int i = 0; i < 5; i++) {
                if (!found) {
                    if (topNotifications[i] != null) {
                        if (topNotifications[i].getNotification().priority <= activeNotification.getNotification().priority) {
                            System.arraycopy(topNotifications, i, topNotifications, i + 1, 4 - i);
                            topNotifications[i] = activeNotification;
                            found = true;
                        }
                    } else {
                        topNotifications[i] = activeNotification;
                        found = true;
                    }
                }
            }
        }

        for (int i = 0; i < 5; i++) {
            if (notification.getId() == topNotifications[i].getId()) {
                position = i;
            }
        }

        if (position < 5) {
            // Check if updated notification.
            int updatedNotification = -1;
            for (int i = 0; i < mWatchNotifications.length; i++) {
                if (mWatchNotifications[i] != null && notification.getId() == mWatchNotifications[i].getId()) {
                    updatedNotification = i;
                }
            }

            if (updatedNotification >= 0) {
                // Update the notification.
                mNotificationInterface.updateNotification(mWatchNotifications, getApplicationContext(), notification, updatedNotification);
            } else {
                // Otherwise send new notification.
                mNotificationInterface.newNotification(getApplicationContext(), notification, position);
            }
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.d("Wear", "WearService.onNotificationRemoved()");
    }
}
