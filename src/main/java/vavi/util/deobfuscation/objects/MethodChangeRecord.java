/*
 * https://github.com/fileoffset/JDO
 */

package vavi.util.deobfuscation.objects;

import vavi.util.deobfuscation.common.info.MethodInfo;

public class MethodChangeRecord implements ChangeRecord {
    // just a simple class to hold the information temporarily
    private MethodInfo originalMethod;

    private MethodInfo newMethod;

    public MethodChangeRecord(MethodInfo original) {
        originalMethod = (MethodInfo) original.clone();
    }

    public void changedTo(MethodInfo new_) {
        newMethod = (MethodInfo) new_.clone();
    }

    public MethodInfo getOriginalMethod() {

        return originalMethod;
    }

    public MethodInfo getNewMethod() {
        return newMethod;
    }
}
