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


public class ConstantStringInfo extends ConstantPoolVariableInfo {
    private int nameIndex;

    public ConstantStringInfo() {
        nameIndex = 0;
        value = "";
    }

    @Override
    public void read(int tag, DataInput reader) throws IOException {
        this.tag = tag;
        nameIndex = Common.readShort(reader);
        nameIndex--;
    }

    @Override
    public void write(DataOutput writer) throws IOException {
        // write the tag
        Common.writeByte(writer, tag);
        Common.writeShort(writer, nameIndex + 1);
    }

    @Override
    public boolean resolve(List<?> items) throws IOException {
        // use the index into the constant pool table
        // to find our UTF8 encoded class or interface name
        if (nameIndex < items.size()) {
            Object o = items.get(nameIndex);
            if (o instanceof ConstantUtf8Info) {
                value = new String(((ConstantUtf8Info) o).bytes, "UTF-8");
                ((ConstantPoolInfo) o).references++;

                return true;
            }
        }

        return false;
    }
}
