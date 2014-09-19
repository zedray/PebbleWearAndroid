package com.richardgg.pebblewear;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
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

    // Keys.
    private final static int LIST_REQUEST = 0;
    private final static int REMOVE_NOTIFICATION = 1;
    private final static int SEND_ACTIONS = 2;

    private StatusBarNotification[] mWatchNotifications;
    private MessageInterface mMessageInterface;
    private NotificationInterface mNotificationInterface;

    @Override
    public void onCreate() {
        Log.i(TAG, "WearService.onCreate() Pebble is " + ((PebbleKit.isWatchConnected(getApplicationContext())) ? "connected" : "not connected"));
        if (!PebbleKit.areAppMessagesSupported(getApplicationContext())) {
            Log.i(TAG, "App Message is not supported!");
            stopSelf();
            return;
        }

        mWatchNotifications = new StatusBarNotification[5];
        mMessageInterface = new MessageInterface(PEBBLE_APP_UUID);
        mNotificationInterface = new NotificationInterface(mMessageInterface);
        final NotificationListenerService service = this;

        PebbleKit.registerPebbleConnectedReceiver(getApplicationContext(), new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.w(TAG, "PebbleConnectedReceiver.onReceive() Pebble connected!");
            }
        });
        PebbleKit.registerPebbleDisconnectedReceiver(getApplicationContext(), new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.w(TAG, "PebbleConnectedReceiver.onReceive() Pebble disconnected!");
            }
        });

        PebbleKit.registerReceivedDataHandler(this, new PebbleKit.PebbleDataReceiver(PEBBLE_APP_UUID) {
            @Override
            public void receiveData(final Context context, final int transactionId, final PebbleDictionary data) {

                // Note: Don't do any UI work inside the Handler!
                StatusBarNotification[] activeNotifications = getActiveNotifications();
                switch (data.getInteger(1).intValue()) {
                    case LIST_REQUEST:
                        Log.d(TAG, "PebbleDataReceiver.receiveData() LIST_REQUEST");
                        mNotificationInterface.listRequest(context, activeNotifications);
                        break;

                    case REMOVE_NOTIFICATION:
                        Log.d(TAG, "PebbleDataReceiver.receiveData() REMOVE_NOTIFICATION");
                        NotificationInterface.removeNotification(service, activeNotifications, data.getInteger(2).intValue());
                        break;

                    case SEND_ACTIONS:
                        Log.d(TAG, "PebbleDataReceiver.receiveData() SEND_ACTIONS");
                        mNotificationInterface.sendActions(context, activeNotifications, data.getInteger(2).intValue());
                        break;

                    default:
                        Log.w(TAG, "PebbleDataReceiver.receiveData() Unknown data " + data.getInteger(1));
                }
                PebbleKit.sendAckToPebble(context, transactionId);
            }
        });

        PebbleKit.registerReceivedNackHandler(this, new PebbleKit.PebbleNackReceiver(PEBBLE_APP_UUID) {
            @Override
            public void receiveNack(Context context, int transactionId) {
                Log.d(TAG, "PebbleNackReceiver.receiveData()");
                mMessageInterface.fail(transactionId);
                mMessageInterface.send(context, null);
            }
        });

        PebbleKit.registerReceivedAckHandler(this, new PebbleKit.PebbleAckReceiver(PEBBLE_APP_UUID) {
            @Override
            public void receiveAck(Context context, int transactionId) {
                Log.d(TAG, "PebbleAckReceiver.receiveData() transactionId:" + transactionId);
                mMessageInterface.success(transactionId);
                mMessageInterface.send(context, null);
            }
        });

        PebbleKit.startAppOnPebble(getApplicationContext(), PEBBLE_APP_UUID);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "WearService.onDestroy()");
    }

    @Override
    public void onNotificationPosted(StatusBarNotification notification) {
        Log.i(TAG, "WearService.onNotificationPosted() "
                + Utils.getTitleContent(notification.getNotification().extras) + " - "
                + Utils.getTextContent(notification.getNotification().extras));

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
    public void onNotificationRemoved(StatusBarNotification notification) {
        Log.i("Wear", "WearService.onNotificationRemoved() "
                + Utils.getTitleContent(notification.getNotification().extras) + " - "
                + Utils.getTextContent(notification.getNotification().extras));
    }
}
