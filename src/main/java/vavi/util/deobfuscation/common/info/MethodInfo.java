/*
 * https://github.com/fileoffset/JDO
 */

package vavi.util.deobfuscation.common.info;

import java.io.DataInput;
import java.io.DataOutput;

import vavi.util.deobfuscation.common.Common;
import vavi.util.deobfuscation.common.attrubuteinfo.CodeAttributeInfo;
import vavi.util.deobfuscation.common.constantpoolinfo.ConstantUtf8Info;
import vavi.util.deobfuscation.objects.Attributes;
import vavi.util.deobfuscation.objects.ConstantPool;


public class MethodInfo {
    int accessFlags;

    int nameIndex;

    int descriptorIndex;

    ConstantUtf8Info name;

    ConstantUtf8Info descriptor;

    Attributes attributes;

    private MethodInfo() {
    }

    public MethodInfo(DataInput reader, ConstantPool constantPool) {
        this.accessFlags = 0;
        this.nameIndex = 0;
        this.descriptorIndex = 0;
        this.name = null;
        this.descriptor = null;
        this.attributes = null;

        try {
            accessFlags = Common.readShort(reader);
            nameIndex = Common.readShort(reader);
            nameIndex--;
            descriptorIndex = Common.readShort(reader);
            System.err.printf("FDescriptorIndex: %04x\n", descriptorIndex);
            descriptorIndex--;
            // resolve the references
            descriptor = (ConstantUtf8Info) constantPool.getItem(descriptorIndex);
            name = (ConstantUtf8Info) constantPool.getItem(nameIndex);
            descriptor.references++;
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

    public void write(DataOutput writer) {
        try {
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

    public ConstantUtf8Info getName() {
        return name;
    }

    public String getDescriptor() {
        return descriptor.value;
    }

    public Attributes getAttributes() {
        return attributes;
    }

    public long getOffset() {
        if (getAttributes() != null && getAttributes().getItems().size() > 0) {
            try {
                return ((CodeAttributeInfo) getAttributes().getItems().get(0)).getCodeOffset();
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }

        return 0;
    }

    public int getNameIndex() {
        return nameIndex;
    }

    public Object clone() {
        MethodInfo newMethodInfo = new MethodInfo();
        newMethodInfo.accessFlags = accessFlags;
        newMethodInfo.nameIndex = nameIndex;
        newMethodInfo.descriptorIndex = descriptorIndex;
        newMethodInfo.name = name;
        newMethodInfo.descriptor = descriptor;
        newMethodInfo.attributes = attributes;
        return newMethodInfo;
    }

    public void setType(int index, ConstantPool constantPool) {
        descriptorIndex = index;
        descriptor = (ConstantUtf8Info) constantPool.getItem(descriptorIndex);
        descriptor.references++;
    }
}
