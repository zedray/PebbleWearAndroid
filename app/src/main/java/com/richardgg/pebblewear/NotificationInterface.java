package com.richardgg.pebblewear;

import android.app.Notification;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.DisplayMetrics;
import android.util.Log;

import com.getpebble.android.kit.util.PebbleDictionary;

/**
 * Created by mbrady on 9/19/14.
 */
public class NotificationInterface {

    private final static int MAX_NOTIFICATIONS = 5;

    // Keys.
    private final static int COMMAND = 0;
    private final static int BYTES = 1;
    private final static int LINE = 2;
    private final static int ID = 3;

    // Bytes???
    private final static byte CLEAR = 0;
    private final static byte UPDATE_TEXT = 1;
    private final static byte UPDATE_ICON = 2;
    private final static byte UPDATE_IMAGE = 3;
    private final static byte MOVE = 4;
    private final static byte VIEW = 5;
    private final static byte REPORT = 6;
    private final static byte ACTIONS = 7;

    private MessageInterface mMessageInterface;
    private StatusBarNotification[] mWatchNotifications;

    public NotificationInterface(MessageInterface messageInterface) {
        mMessageInterface = messageInterface;
        mWatchNotifications = new StatusBarNotification[5];
    }

    public void updateNotificationsList(Context context, StatusBarNotification notification,
                                        StatusBarNotification[] activeNotifications) {

        int position = MAX_NOTIFICATIONS + 1;
        StatusBarNotification[] topNotifications = new StatusBarNotification[MAX_NOTIFICATIONS];
        for (StatusBarNotification activeNotification : activeNotifications) {
            boolean found = false;
            // What are we trying to do here?
            for (int i = 0; i < MAX_NOTIFICATIONS; i++) {
                if (!found) {
                    if (topNotifications[i] != null) {
                        if (topNotifications[i].getNotification().priority <= activeNotification.getNotification().priority) {
                            System.arraycopy(topNotifications, i, topNotifications, i + 1, 4 - i); // What does 4 mean???
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

        for (int i = 0; i < MAX_NOTIFICATIONS; i++) {
            if (notification.getId() == topNotifications[i].getId()) {
                position = i;
            }
        }

        if (position < MAX_NOTIFICATIONS) {
            // Check if updated notification.
            int updatedNotification = -1;
            for (int i = 0; i < mWatchNotifications.length; i++) {
                if (mWatchNotifications[i] != null && notification.getId() == mWatchNotifications[i].getId()) {
                    updatedNotification = i;
                }
            }

            if (updatedNotification >= 0) {
                // Update the notification.
                updateNotification(mWatchNotifications, context, notification, updatedNotification);
            } else {
                // Otherwise send new notification.
                newNotification(context, notification, position);
            }
        }
    }

    public void listRequest(Context context, StatusBarNotification[] activeNotifications) {
        Log.i(PebbleWearService.TAG, "NotificationInterface.listRequest() " + activeNotifications.length);
        StatusBarNotification[] topNotifications = new StatusBarNotification[5];
        for (StatusBarNotification notification : activeNotifications) {
            boolean found = false;
            for(int i = 0; i < 5; i++) {
                if (!found) {
                    if (topNotifications[i] != null) {
                        if (topNotifications[i].getNotification().priority <= notification.getNotification().priority){
                            for(int j = 4; j > i; j--){
                                topNotifications[j] = topNotifications[j - 1];
                            }
                            topNotifications[i] = notification;
                            found = true;
                        }
                    } else {
                        topNotifications[i] = notification;
                        found = true;
                    }
                }
            }
        }

        for (int i = 0; i < 5; i++) {
            if (topNotifications[i] != null) {
                newNotification(context, topNotifications[i], i);
            }
        }
    }

    public static void removeNotification(NotificationListenerService service, StatusBarNotification[] activeNotifications, int id) {
        Log.i(PebbleWearService.TAG, "NotificationInterface.removeNotification() id: " + id);
        StatusBarNotification[] topNotifications = new StatusBarNotification[5];
        for (StatusBarNotification notification : activeNotifications) {
            boolean found = false;
            for(int i = 0; i < 5; i++) {
                if (!found) {
                    if (topNotifications[i] != null) {
                        if (topNotifications[i].getNotification().priority <= notification.getNotification().priority){
                            for (int j = 4; j > i; j--) {
                                topNotifications[j] = topNotifications[j-1];
                            }
                            topNotifications[i] = notification;
                            found = true;
                        }
                    } else {
                        topNotifications[i] = notification;
                        found = true;
                    }
                }
            }
        }

        service.cancelNotification(topNotifications[id].getPackageName(), topNotifications[id].getTag(), topNotifications[id].getId());
    }

    public void sendActions(Context context, StatusBarNotification[] activeNotifications, int id) {
        Log.i(PebbleWearService.TAG, "NotificationInterface.sendActions()");
        StatusBarNotification[] topNotifications = new StatusBarNotification[5];
        for(StatusBarNotification notification : activeNotifications) {
            boolean found = false;
            for(int i = 0; i < 5; i++) {
                if (!found) {
                    if (topNotifications[i] != null) {
                        if (topNotifications[i].getNotification().priority <= notification.getNotification().priority) {
                            System.arraycopy(topNotifications, i, topNotifications, i + 1, 4 - i);
                            topNotifications[i] = notification;
                            found = true;
                        }
                    } else {
                        topNotifications[i] = notification;
                        found = true;
                    }
                }
            }
        }
        if (topNotifications[id].getNotification().actions != null) {
            PebbleDictionary message = new PebbleDictionary();
            message.addInt8(COMMAND, ACTIONS);
            message.addBytes(BYTES, Utils.getActionBytes(topNotifications[id].getNotification().actions));
            mMessageInterface.send(context, message);
        }
    }

    public void newNotification(Context context, StatusBarNotification sbn, int position) {
        Log.i(PebbleWearService.TAG, "NotificationInterface.newNotification() position " + position);
        PebbleDictionary message = new PebbleDictionary();
        message.addInt8(COMMAND, CLEAR);
        message.addInt32(ID, position);
        mMessageInterface.send(context, message);

        sendMainContent(context, sbn, position);
        //sendIcon(context, sbn, position);
        //sendImage(context, sbn, position);
    }

    public void updateNotification(StatusBarNotification[] sWatchNotifications, Context context, StatusBarNotification sbn, int watchNo) {
        Log.d(PebbleWearService.TAG, "NotificationInterface.updateNotification()");

        // For convenience.
        Bundle watchExtras = sWatchNotifications[watchNo].getNotification().extras;
        Bundle newExtras = sbn.getNotification().extras;

        // Check if text needs updating.
        boolean updateText = false;
        if (!Utils.getTitleContent(watchExtras).equals(Utils.getTitleContent(newExtras))) {
            updateText = true;
        }
        if (!Utils.getTextContent(watchExtras).equals(Utils.getTextContent(newExtras))) {
            updateText = true;
        }

        // Update text.
        if (updateText) {
            sendMainContent(context, sbn, watchNo);
        }

        // Update icon if needed.
        if (watchExtras.getInt(Notification.EXTRA_SMALL_ICON) != newExtras.getInt(Notification.EXTRA_SMALL_ICON)) {
            //sendIcon(context, sbn, watchNo);
        }

        // Update image if needed.
        if (Utils.getImage(context, sWatchNotifications[watchNo]) != Utils.getImage(context, sbn)) {
            //sendImage(context, sbn, watchNo);
        }
    }

    public void sendIcon(Context context, StatusBarNotification sbn, int position) {
        Log.i(PebbleWearService.TAG, "NotificationInterface.sendIcon()");
        try {
            // Get app name.
            String packageName = sbn.getPackageName();
            //create context (to access resources) may create exception
            Context appContext = context.createPackageContext(packageName, PebbleWearService.CONTEXT_IGNORE_SECURITY);
            //get iconID
            int iconID = sbn.getNotification().extras.getInt(Notification.EXTRA_SMALL_ICON);
            //get the drawable as HIGH_DENSITY (48x48px) (32x32 is another option, but may scale poorly)
            Drawable iconDrawable = appContext.getResources().getDrawableForDensity(iconID, DisplayMetrics.DENSITY_HIGH);
            //create empty bitmap
            Bitmap iconBitmap = Bitmap.createBitmap(iconDrawable.getIntrinsicWidth(), iconDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            //set the bitmap as the canvas
            Canvas canvas = new Canvas(iconBitmap);
            //make sure the drawable bounds are the same
            iconDrawable.setBounds(0,0,canvas.getWidth(),canvas.getHeight());
            //drawable will draw into canvas (which is the bitmap)
            iconDrawable.draw(canvas);

            iconBitmap = Bitmap.createScaledBitmap(iconBitmap, 48, 48, true);

            for(int row = 0; row < 48; row++) {
                Log.i(PebbleWearService.TAG, "NotificationInterface.sendIcon() Sending row");
                PebbleDictionary dictionary = new PebbleDictionary();

                byte iconData[] = new byte[48 / 8]; //6
                for (int byteNo = 0; byteNo < 6; byteNo++) {


                    iconData[byteNo] = 0;
                    for (int bitNo = 0; bitNo < 8; bitNo++) {
                        int color = iconBitmap.getPixel(byteNo * 8+7- bitNo, row);

                        int alpha = Color.alpha(color);
                        int red = Color.red(color);
                        int green = Color.green(color);
                        int blue = Color.blue(color);

                        int average = (int) ((float) ((blue + green + red) / 3)  * ((float) alpha / 255.0));

                        iconData[byteNo] += (average < 140) ? 0 : 1;

                        if (bitNo != 7)
                            iconData[byteNo] <<= 1;
                    }
                }
                dictionary.addInt8(COMMAND, UPDATE_ICON);
                dictionary.addBytes(BYTES, iconData);
                dictionary.addInt32(ID, position);
                dictionary.addInt32(LINE, row);
                mMessageInterface.send(context, dictionary);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void sendMainContent(Context context, StatusBarNotification sbn, int position) {
        Log.i(PebbleWearService.TAG, "NotificationInterface.sendMainContent()");

        // If vibrates.
        Bundle extras = sbn.getNotification().extras;

        PebbleDictionary message = new PebbleDictionary();
        message.addInt8(COMMAND, UPDATE_TEXT);
        message.addInt32(ID, position);
        message.addBytes(BYTES, Utils.getTextByes(extras));
        mMessageInterface.send(context, message);
    }

    public void sendImage(Context context, StatusBarNotification sbn, int position) {
        Log.i(PebbleWearService.TAG, "NotificationInterface.sendImage()");

        Bitmap image = Utils.getImage(context, sbn);
        if(image!=null) {
            // Figure out shortest direction.
            int height = image.getHeight();
            int width = image.getWidth();
            if (height < width) {
                width = (int) ((double) width * ((double) 144 / (double) height));
                height = 144;
            } else {
                height = (int) ((double) height * ((double) 144 / (double) width));
                width = 144;
            }
            //scale to that size
            image = Bitmap.createScaledBitmap(image, width, height, true);
            //figure out starting point
            int top = (height - 144) / 2;
            int left = (width - 144) / 2;
            //crop to 144x144

            Log.d(PebbleWearService.TAG, "NotificationInterface.sendImage() width=" + width + " height=" + height + " top=" + top + " left=" + left);
            image = Bitmap.createBitmap(image, left, top, 144, 144);


            int total_error[][] = new int[144][144];

            for (int row = 0; row < 144; row++) {
                PebbleDictionary dictionary = new PebbleDictionary();
                byte imageData[] = new byte[18]; //144 / 8
                int bits[] = new int[8];

                for(int col = 0; col < 144; col++) {
                    Log.d(PebbleWearService.TAG, "NotificationInterface.sendImage() row=" + row + " col=" + col);

                    //get color from image
                    int color = image.getPixel(col, row);
                    int alpha = Color.alpha(color);
                    int red = Color.red(color);
                    int green = Color.green(color);
                    int blue = Color.blue(color);

                    //grey-scale color
                    int average = (int) ((float) ((blue + green + red) / 3) * ((float) alpha / 255.0));

                    //add total error from previous pixels
                    average += total_error[col][row];

                    //save bit for writing
                    bits[col%8] = (average < 127) ? 0 : 1;

                    //grey-scale color distance from black/white
                    int error_amount = average - ((average < 127) ? 0 : 255);

                    //stucki dithering algorithm
                    //row 1
                    if (col < 143)
                        total_error[col + 1][row] += error_amount * 8 / 42;
                    if(col<142)
                        total_error[col + 1][row] += error_amount * 4 / 42;
                    //row 2
                    if (row < 143) {
                        if (col > 1)
                            total_error[col - 2][row + 1] += error_amount * 2 / 42;
                        if (col > 0)
                            total_error[col - 1][row + 1] += error_amount * 4 / 42;
                        //col is the same
                        total_error[col    ][row + 1] += error_amount * 8 / 42;
                        if (col < 143)
                            total_error[col + 1][row + 1] += error_amount * 4 / 42;
                        if (col < 142)
                            total_error[col + 2][row + 1] += error_amount * 2 / 42;
                    }
                    //row 3
                    if (row < 142) {
                        if (col > 1)
                            total_error[col - 2][row + 2] += error_amount * 1 / 42;
                        if (col > 0)
                            total_error[col - 1][row + 2] += error_amount * 2 / 42;
                        //col is the same
                        total_error[col    ][row + 2] += error_amount * 4 / 42;
                        if (col < 143)
                            total_error[col + 1][row + 2] += error_amount * 2 / 42;
                        if (col < 142)
                            total_error[col + 2][row + 2] += error_amount * 1 / 42;
                    }

                    //if last bit, save bits as byte
                    if (col % 8 == 7) {
                        //calculate byteNo
                        int byteNo = col / 8;

                        //make sure byte is zeroed
                        imageData[byteNo] = 0;

                        for(int bit = 0; bit < 8; bit++){
                            //write bits in reverse order
                            imageData[byteNo] += bits[7-bit];

                            //shift bit if not last
                            if(bit < 7)
                                imageData[byteNo] <<= 1;
                        }
                    }
                }
                dictionary.addInt8(COMMAND, UPDATE_IMAGE);
                dictionary.addInt32(LINE,  row);
                dictionary.addInt32(ID, position);
                dictionary.addBytes(BYTES, imageData);
                mMessageInterface.send(context, dictionary);
            }
        }
    }
}
