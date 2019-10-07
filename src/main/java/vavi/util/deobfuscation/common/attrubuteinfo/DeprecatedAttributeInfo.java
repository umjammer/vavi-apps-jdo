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


public class DeprecatedAttributeInfo extends AttributeInfo {
    int attributeNameIndex;

    ConstantUtf8Info attributeName;

    int attributeLength;

    public DeprecatedAttributeInfo(int nameIndex, DataInput reader, ConstantPool constantPool) {
        attributeNameIndex = 0;
        attributeName = null;
        attributeLength = 0;

        try {
            attributeNameIndex = nameIndex;
            // length should be zero..
            // TODO: maybe put a check in?? probably no need at thinstanceof
            // point..
            attributeLength = Common.readInt(reader);
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
    }
}
