package pixy.string;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by k3b on 07.07.2016.
 */
public class ParserTests {
    @Test
    public void shouldParseIntList()  {
        String strValue = "[1,2,3]";
        int[] val = StringUtils.parseIntList(strValue);
        String result = StringUtils.toListString(val,false);
        Assert.assertEquals(strValue, result);
    }
    @Test
    public void shouldParseShortList()  {
        String strValue = "[1,2,3]";
        short[] val = StringUtils.parseShortList(strValue);
        String result = StringUtils.toListString(val,false);
        Assert.assertEquals(strValue, result);
    }
    @Test
    public void shouldParseByteList()  {
        String strValue = "[0x01,0x02,0x03]";
        byte[] val = StringUtils.parseHexByteList(strValue);
        String result = StringUtils.toHexListString(val);
        Assert.assertEquals(strValue, result);
    }
}
