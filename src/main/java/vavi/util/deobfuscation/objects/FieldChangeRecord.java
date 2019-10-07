/*
 * https://github.com/fileoffset/JDO
 */

package vavi.util.deobfuscation.objects;

import vavi.util.deobfuscation.common.info.FieldInfo;

/** just a simple class to hold the information temporarily */
public class FieldChangeRecord implements ChangeRecord {
    private FieldInfo originalField;

    private FieldInfo newField;

    public FieldChangeRecord(FieldInfo original) {
        originalField = (FieldInfo) original.clone();
    }

    public void changedTo(FieldInfo new_) {
        newField = (FieldInfo) new_.clone();
    }

    public FieldInfo getOriginalField() {
        return originalField;
    }

    public FieldInfo getNewField() {
        return newField;
    }
}
