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
import vavi.util.deobfuscation.common.info.TLocalVariableTable;
import vavi.util.deobfuscation.objects.ConstantPool;


public class LocalVariablesAttributeInfo extends AttributeInfo {
    int attributeNameIndex;

    ConstantUtf8Info attributeName;

    int attributeLength;

    int localVariableTableLength;

    TLocalVariableTable[] localVariableTable;

    public LocalVariablesAttributeInfo(int nameIndex, DataInput reader, ConstantPool constantPool) {
        attributeNameIndex = 0;
        attributeName = null;
        attributeLength = 0;
        localVariableTableLength = 0;
        localVariableTable = null;

        try {
            attributeNameIndex = nameIndex;
            attributeLength = Common.readInt(reader);

            localVariableTableLength = Common.readShort(reader);
            localVariableTable = new TLocalVariableTable[localVariableTableLength];
            // fucking nested arrays! ;/
            for (int i = 0; i < localVariableTableLength; i++) {
                localVariableTable[i] = new TLocalVariableTable(reader, constantPool);
            }
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

        Common.writeShort(writer, localVariableTableLength);
        for (int i = 0; i < localVariableTableLength; i++) {
            localVariableTable[i].write(writer);
        }
    }
}
