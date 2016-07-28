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

    private DefaultApiImpl() {

    }

    public static IDirectory createDirecotry(String name) {
        return new DefaultApiImpl().setName(name);
    }

    public static IDataType createDataType(String name) {
        return new DefaultApiImpl().setName(name);
    }

    private static IFieldDefinition createFieldDefinition(String type) {
        return new DefaultApiImpl().setName(type);
    }

    /** simuate IDataType, IDirectory or IFieldDefinition */
    private DefaultApiImpl(String name) {
        this.name = name;
        // this.value = value;
    }

    public static IDirectory createDirecotry(String name, IFieldDefinition fieldDefinition, String value) {
        return new DefaultApiImpl(name, fieldDefinition, value);
    }
    /** create IDirectory with values of type IFieldDefinition */
    private DefaultApiImpl(String name, IFieldDefinition fieldDefinition, String value) {
        this.name = name;
        getValues().add(new DefaultApiImpl(fieldDefinition, value));
    }

    public static IFieldValue createFieldValue(String type, String value) {
        return new DefaultApiImpl(createFieldDefinition(type), value);
    }

    public static IFieldValue createFieldValue(IFieldDefinition fieldDefinition, String value) {
        final DefaultApiImpl result = new DefaultApiImpl(fieldDefinition, value);
        result.name = fieldDefinition.getName() + "=" + value;
        return result;

    }

    /** simualte IFieldValue */
    private DefaultApiImpl(IFieldDefinition fieldDefinition, String value) {
        name = value;
        this.fieldDefinition = fieldDefinition;
        this.value = value;
    }

    public DefaultApiImpl(String name, IDataType dataType) {
        this.dataType = dataType;
        this.name = name;
    }

    private DefaultApiImpl(String name, List<IFieldValue> values) {
        this.name = name;
        this.values = values;
    }

    public static IDirectory createDirectory(String name, List<IFieldValue> values) {
        DefaultApiImpl result = new DefaultApiImpl();
        result.values = values;
        result.name = name;
        return result;
    }

    @Override
    public DefaultApiImpl setName(String name) {
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

    @Override
    public String toString() {
        return this.name;
    }

}
