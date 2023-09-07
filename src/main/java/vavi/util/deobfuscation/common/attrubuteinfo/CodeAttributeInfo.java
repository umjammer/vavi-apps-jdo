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
import vavi.util.deobfuscation.common.info.ExceptionTable;
import vavi.util.deobfuscation.objects.Attributes;
import vavi.util.deobfuscation.objects.ConstantPool;


public class CodeAttributeInfo extends AttributeInfo {
    // stuff we need
    int attributeNameIndex;

    ConstantUtf8Info attributeName;

    int attributeLength;

    int maxStack;

    int maxLocals;

    int codeLength;

    byte[] code;

    int exceptionTableLength;

    ExceptionTable[] exceptionTable;

    Attributes attributes;

    // stuff I want
    long offsetOfCode;

    public CodeAttributeInfo(int nameIndex, DataInput reader, ConstantPool constantPool) {
        attributeNameIndex = 0;
        attributeName = null;
        attributeLength = 0;
        maxStack = 0;
        maxLocals = 0;
        codeLength = 0;
        code = null;
        exceptionTableLength = 0;
        exceptionTable = null;
        attributes = null;

        try {
            attributeNameIndex = nameIndex;
            attributeLength = Common.readInt(reader);

            maxStack = Common.readShort(reader);
            maxLocals = Common.readShort(reader);
            codeLength = Common.readInt(reader);

            // save the offset of the code stream
            offsetOfCode = Common.position;

            code = new byte[codeLength];
            reader.readFully(code, 0, codeLength);
            Common.position += codeLength;

            exceptionTableLength = Common.readShort(reader);
            exceptionTable = new ExceptionTable[exceptionTableLength];
            // fucking nested arrays! ;/
            for (int i = 0; i < exceptionTableLength; i++) {
                exceptionTable[i] = new ExceptionTable(reader, constantPool);
            }

            attributes = new Attributes(reader, constantPool);

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

        Common.writeShort(writer, maxStack);
        Common.writeShort(writer, maxLocals);
        Common.writeInt(writer, codeLength);
        writer.write(code);

        Common.writeShort(writer, exceptionTableLength);

        for (int i = 0; i < exceptionTableLength; i++) {
            exceptionTable[i].write(writer);
        }

        attributes.write(writer);
    }

    public long getCodeOffset() {
        return offsetOfCode;
    }
}
/* */
