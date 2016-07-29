package pixy.api;

/**
 * Created by k3b on 28.07.2016.
 */
public class DebuggableBase {
    private StringBuilder debugMessageBuffer = null;

    public String getDebugMessage() {
        return (debugMessageBuffer != null) ? debugMessageBuffer.toString() : null;
    }

    public boolean isDebugEnabled() {
        return (null != debugMessageBuffer);
    }

    public void setDebugMessageBuffer(StringBuilder debugMessageBuffer) {
        this.debugMessageBuffer = debugMessageBuffer;
    }

    protected void debug(String s) {
        if (this.debugMessageBuffer != null) {
            this.debugMessageBuffer
                    .append(getClass().getSimpleName()).append(":")
                    .append(s).append("\n");
        }
    }
}
