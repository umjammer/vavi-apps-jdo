/*
 * https://github.com/fileoffset/JDO
 */

package vavi.util.deobfuscation.common.attrubuteinfo;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import vavi.util.deobfuscation.common.AttributeInfo;
import vavi.util.deobfuscation.common.Common;
import vavi.util.deobfuscation.common.constantpoolinfo.ConstantUtf8Info;
import vavi.util.deobfuscation.objects.ConstantPool;


public class UnknownAttributeInfo extends AttributeInfo {
    int attributeNameIndex;

    ConstantUtf8Info attributeName;

    int attributeLength;

    byte[] bytes;

    public UnknownAttributeInfo(int nameIndex, DataInput reader, ConstantPool constantPool) {
        attributeNameIndex = 0;
        attributeName = null;
        attributeLength = 0;
        bytes = null;

        try {
            attributeNameIndex = nameIndex;
            attributeLength = Common.readInt(reader);
            bytes = new byte[attributeLength];
            reader.readFully(bytes, 0, attributeLength);
            // resolve references
            attributeName = (ConstantUtf8Info) constantPool.getItem(attributeNameIndex);
            attributeName.references++;
        } catch (Exception e) {
            e.printStackTrace(System.err);
            // do nothing
        }
    }

    @Override
    public void write(DataOutput writer) throws IOException {
        Common.writeShort(writer, attributeNameIndex + 1);
        Common.writeInt(writer, attributeLength);
        writer.write(bytes);
    }
}
