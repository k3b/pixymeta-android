package pixy.android;

import java.io.IOException;
import java.io.InputStream;

import pixy.image.BitmapFactory;
import pixy.image.IBitmap;
import pixy.meta.adobe.ImageResourceID;

/**
 * Created by k3b on 31.05.2016.
 */
public class BitmapFactoryNative implements BitmapFactory.IBitmapFactory {
    static {
        BitmapFactory.register(new BitmapFactoryNative());
    }

    public static void init() {
        // do nothing. just make shure that the static constructor has been called
    }

    @Override
    public IBitmap createBitmap(int colors[], int width, int height, int totalSize, byte[] thumbnailData, int paddedRowBytes, ImageResourceID id) {
        return BitmapNative.createBitmap(colors, width, height);
    }

    /**
     * Decode an input stream into a bitmap. If the input stream is null, or
     * cannot be used to decode a bitmap, the function returns null.
     * The stream's position will be where ever it was after the encoded data
     * was read.
     *
     * @param is The input stream that holds the raw data to be decoded into a
     *           bitmap.
     * @return The decoded bitmap, or null if the image data could not be decoded.
     */
    public IBitmap decodeStream(InputStream is)  throws IOException {
        return BitmapNative.decodeStream(is);
    }

}
