package com.homepunk.github.vinylrecognizer.util;

/**
 * Created by Homepunk on 23.02.2018.
 **/

public class ColorUtil {
    public static String getHexColorAlpha(int percentage, String colorCode){
        double decValue = ((double)percentage / 100) * 255;
        String rawHexColor = colorCode.replace("#","");
        StringBuilder str = new StringBuilder(rawHexColor);

        if(Integer.toHexString((int)decValue).length() == 1)
            str.insert(0, "#0" + Integer.toHexString((int)decValue));
        else
            str.insert(0, "#" + Integer.toHexString((int)decValue));
        return str.toString();
    }
}
