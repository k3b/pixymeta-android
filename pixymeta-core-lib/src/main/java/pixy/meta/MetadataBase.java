package pixy.meta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import pixy.api.IDirectory;
import pixy.api.IMetadata;
import pixy.util.ArrayUtils;

/**
 * Created by k3b on 10.07.2016.
 */
public abstract class MetadataBase  implements IMetadata {
    // Obtain a logger instance
    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataBase.class);

    // Fields
    protected MetadataType type;
    private byte[] data;
    protected boolean isDataRead;
    private StringBuilder debugMessageBuffer = null;

    public MetadataBase(MetadataType type, byte[] data) {
        if ((data != null) && isDebugEnabled()) debug("ctor(" + data.length + ")");
        this.type = type;
        this.setData(data);
        isDataRead = false;
    }

    public void merge(byte[] data) {
        this.setData(ArrayUtils.concat(this.getData(), data));
        isDataRead = false;
        if ((data != null) && isDebugEnabled()) debug("merge(" + data.length + ") => " + this.getData().length);
    }

    protected void ensureDataRead() {
        if(!isDataRead) {
            try {
                debug("ensureDataRead->read");
                read();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public byte[] getData() {
        if(data != null)
            return data.clone();

        return null;
    }

    @Override
    public MetadataType getType() {
        return type;
    }

    @Override
    public boolean isDataRead() {
        return isDataRead;
    }

    //TODO implement for all
    // public abstract void addField(IFieldDefinition tag, Object data);

    /**
     * Writes the metadata out to the output stream
     *
     * @param out OutputStream to write the metadata to
     * @throws IOException
     */
    @Override
    public void write(OutputStream out) throws IOException {
        byte[] data = getData();
        if(data != null) {
            if (isDebugEnabled()) debug("write " + data.length + " bytes");
            out.write(data);
        }
    }

    /**
     * @return directories that belong to this MetaData
     * */
    @Override
    public List<IDirectory> getMetaData() {
        return null;
    }

    public String getDebugMessage() {
        return (debugMessageBuffer != null) ? debugMessageBuffer.toString() : null;
    }

    public boolean isDebugEnabled() {
        return (null != debugMessageBuffer);
    }

    public IMetadata setDebugMessageBuffer(StringBuilder debugMessageBuffer) {
        this.debugMessageBuffer = debugMessageBuffer;
        return this;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +"[" + type + "]";
    }

    protected void debug(String s) {
        if (this.debugMessageBuffer != null) {
            this.debugMessageBuffer
                    .append(getClass().getSimpleName()).append(":")
                    .append(s).append("\n");
        }
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
