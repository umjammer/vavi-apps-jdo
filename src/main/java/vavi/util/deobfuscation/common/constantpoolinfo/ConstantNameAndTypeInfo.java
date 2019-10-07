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
import vavi.util.deobfuscation.objects.ConstantPool;


public class ConstantNameAndTypeInfo extends ConstantPoolInfo {
    private int nameIndex;

    private int descriptorIndex;

    public String name;

    public String descriptor;

    public ConstantNameAndTypeInfo() {
        tag = ConstantPoolInfoTag.ConstantNameAndType.value;
        nameIndex = 0;
        descriptorIndex = 0;
        name = "";
        descriptor = "";
    }

    public ConstantNameAndTypeInfo(int indexName, int indexType, ConstantPool constantPool) {
        try {
            tag = ConstantPoolInfoTag.ConstantNameAndType.value;
            nameIndex = indexName;
            descriptorIndex = indexType;
            name = new String(((ConstantUtf8Info) constantPool.getItem(indexName)).bytes, "UTF-8");
            constantPool.getItem(indexName).references++;
            descriptor = new String(((ConstantUtf8Info) constantPool.getItem(indexType)).bytes, "UTF-8");
            constantPool.getItem(indexType).references++;
        } catch (UnsupportedEncodingException e) {
            System.err.println(e);
            assert false;
        }
    }

    // where index instanceof a valid index into the constant pool table
    public void setName(int index, ConstantPool constantPool) {
        try {
            nameIndex = index;
            name = new String(((ConstantUtf8Info) constantPool.getItem(index)).bytes, "UTF-8");
            constantPool.getItem(index).references++;
        } catch (UnsupportedEncodingException e) {
            System.err.println(e);
            assert false;
        }
    }

    // where index instanceof a valid index into the constant pool table
    public void setType(int index, ConstantPool constantPool) {
        try {
            descriptorIndex = index;
            descriptor = new String(((ConstantUtf8Info) constantPool.getItem(index)).bytes, "UTF-8");
            constantPool.getItem(index).references++;
        } catch (UnsupportedEncodingException e) {
            System.err.println(e);
            assert false;
        }
    }

    @Override
    public void read(int tag, DataInput reader) throws IOException {
        this.tag = tag;
        nameIndex = Common.readShort(reader);
        nameIndex--;
        descriptorIndex = Common.readShort(reader);
        descriptorIndex--;
    }

    @Override
    public void write(DataOutput writer) throws IOException {
        // write the tag
        Common.writeByte(writer, tag);
        Common.writeShort(writer, nameIndex + 1);
        Common.writeShort(writer, descriptorIndex + 1);
    }

    @Override
    public boolean resolve(List<?> items) {
        try {
            name = null;

            if (nameIndex < items.size()) {
                Object o = items.get(nameIndex);
                if (o instanceof ConstantUtf8Info) {
                    name = new String(((ConstantUtf8Info) o).bytes, "UTF-8");
                    ((ConstantPoolInfo) o).references++;
                }
            }

        } catch (Exception e) {
            e.printStackTrace(System.err);
            if (name == null)
                name = "Error retrieving Name!";
        }

        try {
            descriptor = null;

            if (descriptorIndex < items.size()) {
                Object o = items.get(descriptorIndex);
                if (o instanceof ConstantUtf8Info) {
                    descriptor = new String(((ConstantUtf8Info) o).bytes, "UTF-8");
                    ((ConstantPoolInfo) o).references++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            if (descriptor == null)
                descriptor = "Error retrieving Descriptor!";
        }

        return true;
    }

    public int getNameIndex() {
        return nameIndex;
    }

    public int getTypeIndex() {
        return descriptorIndex;
    }
}
