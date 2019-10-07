/*
 * https://github.com/fileoffset/JDO
 */

package vavi.util.deobfuscation.common.info;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import vavi.util.deobfuscation.common.Common;
import vavi.util.deobfuscation.common.constantpoolinfo.ConstantUtf8Info;
import vavi.util.deobfuscation.objects.ConstantPool;


public class TLocalVariableTable {
    int startPC;

    int length;

    int nameIndex;

    ConstantUtf8Info name;

    int descriptorIndex;

    ConstantUtf8Info descriptor;

    int Index;

    public TLocalVariableTable(DataInput reader, ConstantPool constantPool) {
        startPC = 0;
        length = 0;
        nameIndex = 0;
        name = null;
        descriptorIndex = 0;
        descriptor = null;
        Index = 0;

        try {
            startPC = Common.readShort(reader);
            startPC--;
            length = Common.readShort(reader);
            nameIndex = Common.readShort(reader);
            nameIndex--;
            descriptorIndex = Common.readShort(reader);
            descriptorIndex--;
            Index = Common.readShort(reader);
            Index--;
            // resolve references
            name = (ConstantUtf8Info) constantPool.getItem(nameIndex);
            name.references++;
            descriptor = (ConstantUtf8Info) constantPool.getItem(descriptorIndex);
            descriptor.references++;
        } catch (Exception e) {
            e.printStackTrace(System.err);
            // do nothing
        }
    }

    public void write(DataOutput writer) throws IOException {
        Common.writeShort(writer, startPC);
        Common.writeShort(writer, length);
        Common.writeShort(writer, nameIndex + 1);
        Common.writeShort(writer, descriptorIndex + 1);
        Common.writeShort(writer, Index + 1);
    }
}
