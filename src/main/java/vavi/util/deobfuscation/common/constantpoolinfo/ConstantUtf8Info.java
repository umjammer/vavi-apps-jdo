/*
 * https://github.com/fileoffset/JDO
 */

package vavi.util.deobfuscation.common.constantpoolinfo;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
        try {
            tag = ConstantPoolInfoTag.ConstantUtf8.value;
            bytes = text.getBytes("UTF-8");
            length = bytes.length;
            value = new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            System.err.println(e);
            assert false;
        }
    }

    public void setName(String text) {
        try {
            bytes = text.getBytes("UTF-8");
            length = bytes.length;
            value = new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            System.err.println(e);
            assert false;
        }
    }

    @Override
    public void read(int tag, DataInput reader) throws IOException {
        this.tag = tag;
        length = Common.readShort(reader);
        bytes = new byte[length];
        reader.readFully(bytes, 0, length);
        Common.position += length;

        value = new String(bytes, "UTF-8");
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
