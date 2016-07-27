package pixy.api;

import java.util.ArrayList;
import java.util.List;

/**
 * This can be used as IDataType, IFieldDefinition or as IFieldValue
 * Created by k3b on 17.06.2016.
 */
public class DefaultApiImpl implements IDataType, IFieldDefinition, IFieldValue, IDirectory {
    private String name;
    private IFieldDefinition fieldDefinition;
    private String value;
    private IDataType dataType = UNKNOWN;

    public static DefaultApiImpl UNKNOWN = new DefaultApiImpl("JPG_SEGMENT_UNKNOWN");
    private List<IFieldValue> values = null;

    /** simuate IDataType, IDirectory or IFieldDefinition */
    public DefaultApiImpl(String name) {
        this.name = name;
        // this.value = value;
    }

    /** create IDirectory with values of type IFieldDefinition */
    public DefaultApiImpl(String name, IFieldDefinition fieldDefinition, String... values) {
        this.name = name;
        for (String item: values) {
            getValues().add(new DefaultApiImpl(fieldDefinition, item));
        }
    }

    /** simualte IFieldValue */
    public DefaultApiImpl(IFieldDefinition fieldDefinition, String value) {
        this.fieldDefinition = fieldDefinition;
        this.value = value;
    }

    public DefaultApiImpl(String name, IDataType dataType) {
        this.dataType = dataType;
        this.name = name;
    }

    public DefaultApiImpl(String name, List<IFieldValue> values) {

        this.name = name;
        this.values = values;
    }

    @Override
    public IDirectory setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<IFieldValue> getValues() {
        if (values == null) values = new ArrayList<>();
        return values;
    }

    /**
     * return values for fieldDefinition or null if it does not exist.
     *
     * @param fieldDefinition
     */
    @Override
    public IFieldValue getValue(IFieldDefinition fieldDefinition) {
        return getValue(getValues(), fieldDefinition);
    }

    public static IFieldValue getValue(List<IFieldValue> values, IFieldDefinition fieldDefinition) {
        if ((values != null) && (fieldDefinition != null)) {
            for (IFieldValue value : values) {
                if ((value != null) && fieldDefinition.equals(value.getDefinition())) return value;
            }
        }
        return null;
    }

    @Override
    public IFieldDefinition getDefinition() {
        return fieldDefinition;
    }

    @Override
    public String getValueAsString() {
        return value;
    }

    @Override
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public IDataType getDataType() {
        if (fieldDefinition != null) return fieldDefinition.getDataType();
        return dataType;
    }

    public static boolean isNull(Object candidate) {
        return ((candidate == null) || (candidate == UNKNOWN));
    }
}
