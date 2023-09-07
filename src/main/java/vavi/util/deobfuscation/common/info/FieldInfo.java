/*
 * https://github.com/fileoffset/JDO
 */

package vavi.util.deobfuscation.common.info;

import java.io.DataInput;
import java.io.DataOutput;

import vavi.util.deobfuscation.common.Common;
import vavi.util.deobfuscation.common.constantpoolinfo.ConstantUtf8Info;
import vavi.util.deobfuscation.objects.Attributes;
import vavi.util.deobfuscation.objects.ConstantPool;


public class FieldInfo {
    int accessFlags;

    int nameIndex;

    int descriptorIndex;

    ConstantUtf8Info name;

    ConstantUtf8Info descriptor;

    Attributes attributes;

    // my vars
    long offset;

    private FieldInfo() {
    }

    public FieldInfo(DataInput reader, ConstantPool constantPool) {
        accessFlags = 0;
        nameIndex = 0;
        descriptorIndex = 0;
        name = null;
        descriptor = null;
        attributes = null;
        offset = 0;

        try {
            offset = Common.position;
            accessFlags = Common.readShort(reader);
            nameIndex = Common.readShort(reader);
            nameIndex--;
            descriptorIndex = Common.readShort(reader);
            descriptorIndex--;
            // resolve the references
            descriptor = (ConstantUtf8Info) constantPool.getItem(descriptorIndex);
            descriptor.references++;
            name = (ConstantUtf8Info) constantPool.getItem(nameIndex);
            name.references++;
            // Attributes should be able to handle any/all attribute streams
            attributes = new Attributes(reader, constantPool);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            // do nothing for now
        }
    }

    public void setName(int index, ConstantPool constantPool) {
        nameIndex = index;
        name = (ConstantUtf8Info) constantPool.getItem(nameIndex);
        name.references++;
    }

    public ConstantUtf8Info getName() {
        return name;
    }

    public void write(DataOutput writer) {
        try {
            offset = Common.position;
            Common.writeShort(writer, accessFlags);
            Common.writeShort(writer, nameIndex + 1);
            Common.writeShort(writer, descriptorIndex + 1);

            // Attributes should be able to handle any/all attribute streams
            attributes.write(writer);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            // do nothing for now
        }
    }

    public String getDescriptor() {
        return descriptor.value;
    }

    public int getNameIndex() {
        return nameIndex;
    }

    public Attributes getAttributes() {
        return attributes;
    }

    public long getOffset() {
        return offset;
    }

    @Override
    public Object clone() {
        FieldInfo newFileInfo = new FieldInfo();
        newFileInfo.accessFlags = accessFlags;
        newFileInfo.nameIndex = nameIndex;
        newFileInfo.descriptorIndex = descriptorIndex;
        newFileInfo.name = name;
        newFileInfo.descriptor = descriptor;
        newFileInfo.attributes = attributes;
        newFileInfo.offset = offset;
        return newFileInfo;
    }

    public void setType(int index, ConstantPool constantPool) {
        descriptorIndex = index;
        descriptor = (ConstantUtf8Info) constantPool.getItem(descriptorIndex);
        descriptor.references++;
    }
}
