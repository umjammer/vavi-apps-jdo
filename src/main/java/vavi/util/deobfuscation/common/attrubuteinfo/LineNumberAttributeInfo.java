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
import vavi.util.deobfuscation.common.info.TLineNumberTable;
import vavi.util.deobfuscation.objects.ConstantPool;


public class LineNumberAttributeInfo extends AttributeInfo {
    int attributeNameIndex;

    ConstantUtf8Info attributeName;

    int attributeLength;

    int lineNumberTableLength;

    TLineNumberTable[] lineNumberTable;

    long originalPos;

    public LineNumberAttributeInfo(int nameIndex, DataInput reader, ConstantPool constantPool) {
        attributeNameIndex = 0;
        attributeName = null;
        attributeLength = 0;
        lineNumberTableLength = 0;
        lineNumberTable = null;
        originalPos = 0;

        try {
            attributeNameIndex = nameIndex;
            attributeLength = Common.readInt(reader);
            originalPos = Common.position;
            System.err.printf("originalPos: %d\n", originalPos);
            lineNumberTableLength = Common.readShort(reader);
            lineNumberTable = new TLineNumberTable[lineNumberTableLength];
            // fucking nested arrays! ;/
            for (int i = 0; i < lineNumberTableLength; i++) {
                lineNumberTable[i] = new TLineNumberTable(reader);
            }
            // resolve references
            attributeName = (ConstantUtf8Info) constantPool.getItem(attributeNameIndex);
            attributeName.references++;

            System.err.printf("Common.position: 1: %d\n", Common.position);
            Common.position = originalPos + attributeLength;
            System.err.printf("Common.position: 2: %d\n", originalPos + attributeLength);
        } catch (Exception e) {
            // do nothing
            e.printStackTrace(System.err);
        }
    }

    @Override
    public void write(DataOutput writer) throws IOException {
        Common.writeShort(writer, attributeNameIndex + 1);
        Common.writeInt(writer, attributeLength);

        Common.writeShort(writer, lineNumberTableLength);
        for (int i = 0; i < lineNumberTableLength; i++) {
            lineNumberTable[i].write(writer);
        }
    }
}
