/*
 * https://github.com/fileoffset/JDO
 */

package vavi.util.deobfuscation.common.constantpoolinfo;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;

import vavi.util.deobfuscation.common.Common;


public class ConstantDoubleInfo extends ConstantPoolVariableInfo {
    private int highBytes;

    private int lowBytes;

    @Override
    public void read(int tag, DataInput reader) throws IOException {
        this.tag = tag;
        highBytes = Common.readInt(reader);
        lowBytes = Common.readInt(reader);
        value = "NOT_IMPLEMENTED";
    }

    @Override
    public void write(DataOutput writer) throws IOException {
        // write the tag
        Common.writeByte(writer, tag);
        Common.writeInt(writer, highBytes);
        Common.writeInt(writer, lowBytes);
    }

    @Override
    public boolean resolve(List<?> items) {
        value = Double.longBitsToDouble(((long) highBytes << 32) + lowBytes);
        return true;
    }
}
