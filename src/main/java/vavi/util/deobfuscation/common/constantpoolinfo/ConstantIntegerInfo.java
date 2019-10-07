/*
 * https://github.com/fileoffset/JDO
 */

package vavi.util.deobfuscation.common.constantpoolinfo;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;

import vavi.util.deobfuscation.common.Common;


public class ConstantIntegerInfo extends ConstantPoolVariableInfo {
    private int bytes;

    @Override
    public void read(int tag, DataInput reader) throws IOException {
        this.tag = tag;
        bytes = Common.readInt(reader);
    }

    @Override
    public void write(DataOutput writer) throws IOException {
        // write the tag
        Common.writeByte(writer, tag);
        Common.writeInt(writer, bytes);
    }

    @Override
    public boolean resolve(List<?> items) {
        value = (int) bytes;
        return true;
    }
}
