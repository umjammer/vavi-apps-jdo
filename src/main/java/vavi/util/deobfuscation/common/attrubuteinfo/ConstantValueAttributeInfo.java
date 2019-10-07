/*
 * https://github.com/fileoffset/JDO
 */

package vavi.util.deobfuscation.common.attrubuteinfo;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import vavi.util.deobfuscation.common.AttributeInfo;
import vavi.util.deobfuscation.common.Common;
import vavi.util.deobfuscation.common.constantpoolinfo.ConstantPoolVariableInfo;
import vavi.util.deobfuscation.common.constantpoolinfo.ConstantUtf8Info;
import vavi.util.deobfuscation.objects.ConstantPool;


public class ConstantValueAttributeInfo extends AttributeInfo {
    int attributeNameIndex;

    int attributeLength;

    ConstantUtf8Info attributeName;

    int constantValueIndex;

    ConstantPoolVariableInfo constantValue;

    public ConstantValueAttributeInfo(int nameIndex, DataInput reader, ConstantPool constantPool) {
        attributeNameIndex = 0;
        attributeLength = 0;
        attributeName = null;
        constantValueIndex = 0;
        constantValue = null;

        try {
            attributeNameIndex = nameIndex;
            attributeLength = Common.readInt(reader);
            constantValueIndex = Common.readShort(reader);
            constantValueIndex--;
            // resolve references
            attributeName = (ConstantUtf8Info) constantPool.getItem(attributeNameIndex);
            attributeName.references++;
            constantValue = (ConstantPoolVariableInfo) constantPool.getItem(constantValueIndex);
            constantValue.references++;
        } catch (Exception e) {
            e.printStackTrace(System.err);
            // do nothing
        }
    }

    @Override
    public void write(DataOutput writer) throws IOException {
        Common.writeShort(writer, attributeNameIndex + 1);
        Common.writeInt(writer, attributeLength);
        Common.writeShort(writer, constantValueIndex + 1);
    }
}
