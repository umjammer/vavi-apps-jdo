/*
 * https://github.com/fileoffset/JDO
 */

package vavi.util.deobfuscation.common.constantpoolinfo;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import vavi.util.deobfuscation.common.Common;
import vavi.util.deobfuscation.common.ConstantPoolInfo;
import vavi.util.deobfuscation.common.ConstantPoolInfoTag;


public class ConstantUtf8Info extends ConstantPoolInfo {
    public int length;

    public byte[] bytes;

    public String value;

    public ConstantUtf8Info() {
        bytes = null;
        length = 0;
        tag = ConstantPoolInfoTag.ConstantUtf8.value;
        references = 0;
    }

    public ConstantUtf8Info(String text) {
        tag = ConstantPoolInfoTag.ConstantUtf8.value;
        bytes = text.getBytes(StandardCharsets.UTF_8);
        length = bytes.length;
        value = new String(bytes, StandardCharsets.UTF_8);
    }

    public void setName(String text) {
        bytes = text.getBytes(StandardCharsets.UTF_8);
        length = bytes.length;
        value = new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public void read(int tag, DataInput reader) throws IOException {
        this.tag = tag;
        length = Common.readShort(reader);
        bytes = new byte[length];
        reader.readFully(bytes, 0, length);
        Common.position += length;

        value = new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public void write(DataOutput writer) throws IOException {
        // write the tag
        Common.writeByte(writer, tag);
        Common.writeShort(writer, length);
        writer.write(bytes);
        Common.position += length;
    }

    @Override
    public boolean resolve(List<?> items) {
        return true;
    }
}
