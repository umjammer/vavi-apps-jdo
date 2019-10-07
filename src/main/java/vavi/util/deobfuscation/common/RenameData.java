/*
 * https://github.com/fileoffset/JDO
 */

package vavi.util.deobfuscation.common;

public class RenameData {
    String type;

    String name;

    public RenameData(String Type, String Name) {
        type = Type;
        name = Name;
    }

    public String[] getData() {
        String[] s = new String[2];
        s[0] = type;
        s[1] = name;

        return s;
    }

    public String getFieldType() {
        return type;
    }

    public void setFieldType(String value) {
        type = value;
    }

    public String getFieldName() {
        return name;
    }

    public void setFieldName(String value) {
        name = value;
    }
}
