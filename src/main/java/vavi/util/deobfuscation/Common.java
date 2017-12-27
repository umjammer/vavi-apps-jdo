
package vavi.util.deobfuscation;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


// important enums
enum AccessFlags {
    ACC_PUBLIC(0x0001),
    ACC_FINAL(0x0010),
    ACC_SUPER(0x0020),
    ACC_INTERFACE(0x0200),
    ACC_ABSTRACT(0x0400),
    ACC_UNKNOWN(-1);
    int value;

    AccessFlags(int value) {
        this.value = value;
    }

    static AccessFlags valueOf(int value) {
        for (AccessFlags accessFlag : values()) {
            if (accessFlag.value == value) {
                return accessFlag;
            }
        }
        return ACC_UNKNOWN;
//        throw new IllegalArgumentException(String.valueOf(value));
    }
}

enum FieldAccessFlags {
    /** Declared public; may be accessed from outside its package. */
    ACC_PUBLIC(0x0001),
    /** Declared private; usable only within the defining class. */
    ACC_PRIVATE(0x0002),
    /** Declared protected; may be accessed within subclasses. */
    ACC_PROTECTED(0x0004),
    /** Declared static. */
    ACC_STATIC(0x0008),
    /** Declared final; no further assignment after initialization. */
    ACC_FINAL(0x0010),
    /** Declared volatile; cannot be cached. */
    ACC_VOLATILE(0x0040),
    /** Declared transient; not written or read by a persistent object manager. */
    ACC_TRANSIENT(0x0080);
    int value;

    FieldAccessFlags(int value) {
        this.value = value;
    }

    static FieldAccessFlags valueOf(int value) {
        for (FieldAccessFlags accessFlag : values()) {
            if (accessFlag.value == value) {
                return accessFlag;
            }
        }
        throw new IllegalArgumentException(String.valueOf(value));
    }
}

enum ConstantPoolInfoTag {
    ConstantClass(7),
    ConstantFieldref(9),
    ConstantMethodref(10),
    ConstantInterfaceMethodref(11),
    ConstantString(8),
    ConstantInteger(3),
    ConstantFloat(4),
    ConstantLong(5),
    ConstantDouble(6),
    ConstantNameAndType(12),
    ConstantUtf8(1),
    ConstantUnknown(-1);
    int value;

    ConstantPoolInfoTag(int value) {
        this.value = value;
    }

    static ConstantPoolInfoTag valueOf(int value) {
        for (ConstantPoolInfoTag accessFlag : values()) {
            if (accessFlag.value == value) {
                return accessFlag;
            }
        }
        return ConstantUnknown;
//        throw new IllegalArgumentException(Integer.toHexString(value));
    }
}

enum AttributeType {
    ConstantValue,
    Code,
    Exceptions,
    InnerClasses,
    Synthetic,
    SourceFile,
    LineNumberTable,
    LocalVariableTable,
    Deprecated,
    StackMapTable
}

abstract class ConstantPoolInfo {
    public int tag;
    public int references;
    public abstract void read(int tag, DataInput Reader) throws IOException;
    public abstract boolean resolve(List<?> FItems) throws IOException;
}

abstract class ConstantPoolMethodInfo extends ConstantPoolInfo {
    public ConstantClassInfo parentClass;
    public ConstantNameAndTypeInfo nameAndType;
    // private int classIndex;
    // private int nameAndTypeIndex;

    public abstract void setNameAndType(int index, ConstantPool constantPool);
    public abstract void setParent(int index, ConstantPool constantPool);
}

abstract class ConstantPoolVariableInfo extends ConstantPoolInfo {
    public Object value;
}

class ConstantClassInfo extends ConstantPoolInfo {
    public int nameIndex;
    public String name;

    public ConstantClassInfo() {
        name = "";
        nameIndex = 0;
        tag = ConstantPoolInfoTag.ConstantClass.value;
        references = 0;
    }

    @Override
    public void read(int tag, DataInput reader) throws IOException {
            this.tag = tag;
            nameIndex = Common.readShort(reader);
            nameIndex--;
    }

    public void write(DataOutput writer) throws IOException {
        // write the tag
        Common.writeByte(writer, tag);
        Common.writeShort(writer, nameIndex + 1);
    }

    @Override
    public boolean resolve(List<?> items) throws IOException {
        // use the index into the constant pool table
        // to find our UTF8 encoded class or interface name
        if (nameIndex < items.size()) {
            Object o = items.get(nameIndex);
            if (o instanceof ConstantUtf8Info) {
                name = new String(((ConstantUtf8Info) o).bytes, "UTF-8");
                ((ConstantPoolInfo) o).references++;

                return true;
            }
        }

        return false;
    }

    public void setName(int index, ConstantPool constantPool) {
        nameIndex = index;
        name = ((ConstantUtf8Info) constantPool.getItem(index)).value;
        references++;
    }
}

class ConstantFieldrefInfo extends ConstantPoolMethodInfo {
    private int classIndex;
    private int nameAndTypeIndex;

     @Override
     public void read(int tag, DataInput reader) throws IOException {
            this.tag = tag;
            classIndex = Common.readShort(reader);
            nameAndTypeIndex = Common.readShort(reader);
            classIndex--;
            nameAndTypeIndex--;
    }

    public void write(DataOutput writer) throws IOException {
        // write the tag
        Common.writeByte(writer, tag);
        Common.writeShort(writer, classIndex + 1);
        Common.writeShort(writer, nameAndTypeIndex + 1);
    }

    @Override
    public boolean resolve(List<?> items) {
        // use the index into the constant pool table
        // to find our UTF8 encoded class or interface name
        if (classIndex < items.size() && nameAndTypeIndex < items.size()) {
            Object o = items.get(classIndex);
            if (o instanceof ConstantClassInfo) {
                parentClass = (ConstantClassInfo) o;
                ((ConstantPoolInfo) o).references++;
            }

            o = items.get(nameAndTypeIndex);
            if (o instanceof ConstantNameAndTypeInfo) {
                nameAndType = (ConstantNameAndTypeInfo) o;
                ((ConstantPoolInfo) o).references++;
            }

            return true;
        }

        return false;
    }

    @Override
    public void setNameAndType(int index, ConstantPool constantPool) {
        nameAndTypeIndex = index;
        nameAndType = (ConstantNameAndTypeInfo) constantPool.getItem(index);
        nameAndType.references++;
    }

    @Override
    public void setParent(int index, ConstantPool constantPool) {
        classIndex = index;
        parentClass = (ConstantClassInfo) constantPool.getItem(index);
        parentClass.references++;
    }
}

class ConstantMethodrefInfo extends ConstantPoolMethodInfo {
    private int classIndex;
    private int nameAndTypeIndex;

    @Override
    public void read(int tag, DataInput Reader) throws IOException {
            this.tag = tag;
            classIndex = Common.readShort(Reader);
            nameAndTypeIndex = Common.readShort(Reader);
            classIndex--;
            nameAndTypeIndex--;
    }

    public void write(DataOutput writer) throws IOException {
        // write the tag
        Common.writeByte(writer, tag);
        Common.writeShort(writer, classIndex + 1);
        Common.writeShort(writer, nameAndTypeIndex + 1);
    }

    @Override
    public boolean resolve(List<?> items) {
        // use the index into the constant pool table
        // to find our UTF8 encoded class or interface name
        if (classIndex < items.size() && nameAndTypeIndex < items.size()) {
            Object o = items.get(classIndex);
            if (o instanceof ConstantClassInfo) {
                parentClass = (ConstantClassInfo) o;
                ((ConstantPoolInfo) o).references++;
            }

            o = items.get(nameAndTypeIndex);
            if (o instanceof ConstantNameAndTypeInfo) {
                nameAndType = (ConstantNameAndTypeInfo) o;
                ((ConstantPoolInfo) o).references++;
            }

            return true;
        }

        return false;
    }

    @Override
    public void setNameAndType(int Index, ConstantPool constantPool) {
        nameAndTypeIndex = Index;
        nameAndType = (ConstantNameAndTypeInfo) constantPool.getItem(Index);
        nameAndType.references++;
    }

    @Override
    public void setParent(int index, ConstantPool constantPool) {
        classIndex = index;
        parentClass = (ConstantClassInfo) constantPool.getItem(index);
        parentClass.references++;
    }
}

class ConstantInterfaceMethodrefInfo extends ConstantPoolMethodInfo {
    private int classIndex;
    private int nameAndTypeIndex;

    @Override
    public void read(int tag, DataInput Reader) throws IOException {
            this.tag = tag;
            classIndex = Common.readShort(Reader);
            nameAndTypeIndex = Common.readShort(Reader);
            classIndex--;
            nameAndTypeIndex--;
    }

    public void write(DataOutput writer) throws IOException {
        // write the tag
        Common.writeByte(writer, tag);
        Common.writeShort(writer, classIndex + 1);
        Common.writeShort(writer, nameAndTypeIndex + 1);
    }

    @Override
    public boolean resolve(List<?> items) {
        // use the index into the constant pool table
        // to find our UTF8 encoded class or interface name
        if (classIndex <= items.size() && nameAndTypeIndex <= items.size()) {
            Object o = items.get(classIndex);
            if (o instanceof ConstantClassInfo) {
                parentClass = (ConstantClassInfo) o;
                ((ConstantPoolInfo) o).references++;
            }

            o = items.get(nameAndTypeIndex);
            if (o instanceof ConstantNameAndTypeInfo) {
                nameAndType = (ConstantNameAndTypeInfo) o;
                ((ConstantPoolInfo) o).references++;
            }

            return true;
        }

        return false;
    }

    @Override
    public void setNameAndType(int index, ConstantPool constantPool) {
        nameAndTypeIndex = index;
    }

    @Override
    public void setParent(int index, ConstantPool constantPool) {
        classIndex = index;
    }
}

class ConstantStringInfo extends ConstantPoolVariableInfo {
    private int nameIndex;

    public ConstantStringInfo() {
        nameIndex = 0;
        value = "";
    }

    @Override
    public void read(int tag, DataInput reader) throws IOException {
            this.tag = tag;
            nameIndex = Common.readShort(reader);
            nameIndex--;
    }

    public void write(DataOutput writer) throws IOException {
        // write the tag
        Common.writeByte(writer, tag);
        Common.writeShort(writer, nameIndex + 1);
    }

    @Override
    public boolean resolve(List<?> items) throws IOException {
        // use the index into the constant pool table
        // to find our UTF8 encoded class or interface name
        if (nameIndex < items.size()) {
            Object o = items.get(nameIndex);
            if (o instanceof ConstantUtf8Info) {
                value = new String(((ConstantUtf8Info) o).bytes, "UTF-8");
                ((ConstantPoolInfo) o).references++;

                return true;
            }
        }

        return false;
    }
}

class ConstantIntegerInfo extends ConstantPoolVariableInfo {
    private int bytes;

    @Override
    public void read(int tag, DataInput reader) throws IOException {
            this.tag = tag;
            bytes = Common.readInt(reader);
    }

    public void write(DataOutput writer) throws IOException {
        // write the tag
        Common.writeByte(writer, tag);
        Common.writeInt(writer, bytes);
    }

    @Override
    public boolean resolve(List<?> items) {
        value = (int) bytes;
        return true;
    }
}

class ConstantFloatInfo extends ConstantPoolVariableInfo {
    private int bytes;

    @Override
    public void read(int tag, DataInput reader) throws IOException {
            this.tag = tag;
            bytes = Common.readInt(reader);
    }

    public void write(DataOutput writer) throws IOException {
        // write the tag
        Common.writeByte(writer, tag);
        Common.writeInt(writer, bytes);
    }

    @Override
    public boolean resolve(List<?> items) {
        value = Float.intBitsToFloat(bytes);
        return true;
    }
}

class ConstantLongInfo extends ConstantPoolVariableInfo {
    private int highBytes;
    private int lowBytes;

    @Override
    public void read(int tag, DataInput Reader) throws IOException {
            this.tag = tag;
            highBytes = Common.readInt(Reader);
            lowBytes = Common.readInt(Reader);
            value = ((long) highBytes << 32) + lowBytes;
    }

    public void write(DataOutput writer) throws IOException {
        // write the tag
        Common.writeByte(writer, tag);
        Common.writeInt(writer, highBytes);
        Common.writeInt(writer, lowBytes);
    }

    @Override
    public boolean resolve(List<?> items) {
        return true;
    }
}

class ConstantDoubleInfo extends ConstantPoolVariableInfo {
    private int highBytes;
    private int lowBytes;

    @Override
    public void read(int tag, DataInput reader) throws IOException {
            this.tag = tag;
            highBytes = Common.readInt(reader);
            lowBytes = Common.readInt(reader);
            value = "NOT_IMPLEMENTED";
    }

    public void write(DataOutput writer) throws IOException {
        // write the tag
        Common.writeByte(writer, tag);
        Common.writeInt(writer, highBytes);
        Common.writeInt(writer, lowBytes);
    }

    @Override
    public boolean resolve(List<?> items) {
        value = Double.longBitsToDouble(((long) highBytes << 32) + lowBytes);
        return true;
    }
}

class ConstantNameAndTypeInfo extends ConstantPoolInfo {
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

class ConstantUtf8Info extends ConstantPoolInfo {
    public int length;
    public byte[] bytes;
    public String value;

    public ConstantUtf8Info() {
        bytes = null;
        length = 0;
        tag = ConstantPoolInfoTag.ConstantUtf8.value;
        references = 0;
    }

    public ConstantUtf8Info(String text) {
        try {
            tag = ConstantPoolInfoTag.ConstantUtf8.value;
            bytes = text.getBytes("UTF-8");
            length = bytes.length;
            value = new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
System.err.println(e);
            assert false;
        }
    }

    public void setName(String text) {
        try {
            bytes = text.getBytes("UTF-8");
            length = bytes.length;
            value = new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
System.err.println(e);
            assert false;
        }
    }

    @Override
    public void read(int tag, DataInput reader) throws IOException {
            this.tag = tag;
            length = Common.readShort(reader);
            bytes = new byte[length];
            reader.readFully(bytes, 0, length);
            Common.position += length;

            value = new String(bytes, "UTF-8");
    }

    public void write(DataOutput writer) throws IOException {
        // write the tag
        Common.writeByte(writer, tag);
        Common.writeShort(writer, length);
        writer.write(bytes);
        Common.position += length;
    }

    @Override
    public boolean resolve(List<?> items) {
        return true;
    }
}

class FieldInfo {
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

class MethodInfo {
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

abstract class AttributeInfo {
//     int attributeNameIndex;
//     ConstantUtf8Info attributeName;
//     int attributeLength;
//     byte[] bytes;

    public abstract void write(DataOutput writer) throws IOException;
}

class UnknownAttributeInfo extends AttributeInfo {
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

class ConstantValueAttributeInfo extends AttributeInfo {
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

class CodeAttributeInfo extends AttributeInfo {
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

    // stuff i want
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

class ExceptionsAttributeInfo extends AttributeInfo {
    int attributeNameIndex;
    ConstantUtf8Info attributeName;
    int attributeLength;
    int numberOfExceptions;
    TException[] exceptionTable;

    public ExceptionsAttributeInfo(int nameIndex, DataInput reader, ConstantPool constantPool) {
        attributeNameIndex = 0;
        attributeName = null;
        attributeLength = 0;
        numberOfExceptions = 0;
        exceptionTable = null;

        try {
            attributeNameIndex = nameIndex;
            attributeLength = Common.readInt(reader);

            numberOfExceptions = Common.readShort(reader);
            exceptionTable = new TException[numberOfExceptions];
            // fucking nested arrays! ;/
            for (int i = 0; i < numberOfExceptions; i++) {
                exceptionTable[i] = new TException(reader, constantPool);
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

        Common.writeShort(writer, numberOfExceptions);

        for (int i = 0; i < numberOfExceptions; i++) {
            exceptionTable[i].write(writer);
        }
    }
}

class InnerClassesAttributeInfo extends AttributeInfo {
    int attributeNameIndex;
    ConstantUtf8Info attributeName;
    int attributeLength;
    int numberOfClasses;
    TClasses[] classes;

    public InnerClassesAttributeInfo(int nameIndex, DataInput reader, ConstantPool constantPool) {
        attributeNameIndex = 0;
        attributeName = null;
        attributeLength = 0;
        numberOfClasses = 0;
        classes = null;

        try {
            attributeNameIndex = nameIndex;
            attributeLength = Common.readInt(reader);

            numberOfClasses = Common.readShort(reader);
            classes = new TClasses[numberOfClasses];
            // fucking nested arrays! ;/
            for (int i = 0; i < numberOfClasses; i++) {
                classes[i] = new TClasses(reader, constantPool);
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

        Common.writeShort(writer, numberOfClasses);
        for (int i = 0; i < numberOfClasses; i++) {
            classes[i].write(writer);
        }
    }
}

class SyntheticAttributeInfo extends AttributeInfo {
    int attributeNameIndex;
    ConstantUtf8Info attributeName;
    int attributeLength;

    public SyntheticAttributeInfo(int nameIndex, DataInput reader, ConstantPool constantPool) {
        attributeNameIndex = 0;
        attributeName = null;
        attributeLength = 0;

        try {
            attributeNameIndex = nameIndex;
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

class SourceFileAttributeInfo extends AttributeInfo {
    int attributeNameIndex;
    ConstantUtf8Info attributeName;
    int attributeLength;
    int sourceFileIndex;
    ConstantUtf8Info sourceFile;

    public SourceFileAttributeInfo(int nameIndex, DataInput reader, ConstantPool constantPool) {
        attributeNameIndex = 0;
        attributeName = null;
        attributeLength = 0;
        sourceFileIndex = 0;
        sourceFile = null;

        try {
            attributeNameIndex = nameIndex;
            attributeLength = Common.readInt(reader);
            sourceFileIndex = Common.readShort(reader);
            sourceFileIndex--;
            // resolve references
            attributeName = (ConstantUtf8Info) constantPool.getItem(attributeNameIndex);
            attributeName.references++;
            sourceFile = (ConstantUtf8Info) constantPool.getItem(attributeNameIndex);
            sourceFile.references++;
        } catch (Exception e) {
e.printStackTrace(System.err);
            // do nothing
        }
    }

    @Override
    public void write(DataOutput writer) throws IOException {
        Common.writeShort(writer, attributeNameIndex + 1);
        Common.writeInt(writer, attributeLength);
        Common.writeShort(writer, sourceFileIndex + 1);
    }
}

class LineNumberAttributeInfo extends AttributeInfo {
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

class LocalVariablesAttributeInfo extends AttributeInfo {
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

class DeprecatedAttributeInfo extends AttributeInfo {
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

// the inner attribute classes
class TLocalVariableTable {
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

class TLineNumberTable {
    int startPC;
    int lineNumber;

    public TLineNumberTable(DataInput reader) {
        lineNumber = 0;
        startPC = 0;

        try {
            startPC = Common.readShort(reader);
            lineNumber = Common.readShort(reader);
        } catch (Exception e) {
e.printStackTrace(System.err);
            // do nothing
        }
    }

    public void write(DataOutput writer) throws IOException {
        Common.writeShort(writer, startPC);
        Common.writeShort(writer, lineNumber);
    }
}

class TClasses {
    int innerClassInfoIndex;
    ConstantClassInfo innerClassInfo;
    int outerClassInfoIndex;
    ConstantClassInfo outerClassInfo;
    int innerNameIndex;
    ConstantUtf8Info innerName;
    int innerClassAccessFlags;

    public TClasses(DataInput reader, ConstantPool constantPool) {
        innerClassInfoIndex = 0;
        innerClassInfo = null;
        outerClassInfoIndex = 0;

        outerClassInfo = null;
        innerNameIndex = 0;

        innerName = null;
        innerClassAccessFlags = 0;


        try {
            innerClassInfoIndex = Common.readShort(reader);
            innerClassInfoIndex--;
            outerClassInfoIndex = Common.readShort(reader);
            outerClassInfoIndex--;
            innerNameIndex = Common.readShort(reader);
            innerNameIndex--;
            innerClassAccessFlags = Common.readShort(reader);

            // resolve references
            if (innerNameIndex >= 0) {
                innerName = (ConstantUtf8Info) constantPool.getItem(innerNameIndex);
                innerName.references++;
            }
            if (innerNameIndex >= 0) {
                innerClassInfo = (ConstantClassInfo) constantPool.getItem(innerClassInfoIndex);
                innerClassInfo.references++;
            }
            if (innerNameIndex >= 0) {
                outerClassInfo = (ConstantClassInfo) constantPool.getItem(outerClassInfoIndex);
                outerClassInfo.references++;
            }
        } catch (Exception e) {
e.printStackTrace(System.err);
            // do nothing
        }
    }

    public void write(DataOutput writer) throws IOException {
        Common.writeShort(writer, innerClassInfoIndex + 1);
        Common.writeShort(writer, outerClassInfoIndex + 1);
        Common.writeShort(writer, innerNameIndex + 1);
        Common.writeShort(writer, innerClassAccessFlags);
    }
}

class TException {
    int exceptionIndex;

    ConstantClassInfo Exception;

    public TException(DataInput reader, ConstantPool constantPool) {
        exceptionIndex = 0;

        try {
            exceptionIndex = Common.readShort(reader);
            exceptionIndex--;
            // resolve references
            Exception = (ConstantClassInfo) constantPool.getItem(exceptionIndex);
        } catch (Exception e) {
e.printStackTrace(System.err);
            Exception = null;
        }
    }

    public void write(DataOutput writer) throws IOException {
        Common.writeShort(writer, exceptionIndex + 1);
    }
}

class ExceptionTable {
    int startPC;
    int endPC;
    int handlerPC;
    int catchType;

    ConstantClassInfo catch_;

    public ExceptionTable(DataInput reader, ConstantPool constantPool) {
        startPC = 0;
        endPC = 0;
        handlerPC = 0;
        catchType = 0;
        catch_ = null;

        try {
            startPC = Common.readShort(reader);
            startPC--;
            endPC = Common.readShort(reader);
            endPC--;
            handlerPC = Common.readShort(reader);
            handlerPC--;
            catchType = Common.readShort(reader);
            catchType--;

            if (catchType >= 0) {
                catch_ = (ConstantClassInfo) constantPool.getItem(catchType);
            }
        } catch (Exception e) {
e.printStackTrace(System.err);
        }
    }

    public void write(DataOutput writer) throws IOException {
        Common.writeShort(writer, startPC + 1);
        Common.writeShort(writer, endPC + 1);
        Common.writeShort(writer, handlerPC + 1);
        Common.writeShort(writer, catchType + 1);
    }
}

// //
// INTERFACE STRUCTURES //
// //

class InterfaceInfo {
    private int value;
    private ConstantClassInfo interface_;

    public InterfaceInfo(DataInput Reader, ConstantPool ConstantPool) {
        try {
            value = Common.readShort(Reader);
            value--;

            interface_ = (ConstantClassInfo) ConstantPool.getItem(value);
        } catch (Exception e) {
e.printStackTrace(System.err);
            value = 0;
            interface_ = null;
        }
    }

    public void write(DataOutput writer) {
        try {
            Common.writeShort(writer, value + 1);
        } catch (Exception e) {
        }
    }

    public int getValue() {
        return value;
    }

    public ConstantClassInfo getInterface() {
        return interface_;
    }

    public String getName() {
        if (interface_ != null) {
            return interface_.name;
        }

        return "";
    }

    public void setName(String newName) {

    }
}

// //
// COMMON FUNCTIONS //
// //

class RenameData {
    String type;
    String name;

    public RenameData(String Type, String Name) {
        type = Type;
        name = Name;
    }

    public String[] getData() {
        String[] s = new String[2];
        s[0] = type;
        s[1] = name;

        return s;
    }

    public String getFieldType() {
        return type;
    }

    public void setFieldType(String value) {
        type = value;
    }

    public String getFieldName() {
        return name;
    }

    public void setFieldName(String value) {
        name = value;
    }
}

class RenameDatabase {
    private Map<String, List<RenameData>> renameMethods = null;
    private Map<String, List<RenameData>> renameFields = null;
    private Map<String, String> renameClass = null;

    public RenameDatabase() {
        renameMethods = new HashMap<>();
        renameFields = new HashMap<>();
        renameClass = new HashMap<>();
    }

    public void addRename(Map<String, List<RenameData>> destTable, String className, String oldDescriptor, String oldName, String newDescriptor, String newName) {
        List<RenameData> al = destTable.get(className);

        if (al == null) {
            al = new ArrayList<>();
            destTable.put(className, al);
        } else {
            // make sure it doesnt already exist
            for (int i = 0; i < al.size(); i += 2) {
                RenameData rd = al.get(i);

                if (rd.getFieldName() == oldName && rd.getFieldType() == oldDescriptor) {
                    // if it does, overwrite it, don't add in a new one
                    rd.setFieldName(newName);
                    return;
                }
            }
        }

        al.add(new RenameData(oldDescriptor, oldName));
        al.add(new RenameData(newDescriptor, newName));
    }

    public RenameData getRenameInfo(Map<String, List<RenameData>> destTable, String className, String oldDescriptor, String oldName) {
        List<RenameData> al = destTable.get(className);

        if (al == null)
            return null;

        for (int i = 0; i < al.size(); i += 2) {
            RenameData rd = al.get(i);

            if (rd.getFieldName() == oldName && rd.getFieldType() == oldDescriptor) {
                return al.get(i + 1);
            }
        }

        return null;
    }

    public void addRenameMethod(String className, String oldDescriptor, String oldName, String newDescriptor, String newName) {
        addRename(renameMethods, className, oldDescriptor, oldName, newDescriptor, newName);
    }

    public void addRenameField(String className, String oldDescriptor, String oldName, String newDescriptor, String newName) {
        addRename(renameFields, className, oldDescriptor, oldName, newDescriptor, newName);
    }

    public RenameData getNewMethodInfo(String className, String oldDescriptor, String oldName) {
        // searches for a matching method in the methodlist
        return getRenameInfo(renameMethods, className, oldDescriptor, oldName);
    }

    public RenameData getNewFieldInfo(String className, String oldDescriptor, String oldName) {
        // searches for a matching method in the methodlist
        return getRenameInfo(renameFields, className, oldDescriptor, oldName);
    }

    public void addRenameClass(String oldClassName, String newClassName) {
        renameClass.put(oldClassName, newClassName);
    }

    public String getNewClassName(String oldClassName) {
        return renameClass.get(oldClassName);
    }

    public String getNewClassNameOnly(String oldClassName) {
        String temp = getNewClassName(oldClassName);

        if (temp == null)
            return null;

        String[] strspl = temp.split(":");

        if (strspl.length > 0) {
            return strspl[0].trim();
        }

        return null;
    }

    // serialize this to read in .xml file ?
//     public boolean ReadFromFile(String FileName) {
//         return false;
//     }

}

class Common {
    static long position = 0;

    public static String getClassName(String fullName) {
        // gets the class name from a class path
        if (fullName.indexOf("/") > 0)
            return fullName.substring(fullName.lastIndexOf('/') + 1, fullName.length() - fullName.lastIndexOf('/') - 1);
        else
            return fullName;
    }

    public static String getClassPath(String FullName) {
        // gets the class name from a class path
        return FullName.substring(0, FullName.lastIndexOf('/') + 1);
    }

    public static String newClassName(String originalClassName, String newName) {
        newName = Common.getClassName(newName);
        // new name should be the short name
        // original class name should be original long name
        if (originalClassName.lastIndexOf('/') > 0) {
//            String old_name = OriginalClassName.Substring(OriginalClassName.lastIndexOf('/') + 1, OriginalClassName.length() - OriginalClassName.lastIndexOf('/') - 1);
            originalClassName = originalClassName.substring(0, originalClassName.lastIndexOf('/')) + originalClassName.substring(originalClassName.length() - originalClassName.lastIndexOf('/'));
//            OriginalClassName += NewName + old_name;
            originalClassName += newName;

            return originalClassName;
        }

        // return NewName + OriginalClassName;
        return newName;
    }

    public static String FixDescriptor(String descriptor, String oldClassName, String newClassName) {
        return descriptor.replace("L" + oldClassName + ";", "L" + newClassName + ";");
    }

//    private static int SwapBytes(int value) {
//        int a = (value >>> 8);
//        int b = (value << 8);
//
//        return (a | b);
//    }

    public static int readShort(DataInput reader) throws IOException {
        if (reader == null) {
            return 0;
        }

        int val = reader.readUnsignedShort();
        position += 2;
//        val = SwapBytes(val);

        return val;
    }

    public static void writeShort(DataOutput writer, int data) throws IOException {
        if (writer == null) {
            return;
        }

        // convert the data from small endian to big endian
//        Data = SwapBytes(Data);

        writer.writeShort(data);
        position += 2;
    }

    public static int readInt(DataInput reader) throws IOException {
        if (reader == null) {
            return 0;
        }

        // get the value, and then change it from big endian to small endian
        int val = reader.readInt();
        position += 4;
//        int temp = val >>> 16;
//        temp = SwapBytes(temp);
//        val = val & 0x0FFFF;
//        val = SwapBytes(val);
//        val = (val << 16) | temp;

        return val;
    }

    public static void writeInt(DataOutput writer, int data) throws IOException {
        if (writer == null) {
            return;
        }

        // convert the data from small endian to big endian
//        int temp = Data >>> 16;
//        temp = SwapBytes(temp);
//        Data = Data & 0x0FFFF;
//        Data = SwapBytes(Data);
//        Data = (Data << 16) | temp;

        writer.writeInt(data);
        position += 4;
    }

    public static int readByte(DataInput reader) throws IOException {
        if (reader == null) {
            return 0;
        }

        position += 1;
        return reader.readUnsignedByte();
    }

    public static void writeByte(DataOutput writer, int data) throws IOException {
        if (writer == null) {
            return;
        }

        writer.writeByte(data);
        position += 1;
    }
}
