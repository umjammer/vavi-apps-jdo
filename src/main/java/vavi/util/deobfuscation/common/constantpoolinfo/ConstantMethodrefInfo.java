/*
 * https://github.com/fileoffset/JDO
 */

package vavi.util.deobfuscation.common.constantpoolinfo;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;

import vavi.util.deobfuscation.common.Common;
import vavi.util.deobfuscation.common.ConstantPoolInfo;
import vavi.util.deobfuscation.objects.ConstantPool;


public class ConstantMethodrefInfo extends ConstantPoolMethodInfo {
    private int classIndex;

    private int nameAndTypeIndex;

    @Override
    public void read(int tag, DataInput Reader) throws IOException {
        this.tag = tag;
        classIndex = Common.readShort(Reader);
        nameAndTypeIndex = Common.readShort(Reader);
        classIndex--;
        nameAndTypeIndex--;
    }

    @Override
    public void write(DataOutput writer) throws IOException {
        // write the tag
        Common.writeByte(writer, tag);
        Common.writeShort(writer, classIndex + 1);
        Common.writeShort(writer, nameAndTypeIndex + 1);
    }

    @Override
    public boolean resolve(List<?> items) {
        // use the index into the constant pool table
        // to find our UTF8 encoded class or interface name
        if (classIndex < items.size() && nameAndTypeIndex < items.size()) {
            Object o = items.get(classIndex);
            if (o instanceof ConstantClassInfo) {
                parentClass = (ConstantClassInfo) o;
                ((ConstantPoolInfo) o).references++;
            }

            o = items.get(nameAndTypeIndex);
            if (o instanceof ConstantNameAndTypeInfo) {
                nameAndType = (ConstantNameAndTypeInfo) o;
                ((ConstantPoolInfo) o).references++;
            }

            return true;
        }

        return false;
    }

    @Override
    public void setNameAndType(int Index, ConstantPool constantPool) {
        nameAndTypeIndex = Index;
        nameAndType = (ConstantNameAndTypeInfo) constantPool.getItem(Index);
        nameAndType.references++;
    }

    @Override
    public void setParent(int index, ConstantPool constantPool) {
        classIndex = index;
        parentClass = (ConstantClassInfo) constantPool.getItem(index);
        parentClass.references++;
    }
}
/* */
