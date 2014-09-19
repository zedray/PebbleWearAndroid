package com.richardgg.pebblewear;

import android.content.Context;
import android.util.Log;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by mbrady on 9/19/14.
 */
public class MessageInterface {

    private boolean mReadyForSend;
    private ArrayList<PebbleDictionary> mMessageQueue;
    private UUID mPebbleAppUuid;

    public MessageInterface(UUID pebbleAppUuid) {
        mPebbleAppUuid = pebbleAppUuid;
        mReadyForSend = true;
        mMessageQueue = new ArrayList<PebbleDictionary>();
    }

    public synchronized void send(Context context, PebbleDictionary message) {
        if (message != null) {
            mMessageQueue.add(message);
        }
        if (mReadyForSend && mMessageQueue.size() > 0) {
            PebbleKit.sendDataToPebble(context, mPebbleAppUuid, mMessageQueue.get(0));
            mReadyForSend = false;
        }
        if (mMessageQueue.isEmpty()) {
            Log.d(WearService.TAG, "MessageInterface.send() Done");
        }
    }

    public synchronized void success() {
        if (!mMessageQueue.isEmpty()) {
            mMessageQueue.remove(0);
        }
    }

    public synchronized void setReady() {
        mReadyForSend = true;
    }

    public synchronized void cancel() {
        mMessageQueue.clear();
    }
}