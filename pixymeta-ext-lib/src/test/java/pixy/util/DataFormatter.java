package pixy.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by k3b on 27.07.2016.
 */
public class DataFormatter {
    private static final int MAXLEN = 81;
    private static final int SPLITPOS = MAXLEN / 3;

    /** if value is unformatted and to lonog return abriviated value */
    public static String abreviateValue(String s) {
        final int length = (s == null) ? (MAXLEN) : s.length();
        if (length < MAXLEN) return s;

        if (s.indexOf('\n') >= 0) return s; // formatted text containing newline (i.e. xml) should be returned unmodified

        return "[" + length + " bytes, md5=" + getMd5(s) + "] "
                + s.substring(0, SPLITPOS) + "..." + s.substring(length - SPLITPOS, length - 1);
    }
    private static String getMd5(String s) {
        // md5 from http://stackoverflow.com/questions/304268/getting-a-files-md5-checksum-in-java
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(s.getBytes(),0,s.length());
            String signature = new BigInteger(1,md5.digest()).toString(16);
            return signature;
        } catch (final NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }
}
