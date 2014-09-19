package com.richardgg.pebblewear;

import android.content.Context;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.getpebble.android.kit.util.PebbleDictionary;

public class DeleteMe {

    private void sendList(MessageInterface messageInterface, Context context, StatusBarNotification[] activeNotifications) {
        StatusBarNotification[] watchNotifications = new StatusBarNotification[5];
        for(StatusBarNotification sbnA : activeNotifications) {
            boolean found = false;
            for(int i = 0; i < 5; i++) {
                if (!found) {
                    if (watchNotifications[i] != null) {
                        if (watchNotifications[i].getNotification().priority <= sbnA.getNotification().priority){
                            for(int j = 4; j > i; j--){
                                watchNotifications[j] = watchNotifications[j-1];
                            }
                            watchNotifications[i] = sbnA;
                            found = true;
                        }
                    } else {
                        watchNotifications[i] = sbnA;
                        found = true;
                    }
                }
            }
        }

        PebbleDictionary message = new PebbleDictionary();
        for(int i = 0; i < 5; i++) {
            if(watchNotifications[i] != null) {
                //message.addString(i, getTitleContent(sWatchNotifications[i].getNotification().extras));
            } else {
                message.addString(i, " ");
            }
        }
        messageInterface.send(context, message);

        Log.d(WearService.TAG, "sent titles");

        message = new PebbleDictionary();
        for(int i = 0; i < 5; i++) {
            if(watchNotifications[i] != null) {
                //message.addString(5+i,getTextContent(sWatchNotifications[i].getNotification().extras) );
            }
            else {
                message.addString(5+i, " ");
            }
        }
        messageInterface.send(context, message);

        Log.d(WearService.TAG, "sent text");
    }
}
