/*
 * https://github.com/fileoffset/JDO
 */

package vavi.util.deobfuscation.common.constantpoolinfo;

import vavi.util.deobfuscation.common.ConstantPoolInfo;
import vavi.util.deobfuscation.objects.ConstantPool;

public abstract class ConstantPoolMethodInfo extends ConstantPoolInfo {
    public ConstantClassInfo parentClass;

    public ConstantNameAndTypeInfo nameAndType;

//    private int classIndex;

//    private int nameAndTypeIndex;

    public abstract void setNameAndType(int index, ConstantPool constantPool);

    public abstract void setParent(int index, ConstantPool constantPool);
}
