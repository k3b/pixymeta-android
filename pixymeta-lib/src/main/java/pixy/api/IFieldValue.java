package pixy.api;

/**
 * Created by k3b on 17.06.2016.
 */
public interface IFieldValue {
    IFieldDefinition getDefinition();
    String getValueAsString();
    IDataType getDataType();
}
