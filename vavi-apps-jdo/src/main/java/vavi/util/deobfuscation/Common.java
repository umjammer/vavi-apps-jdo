
package vavi.util.deobfuscation;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Hashtable;


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
    // Declared public; may be accessed from outside its package.
    ACC_PUBLIC(0x0001),
    // Declared private; usable only within the defining class.
    ACC_PRIVATE(0x0002),
    // Declared protected; may be accessed within subclasses.
    ACC_PROTECTED(0x0004),
    // Declared static.
    ACC_STATIC(0x0008),
    // Declared final; no further assignment after initialization.
    ACC_FINAL(0x0010),
    // Declared volatile; cannot be cached.
    ACC_VOLATILE(0x0040),
    // Declared transient; not written or read by a persistent object manager.
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

//
// CONSTANT POOL STRUCTURES
//

abstract class ConstantPoolInfo {
    public int tag;

    public int references;

    public abstract void read(int tag, DataInput Reader) throws IOException;

    public abstract boolean resolve(ArrayList<?> FItems) throws IOException;
}

abstract class ConstantPoolMethodInfo extends ConstantPoolInfo {
    public ConstantClassInfo ParentClass;

    public ConstantNameAndTypeInfo NameAndType;

    // private int ClassIndex;
    // private int NameAndTypeIndex;

    public abstract void SetNameAndType(int Index, TConstantPool ConstantPool);

    public abstract void SetParent(int Index, TConstantPool ConstantPool);
}

abstract class ConstantPoolVariableInfo extends ConstantPoolInfo {
    public Object Value;
}

class ConstantClassInfo extends ConstantPoolInfo {
    public int NameIndex;

    public String Name;

    public ConstantClassInfo() {
        Name = "";
        NameIndex = 0;
        tag = ConstantPoolInfoTag.ConstantClass.value;
        references = 0;
    }

    @Override
    public void read(int tag, DataInput Reader) throws IOException {
            this.tag = tag;
            NameIndex = Common.readShort(Reader);
            NameIndex--;
    }

    public void write(DataOutput writer) throws IOException {
        // write the tag
        Common.writeByte(writer, tag);
        Common.writeShort(writer, NameIndex + 1);
    }

    @Override
    public boolean resolve(ArrayList<?> FItems) throws IOException {
        // use the index into the constant pool table
        // to find our UTF8 encoded class or interface name
        if (NameIndex < FItems.size()) {
            Object o = FItems.get(NameIndex);
            if (o instanceof ConstantUtf8Info) {
                Name = new String(((ConstantUtf8Info) o).Bytes, "UTF-8");
                ((ConstantPoolInfo) o).references++;

                return true;
            }
        }

        return false;
    }

    public void SetName(int Index, TConstantPool ConstantPool) {
        NameIndex = Index;
        Name = ((ConstantUtf8Info) ConstantPool.getItem(Index)).Value;
        references++;
    }
}

class ConstantFieldrefInfo extends ConstantPoolMethodInfo {
    private int ClassIndex;

    private int NameAndTypeIndex;

     @Override
     public void read(int tag, DataInput Reader) throws IOException {
            this.tag = tag;
            ClassIndex = Common.readShort(Reader);
            NameAndTypeIndex = Common.readShort(Reader);
            ClassIndex--;
            NameAndTypeIndex--;
    }

    public void write(DataOutput writer) throws IOException {
        // write the tag
        Common.writeByte(writer, tag);
        Common.writeShort(writer, ClassIndex + 1);
        Common.writeShort(writer, NameAndTypeIndex + 1);
    }

    @Override
    public boolean resolve(ArrayList<?> FItems) {
        // use the index into the constant pool table
        // to find our UTF8 encoded class or interface name
        if (ClassIndex < FItems.size() && NameAndTypeIndex < FItems.size()) {
            Object o = FItems.get(ClassIndex);
            if (o instanceof ConstantClassInfo) {
                ParentClass = (ConstantClassInfo) o;
                ((ConstantPoolInfo) o).references++;
            }

            o = FItems.get(NameAndTypeIndex);
            if (o instanceof ConstantNameAndTypeInfo) {
                NameAndType = (ConstantNameAndTypeInfo) o;
                ((ConstantPoolInfo) o).references++;
            }

            return true;
        }

        return false;
    }

    @Override
    public void SetNameAndType(int Index, TConstantPool ConstantPool) {
        NameAndTypeIndex = Index;
        NameAndType = (ConstantNameAndTypeInfo) ConstantPool.getItem(Index);
        NameAndType.references++;
    }

    @Override
    public void SetParent(int Index, TConstantPool ConstantPool) {
        ClassIndex = Index;
        ParentClass = (ConstantClassInfo) ConstantPool.getItem(Index);
        ParentClass.references++;
    }
}

class ConstantMethodrefInfo extends ConstantPoolMethodInfo {
    private int ClassIndex;

    private int NameAndTypeIndex;

    @Override
    public void read(int tag, DataInput Reader) throws IOException {
            this.tag = tag;
            ClassIndex = Common.readShort(Reader);
            NameAndTypeIndex = Common.readShort(Reader);
            ClassIndex--;
            NameAndTypeIndex--;
    }

    public void write(DataOutput writer) throws IOException {
        // write the tag
        Common.writeByte(writer, tag);
        Common.writeShort(writer, ClassIndex + 1);
        Common.writeShort(writer, NameAndTypeIndex + 1);
    }

    @Override
    public boolean resolve(ArrayList<?> FItems) {
        // use the index into the constant pool table
        // to find our UTF8 encoded class or interface name
        if (ClassIndex < FItems.size() && NameAndTypeIndex < FItems.size()) {
            Object o = FItems.get(ClassIndex);
            if (o instanceof ConstantClassInfo) {
                ParentClass = (ConstantClassInfo) o;
                ((ConstantPoolInfo) o).references++;
            }

            o = FItems.get(NameAndTypeIndex);
            if (o instanceof ConstantNameAndTypeInfo) {
                NameAndType = (ConstantNameAndTypeInfo) o;
                ((ConstantPoolInfo) o).references++;
            }

            return true;
        }

        return false;
    }

    @Override
    public void SetNameAndType(int Index, TConstantPool ConstantPool) {
        NameAndTypeIndex = Index;
        NameAndType = (ConstantNameAndTypeInfo) ConstantPool.getItem(Index);
        NameAndType.references++;
    }

    @Override
    public void SetParent(int Index, TConstantPool ConstantPool) {
        ClassIndex = Index;
        ParentClass = (ConstantClassInfo) ConstantPool.getItem(Index);
        ParentClass.references++;
    }
}

class ConstantInterfaceMethodrefInfo extends ConstantPoolMethodInfo {
    private int ClassIndex;

    private int NameAndTypeIndex;

    @Override
    public void read(int tag, DataInput Reader) throws IOException {
            this.tag = tag;
            ClassIndex = Common.readShort(Reader);
            NameAndTypeIndex = Common.readShort(Reader);
            ClassIndex--;
            NameAndTypeIndex--;
    }

    public void write(DataOutput writer) throws IOException {
        // write the tag
        Common.writeByte(writer, tag);
        Common.writeShort(writer, ClassIndex + 1);
        Common.writeShort(writer, NameAndTypeIndex + 1);
    }

    @Override
    public boolean resolve(ArrayList<?> FItems) {
        // use the index into the constant pool table
        // to find our UTF8 encoded class or interface name
        if (ClassIndex <= FItems.size() && NameAndTypeIndex <= FItems.size()) {
            Object o = FItems.get(ClassIndex);
            if (o instanceof ConstantClassInfo) {
                ParentClass = (ConstantClassInfo) o;
                ((ConstantPoolInfo) o).references++;
            }

            o = FItems.get(NameAndTypeIndex);
            if (o instanceof ConstantNameAndTypeInfo) {
                NameAndType = (ConstantNameAndTypeInfo) o;
                ((ConstantPoolInfo) o).references++;
            }

            return true;
        }

        return false;
    }

    @Override
    public void SetNameAndType(int Index, TConstantPool ConstantPool) {
        NameAndTypeIndex = Index;
    }

    @Override
    public void SetParent(int Index, TConstantPool ConstantPool) {
        ClassIndex = Index;
    }
}

class ConstantStringInfo extends ConstantPoolVariableInfo {
    private int NameIndex;

    public ConstantStringInfo() {
        NameIndex = 0;
        Value = "";
    }

    @Override
    public void read(int tag, DataInput Reader) throws IOException {
            this.tag = tag;
            NameIndex = Common.readShort(Reader);
            NameIndex--;
    }

    public void write(DataOutput writer) throws IOException {
        // write the tag
        Common.writeByte(writer, tag);
        Common.writeShort(writer, NameIndex + 1);
    }

    @Override
    public boolean resolve(ArrayList<?> FItems) throws IOException {
        // use the index into the constant pool table
        // to find our UTF8 encoded class or interface name
        if (NameIndex < FItems.size()) {
            Object o = FItems.get(NameIndex);
            if (o instanceof ConstantUtf8Info) {
                Value = new String(((ConstantUtf8Info) o).Bytes, "UTF-8");
                ((ConstantPoolInfo) o).references++;

                return true;
            }
        }

        return false;
    }
}

class ConstantIntegerInfo extends ConstantPoolVariableInfo {
    private int Bytes;

    @Override
    public void read(int tag, DataInput Reader) throws IOException {
            this.tag = tag;
            Bytes = Common.readInt(Reader);
    }

    public void write(DataOutput writer) throws IOException {
        // write the tag
        Common.writeByte(writer, tag);
        Common.writeInt(writer, Bytes);
    }

    @Override
    public boolean resolve(ArrayList<?> FItems) {
        Value = (int) Bytes;
        return true;
    }
}

class ConstantFloatInfo extends ConstantPoolVariableInfo {
    private int Bytes;

    @Override
    public void read(int tag, DataInput Reader) throws IOException {
            this.tag = tag;
            Bytes = Common.readInt(Reader);
    }

    public void write(DataOutput writer) throws IOException {
        // write the tag
        Common.writeByte(writer, tag);
        Common.writeInt(writer, Bytes);
    }

    @Override
    public boolean resolve(ArrayList<?> FItems) {
        Value = Float.intBitsToFloat(Bytes);
        return true;
    }
}

class ConstantLongInfo extends ConstantPoolVariableInfo {
    private int HighBytes;

    private int LowBytes;

    @Override
    public void read(int tag, DataInput Reader) throws IOException {
            this.tag = tag;
            HighBytes = Common.readInt(Reader);
            LowBytes = Common.readInt(Reader);
            Value = ((long) HighBytes << 32) + LowBytes;
    }

    public void write(DataOutput writer) throws IOException {
        // write the tag
        Common.writeByte(writer, tag);
        Common.writeInt(writer, HighBytes);
        Common.writeInt(writer, LowBytes);
    }

    @Override
    public boolean resolve(ArrayList<?> FItems) {
        return true;
    }
}

class ConstantDoubleInfo extends ConstantPoolVariableInfo {
    private int HighBytes;

    private int LowBytes;

    public @Override
    void read(int tag, DataInput Reader) throws IOException {
            this.tag = tag;
            HighBytes = Common.readInt(Reader);
            LowBytes = Common.readInt(Reader);
            Value = "NOT_IMPLEMENTED";
    }

    public void write(DataOutput writer) throws IOException {
        // write the tag
        Common.writeByte(writer, tag);
        Common.writeInt(writer, HighBytes);
        Common.writeInt(writer, LowBytes);
    }

    @Override
    public boolean resolve(ArrayList<?> FItems) {
        Value = Double.longBitsToDouble(((long) HighBytes << 32) + LowBytes);
        return true;
    }
}

class ConstantNameAndTypeInfo extends ConstantPoolInfo {
    private int FNameIndex;

    private int FDescriptorIndex;

    public String Name;

    public String Descriptor;

    public ConstantNameAndTypeInfo() {
        tag = ConstantPoolInfoTag.ConstantNameAndType.value;
        FNameIndex = 0;
        FDescriptorIndex = 0;
        Name = "";
        Descriptor = "";
    }

    public ConstantNameAndTypeInfo(int IndexName, int IndexType, TConstantPool ConstantPool) {
        try {
            tag = ConstantPoolInfoTag.ConstantNameAndType.value;
            FNameIndex = IndexName;
            FDescriptorIndex = IndexType;
            Name = new String(((ConstantUtf8Info) ConstantPool.getItem(IndexName)).Bytes, "UTF-8");
            ConstantPool.getItem(IndexName).references++;
            Descriptor = new String(((ConstantUtf8Info) ConstantPool.getItem(IndexType)).Bytes, "UTF-8");
            ConstantPool.getItem(IndexType).references++;
        } catch (UnsupportedEncodingException e) {
System.err.println(e);
            assert false;
        }
    }

    // where index instanceof a valid index into the constant pool table
    public void SetName(int Index, TConstantPool ConstantPool) {
        try {
            FNameIndex = Index;
            Name = new String(((ConstantUtf8Info) ConstantPool.getItem(Index)).Bytes, "UTF-8");
            ConstantPool.getItem(Index).references++;
        } catch (UnsupportedEncodingException e) {
System.err.println(e);
            assert false;
        }
    }

    // where index instanceof a valid index into the constant pool table
    public void SetType(int Index, TConstantPool ConstantPool) {
        try {
            FDescriptorIndex = Index;
            Descriptor = new String(((ConstantUtf8Info) ConstantPool.getItem(Index)).Bytes, "UTF-8");
            ConstantPool.getItem(Index).references++;
        } catch (UnsupportedEncodingException e) {
System.err.println(e);
            assert false;
        }
    }

    public @Override
    void read(int tag, DataInput Reader) throws IOException {
            this.tag = tag;
            FNameIndex = Common.readShort(Reader);
            FNameIndex--;
            FDescriptorIndex = Common.readShort(Reader);
            FDescriptorIndex--;
    }

    public void write(DataOutput writer) throws IOException {
        // write the tag
        Common.writeByte(writer, tag);
        Common.writeShort(writer, FNameIndex + 1);
        Common.writeShort(writer, FDescriptorIndex + 1);
    }

    @Override
    public boolean resolve(ArrayList<?> FItems) {
        try {
            Name = null;

            if (FNameIndex < FItems.size()) {
                Object o = FItems.get(FNameIndex);
                if (o instanceof ConstantUtf8Info) {
                    Name = new String(((ConstantUtf8Info) o).Bytes, "UTF-8");
                    ((ConstantPoolInfo) o).references++;
                }
            }

        } catch (Exception e) {
e.printStackTrace(System.err);
            if (Name == null)
                Name = "Error retrieving Name!";
        }

        try {
            Descriptor = null;

            if (FDescriptorIndex < FItems.size()) {
                Object o = FItems.get(FDescriptorIndex);
                if (o instanceof ConstantUtf8Info) {
                    Descriptor = new String(((ConstantUtf8Info) o).Bytes, "UTF-8");
                    ((ConstantPoolInfo) o).references++;
                }
            }
        } catch (Exception e) {
e.printStackTrace(System.err);
            if (Descriptor == null)
                Descriptor = "Error retrieving Descriptor!";
        }

        return true;
    }

    public int getNameIndex() {

        {
            return FNameIndex;
        }
    }

    public int getTypeIndex() {

        {
            return FDescriptorIndex;
        }
    }
}

class ConstantUtf8Info extends ConstantPoolInfo {
    public int Length;

    public byte[] Bytes;

    public String Value;

    public ConstantUtf8Info() {
        Bytes = null;
        Length = 0;
        tag = ConstantPoolInfoTag.ConstantUtf8.value;
        references = 0;
    }

    public ConstantUtf8Info(String Text) {
        try {
            tag = ConstantPoolInfoTag.ConstantUtf8.value;
            Bytes = Text.getBytes("UTF-8");
            Length = Bytes.length;
            Value = new String(Bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
System.err.println(e);
            assert false;
        }
    }

    public void SetName(String Text) {
        try {
            Bytes = Text.getBytes("UTF-8");
            Length = Bytes.length;
            Value = new String(Bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
System.err.println(e);
            assert false;
        }
    }

    public @Override
    void read(int tag, DataInput Reader) throws IOException {
            this.tag = tag;
            Length = Common.readShort(Reader);
            Bytes = new byte[Length];
            Reader.readFully(Bytes, 0, Length);
            Common.position += Length;

            Value = new String(Bytes, "UTF-8");
    }

    public void write(DataOutput writer) throws IOException {
        // write the tag
        Common.writeByte(writer, tag);
        Common.writeShort(writer, Length);
        writer.write(Bytes);
        Common.position += Length;
    }

    public @Override
    boolean resolve(ArrayList<?> FItems) {
        return true;
    }
}

// 
// FIELD STRUCTURES
// 

class FieldInfo {
    int FAccessFlags;
    int FNameIndex;
    int FDescriptorIndex;
    ConstantUtf8Info FName;
    ConstantUtf8Info FDescriptor;
    TAttributes FAttributes;

    // my vars
    long FOffset;

    private FieldInfo() {
    }

    public FieldInfo(DataInput Reader, TConstantPool ConstantPool) {
        FAccessFlags = 0;
        FNameIndex = 0;
        FDescriptorIndex = 0;
        FName = null;
        FDescriptor = null;
        FAttributes = null;
        FOffset = 0;

        try {
            FOffset = Common.position;
            FAccessFlags = Common.readShort(Reader);
            FNameIndex = Common.readShort(Reader);
            FNameIndex--;
            FDescriptorIndex = Common.readShort(Reader);
            FDescriptorIndex--;
            // resolve the references
            FDescriptor = (ConstantUtf8Info) ConstantPool.getItem(FDescriptorIndex);
            FDescriptor.references++;
            FName = (ConstantUtf8Info) ConstantPool.getItem(FNameIndex);
            FName.references++;
            // Attributes should be able to handle any/all attribute streams
            FAttributes = new TAttributes(Reader, ConstantPool);
        } catch (Exception e) {
e.printStackTrace(System.err);
            // do nothing for now
        }
    }

    public void SetName(int index, TConstantPool ConstantPool) {
        FNameIndex = index;
        FName = (ConstantUtf8Info) ConstantPool.getItem(FNameIndex);
        FName.references++;
    }

    public ConstantUtf8Info getName() {
        return FName;
    }

    public void write(DataOutput writer) {
        try {
            FOffset = Common.position;
            Common.writeShort(writer, FAccessFlags);
            Common.writeShort(writer, FNameIndex + 1);
            Common.writeShort(writer, FDescriptorIndex + 1);

            // Attributes should be able to handle any/all attribute streams
            FAttributes.Write(writer);
        } catch (Exception e) {
e.printStackTrace(System.err);
            // do nothing for now
        }
    }

    public String getDescriptor() {
        return FDescriptor.Value;
    }

    public int getNameIndex() {
        return FNameIndex;
    }

    public TAttributes getAttributes() {
        return FAttributes;
    }

    public long getOffset() {
        return FOffset;
    }

    public Object clone() {
            FieldInfo newFileInfo = new FieldInfo();
            newFileInfo.FAccessFlags = FAccessFlags;
            newFileInfo.FNameIndex = FNameIndex;
            newFileInfo.FDescriptorIndex = FDescriptorIndex;
            newFileInfo.FName = FName;
            newFileInfo.FDescriptor = FDescriptor;
            newFileInfo.FAttributes = FAttributes;
            newFileInfo.FOffset = FOffset;
            return newFileInfo;
    }

    public void SetType(int index, TConstantPool ConstantPool) {
        FDescriptorIndex = index;
        FDescriptor = (ConstantUtf8Info) ConstantPool.getItem(FDescriptorIndex);
        FDescriptor.references++;
    }
}

// //
// METHODINFO STRUCTURES //
// //

class MethodInfo {
    int FAccessFlags;

    int FNameIndex;

    int FDescriptorIndex;

    ConstantUtf8Info FName;

    ConstantUtf8Info FDescriptor;

    TAttributes FAttributes;

    private MethodInfo() {
    }

    public MethodInfo(DataInput Reader, TConstantPool ConstantPool) {
        FAccessFlags = 0;
        FNameIndex = 0;
        FDescriptorIndex = 0;
        FName = null;
        FDescriptor = null;
        FAttributes = null;

        try {
            FAccessFlags = Common.readShort(Reader);
            FNameIndex = Common.readShort(Reader);
            FNameIndex--;
            FDescriptorIndex = Common.readShort(Reader);
System.err.printf("FDescriptorIndex: %04x\n", FDescriptorIndex);
            FDescriptorIndex--;
            // resolve the references
            FDescriptor = (ConstantUtf8Info) ConstantPool.getItem(FDescriptorIndex);
            FName = (ConstantUtf8Info) ConstantPool.getItem(FNameIndex);
            FDescriptor.references++;
            FName.references++;
            // Attributes should be able to handle any/all attribute streams
            FAttributes = new TAttributes(Reader, ConstantPool);
        } catch (Exception e) {
e.printStackTrace(System.err);
            // do nothing for now
        }
    }

    public void SetName(int index, TConstantPool ConstantPool) {
        FNameIndex = index;
        FName = (ConstantUtf8Info) ConstantPool.getItem(FNameIndex);
        FName.references++;
    }

    public void write(DataOutput writer) {
        try {
            Common.writeShort(writer, FAccessFlags);
            Common.writeShort(writer, FNameIndex + 1);
            Common.writeShort(writer, FDescriptorIndex + 1);

            // Attributes should be able to handle any/all attribute streams
            FAttributes.Write(writer);
        } catch (Exception e) {
e.printStackTrace(System.err);
            // do nothing for now
        }
    }

    public ConstantUtf8Info getName() {
        return FName;
    }

    public String getDescriptor() {
        return FDescriptor.Value;
    }

    public TAttributes getAttributes() {
        return FAttributes;
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
        return FNameIndex;
    }

    public Object clone() {
        MethodInfo newMethodInfo = new MethodInfo();
        newMethodInfo.FAccessFlags = FAccessFlags;
        newMethodInfo.FNameIndex = FNameIndex;
        newMethodInfo.FDescriptorIndex = FDescriptorIndex;
        newMethodInfo.FName = FName;
        newMethodInfo.FDescriptor = FDescriptor;
        newMethodInfo.FAttributes = FAttributes;
        return newMethodInfo;
    }

    public void SetType(int index, TConstantPool ConstantPool) {
        FDescriptorIndex = index;
        FDescriptor = (ConstantUtf8Info) ConstantPool.getItem(FDescriptorIndex);
        FDescriptor.references++;
    }
}

// //
// ATTRIBUTE STRUCTURES //
// //

abstract class AttributeInfo {
    // int AttributeNameIndex;
    // ConstantUtf8Info AttributeName;
    // int AttributeLength;
    // byte[] Bytes;

    public abstract void write(DataOutput writer) throws IOException;
}

class UnknownAttributeInfo extends AttributeInfo {
    int AttributeNameIndex;

    ConstantUtf8Info AttributeName;

    int AttributeLength;

    byte[] Bytes;

    public UnknownAttributeInfo(int NameIndex, DataInput Reader, TConstantPool ConstantPool) {
        AttributeNameIndex = 0;
        AttributeName = null;
        AttributeLength = 0;
        Bytes = null;

        try {
            AttributeNameIndex = NameIndex;
            AttributeLength = Common.readInt(Reader);
            Bytes = new byte[AttributeLength];
            Reader.readFully(Bytes, 0, AttributeLength);
            // resolve references
            AttributeName = (ConstantUtf8Info) ConstantPool.getItem(AttributeNameIndex);
            AttributeName.references++;
        } catch (Exception e) {
e.printStackTrace(System.err);
            // do nothing
        }
    }

     @Override
     public void write(DataOutput writer) throws IOException {
        Common.writeShort(writer, AttributeNameIndex + 1);
        Common.writeInt(writer, AttributeLength);
        writer.write(Bytes);
    }
}

class ConstantValueAttributeInfo extends AttributeInfo {
    int AttributeNameIndex;

    int AttributeLength;

    ConstantUtf8Info AttributeName;

    int ConstantValueIndex;

    ConstantPoolVariableInfo ConstantValue;

    public ConstantValueAttributeInfo(int NameIndex, DataInput Reader, TConstantPool ConstantPool) {
        AttributeNameIndex = 0;
        AttributeLength = 0;
        AttributeName = null;
        ConstantValueIndex = 0;
        ConstantValue = null;

        try {
            AttributeNameIndex = NameIndex;
            AttributeLength = Common.readInt(Reader);
            ConstantValueIndex = Common.readShort(Reader);
            ConstantValueIndex--;
            // resolve references
            AttributeName = (ConstantUtf8Info) ConstantPool.getItem(AttributeNameIndex);
            AttributeName.references++;
            ConstantValue = (ConstantPoolVariableInfo) ConstantPool.getItem(ConstantValueIndex);
            ConstantValue.references++;
        } catch (Exception e) {
e.printStackTrace(System.err);
            // do nothing
        }
    }

    public @Override
    void write(DataOutput writer) throws IOException {
        Common.writeShort(writer, AttributeNameIndex + 1);
        Common.writeInt(writer, AttributeLength);
        Common.writeShort(writer, ConstantValueIndex + 1);
    }
}

class CodeAttributeInfo extends AttributeInfo {
    // stuff we need
    int AttributeNameIndex;

    ConstantUtf8Info AttributeName;

    int AttributeLength;

    int MaxStack;

    int MaxLocals;

    int CodeLength;

    byte[] Code;

    int ExceptionTableLength;

    TExceptionTable[] ExceptionTable;

    TAttributes Attributes;

    // stuff i want
    long FOffsetOfCode;

    public CodeAttributeInfo(int NameIndex, DataInput Reader, TConstantPool ConstantPool) {
        AttributeNameIndex = 0;
        AttributeName = null;
        AttributeLength = 0;
        MaxStack = 0;
        MaxLocals = 0;
        CodeLength = 0;
        Code = null;
        ExceptionTableLength = 0;
        ExceptionTable = null;
        Attributes = null;

        try {
            AttributeNameIndex = NameIndex;
            AttributeLength = Common.readInt(Reader);

            MaxStack = Common.readShort(Reader);
            MaxLocals = Common.readShort(Reader);
            CodeLength = Common.readInt(Reader);

            // save the offset of the code stream
            FOffsetOfCode = Common.position;

            Code = new byte[CodeLength];
            Reader.readFully(Code, 0, CodeLength);
            Common.position += CodeLength;

            ExceptionTableLength = Common.readShort(Reader);
            ExceptionTable = new TExceptionTable[ExceptionTableLength];
            // fucking nested arrays! ;/
            for (int i = 0; i < ExceptionTableLength; i++) {
                ExceptionTable[i] = new TExceptionTable(Reader, ConstantPool);
            }

            Attributes = new TAttributes(Reader, ConstantPool);

            // resolve references
            AttributeName = (ConstantUtf8Info) ConstantPool.getItem(AttributeNameIndex);
            AttributeName.references++;
        } catch (Exception e) {
e.printStackTrace(System.err);
            // do nothing
        }
    }

     @Override
     public void write(DataOutput writer) throws IOException {
        Common.writeShort(writer, AttributeNameIndex + 1);
        Common.writeInt(writer, AttributeLength);

        Common.writeShort(writer, MaxStack);
        Common.writeShort(writer, MaxLocals);
        Common.writeInt(writer, CodeLength);
        writer.write(Code);

        Common.writeShort(writer, ExceptionTableLength);

        for (int i = 0; i < ExceptionTableLength; i++) {
            ExceptionTable[i].write(writer);
        }

        Attributes.Write(writer);
    }

    public long getCodeOffset() {

        return FOffsetOfCode;
    }
}

class ExceptionsAttributeInfo extends AttributeInfo {
    int AttributeNameIndex;

    ConstantUtf8Info AttributeName;

    int AttributeLength;

    int NumberOfExceptions;

    TException[] ExceptionTable;

    public ExceptionsAttributeInfo(int NameIndex, DataInput Reader, TConstantPool ConstantPool) {
        AttributeNameIndex = 0;
        AttributeName = null;
        AttributeLength = 0;
        NumberOfExceptions = 0;
        ExceptionTable = null;

        try {
            AttributeNameIndex = NameIndex;
            AttributeLength = Common.readInt(Reader);

            NumberOfExceptions = Common.readShort(Reader);
            ExceptionTable = new TException[NumberOfExceptions];
            // fucking nested arrays! ;/
            for (int i = 0; i < NumberOfExceptions; i++) {
                ExceptionTable[i] = new TException(Reader, ConstantPool);
            }
            // resolve references
            AttributeName = (ConstantUtf8Info) ConstantPool.getItem(AttributeNameIndex);
            AttributeName.references++;
        } catch (Exception e) {
e.printStackTrace(System.err);
            // do nothing
        }
    }

     @Override
     public void write(DataOutput writer) throws IOException {
        Common.writeShort(writer, AttributeNameIndex + 1);
        Common.writeInt(writer, AttributeLength);

        Common.writeShort(writer, NumberOfExceptions);

        for (int i = 0; i < NumberOfExceptions; i++) {
            ExceptionTable[i].write(writer);
        }
    }
}

class InnerClassesAttributeInfo extends AttributeInfo {
    int AttributeNameIndex;

    ConstantUtf8Info AttributeName;

    int AttributeLength;

    int NumberOfClasses;

    TClasses[] Classes;

    public InnerClassesAttributeInfo(int NameIndex, DataInput Reader, TConstantPool ConstantPool) {
        AttributeNameIndex = 0;
        AttributeName = null;
        AttributeLength = 0;
        NumberOfClasses = 0;
        Classes = null;

        try {
            AttributeNameIndex = NameIndex;
            AttributeLength = Common.readInt(Reader);

            NumberOfClasses = Common.readShort(Reader);
            Classes = new TClasses[NumberOfClasses];
            // fucking nested arrays! ;/
            for (int i = 0; i < NumberOfClasses; i++) {
                Classes[i] = new TClasses(Reader, ConstantPool);
            }
            // resolve references
            AttributeName = (ConstantUtf8Info) ConstantPool.getItem(AttributeNameIndex);
            AttributeName.references++;
        } catch (Exception e) {
e.printStackTrace(System.err);
            // do nothing
        }
    }

     @Override
     public void write(DataOutput writer) throws IOException {
        Common.writeShort(writer, AttributeNameIndex + 1);
        Common.writeInt(writer, AttributeLength);

        Common.writeShort(writer, NumberOfClasses);
        for (int i = 0; i < NumberOfClasses; i++) {
            Classes[i].write(writer);
        }
    }
}

class SyntheticAttributeInfo extends AttributeInfo {
    int AttributeNameIndex;

    ConstantUtf8Info AttributeName;

    int AttributeLength;

    public SyntheticAttributeInfo(int NameIndex, DataInput Reader, TConstantPool ConstantPool) {
        AttributeNameIndex = 0;
        AttributeName = null;
        AttributeLength = 0;

        try {
            AttributeNameIndex = NameIndex;
            AttributeLength = Common.readInt(Reader);

            // resolve references
            AttributeName = (ConstantUtf8Info) ConstantPool.getItem(AttributeNameIndex);
            AttributeName.references++;
        } catch (Exception e) {
e.printStackTrace(System.err);
            // do nothing
        }
    }

    @Override
    public void write(DataOutput writer) throws IOException {
        Common.writeShort(writer, AttributeNameIndex + 1);
        Common.writeInt(writer, AttributeLength);
    }
}

class SourceFileAttributeInfo extends AttributeInfo {
    int AttributeNameIndex;

    ConstantUtf8Info AttributeName;

    int AttributeLength;

    int SourceFileIndex;

    ConstantUtf8Info SourceFile;

    public SourceFileAttributeInfo(int NameIndex, DataInput Reader, TConstantPool ConstantPool) {
        AttributeNameIndex = 0;
        AttributeName = null;
        AttributeLength = 0;
        SourceFileIndex = 0;
        SourceFile = null;

        try {
            AttributeNameIndex = NameIndex;
            AttributeLength = Common.readInt(Reader);
            SourceFileIndex = Common.readShort(Reader);
            SourceFileIndex--;
            // resolve references
            AttributeName = (ConstantUtf8Info) ConstantPool.getItem(AttributeNameIndex);
            AttributeName.references++;
            SourceFile = (ConstantUtf8Info) ConstantPool.getItem(AttributeNameIndex);
            SourceFile.references++;
        } catch (Exception e) {
e.printStackTrace(System.err);
            // do nothing
        }
    }

    @Override
    public void write(DataOutput writer) throws IOException {
        Common.writeShort(writer, AttributeNameIndex + 1);
        Common.writeInt(writer, AttributeLength);
        Common.writeShort(writer, SourceFileIndex + 1);
    }
}

class LineNumberAttributeInfo extends AttributeInfo {
    int AttributeNameIndex;

    ConstantUtf8Info AttributeName;

    int AttributeLength;

    int LineNumberTableLength;

    TLineNumberTable[] LineNumberTable;

    long OriginalPos;

    public LineNumberAttributeInfo(int NameIndex, DataInput Reader, TConstantPool ConstantPool) {
        AttributeNameIndex = 0;
        AttributeName = null;
        AttributeLength = 0;
        LineNumberTableLength = 0;
        LineNumberTable = null;
        OriginalPos = 0;

        try {
            AttributeNameIndex = NameIndex;
            AttributeLength = Common.readInt(Reader);
            OriginalPos = Common.position;
System.err.printf("originalPos: %d\n", OriginalPos);
            LineNumberTableLength = Common.readShort(Reader);
            LineNumberTable = new TLineNumberTable[LineNumberTableLength];
            // fucking nested arrays! ;/
            for (int i = 0; i < LineNumberTableLength; i++) {
                LineNumberTable[i] = new TLineNumberTable(Reader);
            }
            // resolve references
            AttributeName = (ConstantUtf8Info) ConstantPool.getItem(AttributeNameIndex);
            AttributeName.references++;

System.err.printf("Common.position: 1: %d\n", Common.position);
            Common.position = OriginalPos + AttributeLength;
System.err.printf("Common.position: 2: %d\n", OriginalPos + AttributeLength);
        } catch (Exception e) {
            // do nothing
e.printStackTrace(System.err);
        }
    }

    @Override
    public void write(DataOutput writer) throws IOException {
        Common.writeShort(writer, AttributeNameIndex + 1);
        Common.writeInt(writer, AttributeLength);

        Common.writeShort(writer, LineNumberTableLength);
        for (int i = 0; i < LineNumberTableLength; i++) {
            LineNumberTable[i].write(writer);
        }
    }
}

class LocalVariablesAttributeInfo extends AttributeInfo {
    int AttributeNameIndex;

    ConstantUtf8Info AttributeName;

    int AttributeLength;

    int LocalVariableTableLength;

    TLocalVariableTable[] LocalVariableTable;

    public LocalVariablesAttributeInfo(int NameIndex, DataInput Reader, TConstantPool ConstantPool) {
        AttributeNameIndex = 0;
        AttributeName = null;
        AttributeLength = 0;
        LocalVariableTableLength = 0;
        LocalVariableTable = null;

        try {
            AttributeNameIndex = NameIndex;
            AttributeLength = Common.readInt(Reader);

            LocalVariableTableLength = Common.readShort(Reader);
            LocalVariableTable = new TLocalVariableTable[LocalVariableTableLength];
            // fucking nested arrays! ;/
            for (int i = 0; i < LocalVariableTableLength; i++) {
                LocalVariableTable[i] = new TLocalVariableTable(Reader, ConstantPool);
            }
            // resolve references
            AttributeName = (ConstantUtf8Info) ConstantPool.getItem(AttributeNameIndex);
            AttributeName.references++;
        } catch (Exception e) {
e.printStackTrace(System.err);
            // do nothing
        }
    }

    @Override
    public void write(DataOutput writer) throws IOException {
        Common.writeShort(writer, AttributeNameIndex + 1);
        Common.writeInt(writer, AttributeLength);

        Common.writeShort(writer, LocalVariableTableLength);
        for (int i = 0; i < LocalVariableTableLength; i++) {
            LocalVariableTable[i].write(writer);
        }
    }
}

class DeprecatedAttributeInfo extends AttributeInfo {
    int AttributeNameIndex;

    ConstantUtf8Info AttributeName;

    int AttributeLength;

    public DeprecatedAttributeInfo(int NameIndex, DataInput Reader, TConstantPool ConstantPool) {
        AttributeNameIndex = 0;
        AttributeName = null;
        AttributeLength = 0;

        try {
            AttributeNameIndex = NameIndex;
            // length should be zero..
            // TODO: maybe put a check in?? probably no need at thinstanceof
            // point..
            AttributeLength = Common.readInt(Reader);
            // resolve references
            AttributeName = (ConstantUtf8Info) ConstantPool.getItem(AttributeNameIndex);
            AttributeName.references++;
        } catch (Exception e) {
e.printStackTrace(System.err);
            // do nothing
        }
    }

     @Override
     public void write(DataOutput writer) throws IOException {
        Common.writeShort(writer, AttributeNameIndex + 1);
        Common.writeInt(writer, AttributeLength);
    }
}

// the inner attribute classes
class TLocalVariableTable {
    int StartPC;

    int Length;

    int NameIndex;

    ConstantUtf8Info Name;

    int DescriptorIndex;

    ConstantUtf8Info Descriptor;

    int Index;

    public TLocalVariableTable(DataInput Reader, TConstantPool ConstantPool) {
        StartPC = 0;
        Length = 0;
        NameIndex = 0;
        Name = null;
        DescriptorIndex = 0;
        Descriptor = null;
        Index = 0;

        try {
            StartPC = Common.readShort(Reader);
            StartPC--;
            Length = Common.readShort(Reader);
            NameIndex = Common.readShort(Reader);
            NameIndex--;
            DescriptorIndex = Common.readShort(Reader);
            DescriptorIndex--;
            Index = Common.readShort(Reader);
            Index--;
            // resolve references
            Name = (ConstantUtf8Info) ConstantPool.getItem(NameIndex);
            Name.references++;
            Descriptor = (ConstantUtf8Info) ConstantPool.getItem(DescriptorIndex);
            Descriptor.references++;
        } catch (Exception e) {
e.printStackTrace(System.err);
            // do nothing
        }
    }

    public void write(DataOutput writer) throws IOException {
        Common.writeShort(writer, StartPC);
        Common.writeShort(writer, Length);
        Common.writeShort(writer, NameIndex + 1);
        Common.writeShort(writer, DescriptorIndex + 1);
        Common.writeShort(writer, Index + 1);
    }
}

class TLineNumberTable {
    int StartPC;

    int LineNumber;

    public TLineNumberTable(DataInput Reader) {
        LineNumber = 0;
        StartPC = 0;

        try {
            StartPC = Common.readShort(Reader);
            LineNumber = Common.readShort(Reader);
        } catch (Exception e) {
e.printStackTrace(System.err);
            // do nothing
        }
    }

    public void write(DataOutput writer) throws IOException {
        Common.writeShort(writer, StartPC);
        Common.writeShort(writer, LineNumber);
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

    public TClasses(DataInput Reader, TConstantPool ConstantPool) {
        innerClassInfoIndex = 0;
        innerClassInfo = null;
        outerClassInfoIndex = 0;

        outerClassInfo = null;
        innerNameIndex = 0;

        innerName = null;
        innerClassAccessFlags = 0;


        try {
            innerClassInfoIndex = Common.readShort(Reader);
            innerClassInfoIndex--;
            outerClassInfoIndex = Common.readShort(Reader);
            outerClassInfoIndex--;
            innerNameIndex = Common.readShort(Reader);
            innerNameIndex--;
            innerClassAccessFlags = Common.readShort(Reader);

            // resolve references
            if (innerNameIndex >= 0) {
                innerName = (ConstantUtf8Info) ConstantPool.getItem(innerNameIndex);
                innerName.references++;
            }
            if (innerNameIndex >= 0) {
                innerClassInfo = (ConstantClassInfo) ConstantPool.getItem(innerClassInfoIndex);
                innerClassInfo.references++;
            }
            if (innerNameIndex >= 0) {
                outerClassInfo = (ConstantClassInfo) ConstantPool.getItem(outerClassInfoIndex);
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
    int ExceptionIndex;

    ConstantClassInfo Exception;

    public TException(DataInput Reader, TConstantPool ConstantPool) {
        ExceptionIndex = 0;

        try {
            ExceptionIndex = Common.readShort(Reader);
            ExceptionIndex--;
            // resolve references
            Exception = (ConstantClassInfo) ConstantPool.getItem(ExceptionIndex);
        } catch (Exception e) {
e.printStackTrace(System.err);
            Exception = null;
        }
    }

    public void write(DataOutput writer) throws IOException {
        Common.writeShort(writer, ExceptionIndex + 1);
    }
}

class TExceptionTable {
    int StartPC;

    int EndPC;

    int HandlerPC;

    int CatchType;

    ConstantClassInfo Catch;

    public TExceptionTable(DataInput Reader, TConstantPool ConstantPool) {
        StartPC = 0;
        EndPC = 0;
        HandlerPC = 0;
        CatchType = 0;
        Catch = null;

        try {
            StartPC = Common.readShort(Reader);
            StartPC--;
            EndPC = Common.readShort(Reader);
            EndPC--;
            HandlerPC = Common.readShort(Reader);
            HandlerPC--;
            CatchType = Common.readShort(Reader);
            CatchType--;

            if (CatchType >= 0) {
                Catch = (ConstantClassInfo) ConstantPool.getItem(CatchType);
            }
        } catch (Exception e) {
e.printStackTrace(System.err);
        }
    }

    public void write(DataOutput writer) throws IOException {
        Common.writeShort(writer, StartPC + 1);
        Common.writeShort(writer, EndPC + 1);
        Common.writeShort(writer, HandlerPC + 1);
        Common.writeShort(writer, CatchType + 1);
    }
}

// //
// INTERFACE STRUCTURES //
// //

class InterfaceInfo {
    private int FValue;

    private ConstantClassInfo FInterface;

    public InterfaceInfo(DataInput Reader, TConstantPool ConstantPool) {
        try {
            FValue = Common.readShort(Reader);
            FValue--;

            FInterface = (ConstantClassInfo) ConstantPool.getItem(FValue);
        } catch (Exception e) {
e.printStackTrace(System.err);
            FValue = 0;
            FInterface = null;
        }
    }

    public void write(DataOutput writer) {
        try {
            Common.writeShort(writer, FValue + 1);
        } catch (Exception e) {
        }
    }

    public int getValue() {
        return FValue;
    }

    public ConstantClassInfo getInterface() {
        return FInterface;
    }

    public String getName() {
        if (FInterface != null) {
            return FInterface.Name;
        }

        return "";
    }

    public void SetName(String NewName) {

    }
}

// //
// COMMON FUNCTIONS //
// //

class RenameData {
    String FType;

    String FName;

    public RenameData(String Type, String Name) {
        FType = Type;
        FName = Name;
    }

    public String[] GetData() {
        String[] s = new String[2];
        s[0] = FType;
        s[1] = FName;

        return s;
    }

    public String getFieldType() {
        return FType;
    }

    public void setFieldType(String value) {
        FType = value;
    }

    public String getFieldName() {
        return FName;
    }

    public void setFieldName(String value) {
        FName = value;
    }
}

class RenameDatabase {
    private Hashtable<String, ArrayList<RenameData>> FRenameMethods = null;

    private Hashtable<String, ArrayList<RenameData>> FRenameFields = null;

    private Hashtable<String, String> FRenameClass = null;

    public RenameDatabase() {
        FRenameMethods = new Hashtable<String, ArrayList<RenameData>>();
        FRenameFields = new Hashtable<String, ArrayList<RenameData>>();
        FRenameClass = new Hashtable<String, String>();
    }

    public void AddRename(Hashtable<String, ArrayList<RenameData>> DestTable, String ClassName, String OldDescriptor, String OldName, String NewDescriptor, String NewName) {
        ArrayList<RenameData> al = DestTable.get(ClassName);

        if (al == null) {
            al = new ArrayList<RenameData>();
            DestTable.put(ClassName, al);
        } else {
            // make sure it doesnt already exist
            for (int i = 0; i < al.size(); i += 2) {
                RenameData rd = al.get(i);

                if (rd.getFieldName() == OldName && rd.getFieldType() == OldDescriptor) {
                    // if it does, overwrite it, don't add in a new one
                    rd.setFieldName(NewName);
                    return;
                }
            }
        }

        al.add(new RenameData(OldDescriptor, OldName));
        al.add(new RenameData(NewDescriptor, NewName));
    }

    public RenameData GetRenameInfo(Hashtable<String, ArrayList<RenameData>> DestTable, String ClassName, String OldDescriptor, String OldName) {
        ArrayList<RenameData> al = DestTable.get(ClassName);

        if (al == null)
            return null;

        for (int i = 0; i < al.size(); i += 2) {
            RenameData rd = al.get(i);

            if (rd.getFieldName() == OldName && rd.getFieldType() == OldDescriptor) {
                return al.get(i + 1);
            }
        }

        return null;
    }

    public void AddRenameMethod(String ClassName, String OldDescriptor, String OldName, String NewDescriptor, String NewName) {
        AddRename(FRenameMethods, ClassName, OldDescriptor, OldName, NewDescriptor, NewName);
    }

    public void AddRenameField(String ClassName, String OldDescriptor, String OldName, String NewDescriptor, String NewName) {
        AddRename(FRenameFields, ClassName, OldDescriptor, OldName, NewDescriptor, NewName);
    }

    public RenameData GetNewMethodInfo(String ClassName, String OldDescriptor, String OldName) {
        // searches for a matching method in the methodlist
        return GetRenameInfo(FRenameMethods, ClassName, OldDescriptor, OldName);
    }

    public RenameData GetNewFieldInfo(String ClassName, String OldDescriptor, String OldName) {
        // searches for a matching method in the methodlist
        return GetRenameInfo(FRenameFields, ClassName, OldDescriptor, OldName);
    }

    public void AddRenameClass(String OldClassName, String NewClassName) {
        FRenameClass.put(OldClassName, NewClassName);
    }

    public String GetNewClassName(String OldClassName) {
        return FRenameClass.get(OldClassName);
    }

    public String GetNewClassNameOnly(String OldClassName) {
        String temp = GetNewClassName(OldClassName);

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

    public static String GetClassName(String FullName) {
        // gets the class name from a class path
        if (FullName.indexOf("/") > 0)
            return FullName.substring(FullName.lastIndexOf('/') + 1, FullName.length() - FullName.lastIndexOf('/') - 1);
        else
            return FullName;
    }

    public static String GetClassPath(String FullName) {
        // gets the class name from a class path
        return FullName.substring(0, FullName.lastIndexOf('/') + 1);
    }

    public static String NewClassName(String OriginalClassName, String NewName) {
        NewName = Common.GetClassName(NewName);
        // new name should be the short name
        // original class name should be original long name
        if (OriginalClassName.lastIndexOf('/') > 0) {
//            String old_name = OriginalClassName.Substring(OriginalClassName.lastIndexOf('/') + 1, OriginalClassName.length() - OriginalClassName.lastIndexOf('/') - 1);
            OriginalClassName = OriginalClassName.substring(0, OriginalClassName.lastIndexOf('/')) + OriginalClassName.substring(OriginalClassName.length() - OriginalClassName.lastIndexOf('/'));
//            OriginalClassName += NewName + old_name;
            OriginalClassName += NewName;

            return OriginalClassName;
        }

        // return NewName + OriginalClassName;
        return NewName;
    }

    public static String FixDescriptor(String Descriptor, String OldClassName, String NewClassName) {
        return Descriptor.replace("L" + OldClassName + ";", "L" + NewClassName + ";");
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

    public static void writeShort(DataOutput writer, int Data) throws IOException {
        if (writer == null) {
            return;
        }

        // convert the data from small endian to big endian
//        Data = SwapBytes(Data);

        writer.writeShort(Data);
        position += 2;
    }

    public static int readInt(DataInput Reader) throws IOException {
        if (Reader == null) {
            return 0;
        }

        // get the value, and then change it from big endian to small endian
        int val = Reader.readInt();
        position += 4;
//        int temp = val >>> 16;
//        temp = SwapBytes(temp);
//        val = val & 0x0FFFF;
//        val = SwapBytes(val);
//        val = (val << 16) | temp;

        return val;
    }

    public static void writeInt(DataOutput writer, int Data) throws IOException {
        if (writer == null) {
            return;
        }

        // convert the data from small endian to big endian
//        int temp = Data >>> 16;
//        temp = SwapBytes(temp);
//        Data = Data & 0x0FFFF;
//        Data = SwapBytes(Data);
//        Data = (Data << 16) | temp;

        writer.writeInt(Data);
        position += 4;
    }

    public static int readByte(DataInput Reader) throws IOException {
        if (Reader == null) {
            return 0;
        }

        position += 1;
        return Reader.readUnsignedByte();
    }

    public static void writeByte(DataOutput writer, int Data) throws IOException {
        if (writer == null) {
            return;
        }

        writer.writeByte(Data);
        position += 1;
    }
}
