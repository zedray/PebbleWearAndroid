package com.richardgg.pebblewear;

import android.app.Notification;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;

public class Utils {

    public static byte[] getActionBytes(Notification.Action[] actions) {
        byte[] output = new byte[60];
        for (int i=0; i<actions.length; i++) {
            for (int j=0; j<20; j++) {
                output[j + i*20] = (byte)actions[i].title.charAt(j);
            }
        }
        return output;
    }

    public static Bitmap getImage(Context context, StatusBarNotification sbn) {
        Bundle extras = sbn.getNotification().extras;

        Bitmap image;
        //check for picture
        image = extras.getParcelable(Notification.EXTRA_PICTURE);
        //check for icon
        if(image == null)
            image = extras.getParcelable(Notification.EXTRA_LARGE_ICON);
        //check for appicon
        if(image == null) {
            try {
                Drawable appicon = context.getPackageManager().getApplicationIcon(sbn.getPackageName());
                //create empty bitmap
                image = Bitmap.createBitmap(appicon.getIntrinsicWidth(), appicon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                //set the bitmap as the canvas
                Canvas appcanvas = new Canvas(image);
                //make sure the drawable bounds are the same
                appicon.setBounds(0, 0, appcanvas.getWidth(), appcanvas.getHeight());
                //drawable will draw into canvas (which is the bitmap)
                appicon.draw(appcanvas);

                // image = Bitmap.createScaledBitmap(image, 144, 144, true);
            } catch (PackageManager.NameNotFoundException e) {
                image = null;
            }
        }
        return image;
    }

    public static byte[] getTextByes(Bundle extras) {
        CharSequence title = Utils.getTitleContent(extras);
        CharSequence text = Utils.getTextContent(extras);
        byte[] output = new byte[60];
        for(int i=0; i < 20; i++) {
            if(i < title.length())
                if(title.charAt(i) < 255)
                    output[i] = (byte) title.charAt(i);
                else
                    output[i] = (byte)'?';
            else
                output[i] = '\0';
        }
        for(int i=0;i<40;i++){
            if(i < text.length())
                if(text.charAt(i) < 255)
                    output[i+20] = (byte) text.charAt(i);
                else
                    output[i+20] = (byte)'?';
            else
                output[i+20] = '\0';
        }
        return output;
    }

    public static CharSequence getTitleContent(Bundle extras) {
        CharSequence titleContent = "";
        if (extras.get(Notification.EXTRA_TITLE) != null) {
            int messageLength = extras.getCharSequence(Notification.EXTRA_TITLE).length();
            if (messageLength > 20)
                messageLength = 20;
            titleContent = extras.getCharSequence(Notification.EXTRA_TITLE).subSequence(0, messageLength).toString();
        }
        return titleContent;

    }

    public static CharSequence getTextContent(Bundle extras) {
        CharSequence textContent = " ";
        if(extras.get(Notification.EXTRA_TEXT) != null)
        {
            int messageLength = extras.getCharSequence(Notification.EXTRA_TEXT).length();
            if (messageLength > 40)
                messageLength = 40;
            textContent = extras.getCharSequence(Notification.EXTRA_TEXT).subSequence(0, messageLength).toString();
        }
        return textContent;
    }
}
