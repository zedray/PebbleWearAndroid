package com.richardgg.pebblewear;

import android.content.Context;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Created by mbrady on 9/19/14.
 */
public class MessageInterface {

    private boolean mReadyForSend;
    private List<Message> mMessageQueue;
    private int mTransactionId;
    private UUID mPebbleAppUuid;

    public MessageInterface(UUID pebbleAppUuid) {
        mPebbleAppUuid = pebbleAppUuid;
        mReadyForSend = true;
        mTransactionId = 0;
        mMessageQueue = new ArrayList<Message>();
    }

    public synchronized void send(Context context, PebbleDictionary pebbleDictionary) {
        if (pebbleDictionary != null) {
            mTransactionId += 1;
            //Log.v(WearService.TAG, "MessageInterface.send() New message: " + mTransactionId);
            mMessageQueue.add(new Message(mTransactionId, pebbleDictionary));
        }
        if (mReadyForSend) {
            if (mMessageQueue.size() > 0) {
                Message message = mMessageQueue.get(0);
                //Log.v(WearService.TAG, "MessageInterface.send() Sending message: "+ message.getTransactionId());
                PebbleKit.sendDataToPebbleWithTransactionId(context, mPebbleAppUuid,
                        message.getMessage(), message.getTransactionId());
            }
            mReadyForSend = false;
        }
    }

    public synchronized void success(int transactionId) {
        for (Iterator<Message> iterator = mMessageQueue.listIterator(); iterator.hasNext(); ) {
            Message message = iterator.next();
            if (message.getTransactionId() == transactionId) {
                //Log.v(WearService.TAG, "MessageInterface.success() Removing from queue: " + message.getTransactionId());
                iterator.remove();
                return;
            }
        }
        mReadyForSend = true;
    }

    public void fail(int transactionId) {
        // TODO: Do something smart with the NACK'ed responses.
        success(transactionId);
    }

    private class Message {
        int mTransactionId;
        PebbleDictionary mMessage;

        private Message(int transactionId, PebbleDictionary message) {
            mTransactionId = transactionId;
            mMessage = message;
        }

        public int getTransactionId() {
            return mTransactionId;
        }

        public PebbleDictionary getMessage() {
            return mMessage;
        }
    }
}
