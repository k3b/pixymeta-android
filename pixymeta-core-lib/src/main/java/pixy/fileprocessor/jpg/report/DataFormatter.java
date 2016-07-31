package pixy.fileprocessor.jpg.report;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Copyright (C) 2016 by k3b.
 *
 * Used by regressiontest data to make the display (and file) shorter
 *
 * Created by k3b on 27.07.2016.
 */
public class DataFormatter {
    private static final int MAXLEN = 81;
    private static final int SPLITPOS = MAXLEN / 3;

    /** if value is unformatted and to lonog return abriviated value */
    public static String abreviateValue(String s) {
        if (s == null) return null;

        // xml may contain "<?xpacket begin='ï»¿'  ..." at the end of the xml. that sometimes
        // contain utf8-BOM that gets lost. replaceAll("ï»¿","") does not work
        int end = s.indexOf("<?xpacket begin");
        if (end >= 0) s = s.substring(0, end);

        final int length = s.length();
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
