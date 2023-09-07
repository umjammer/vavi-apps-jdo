/*
 * https://github.com/fileoffset/JDO
 */

package vavi.util.deobfuscation.objects;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import vavi.util.deobfuscation.common.AccessFlags;
import vavi.util.deobfuscation.common.Common;
import vavi.util.deobfuscation.common.ConstantPoolInfoTag;
import vavi.util.deobfuscation.common.constantpoolinfo.ConstantClassInfo;
import vavi.util.deobfuscation.common.constantpoolinfo.ConstantFieldrefInfo;
import vavi.util.deobfuscation.common.constantpoolinfo.ConstantMethodrefInfo;
import vavi.util.deobfuscation.common.constantpoolinfo.ConstantNameAndTypeInfo;
import vavi.util.deobfuscation.common.constantpoolinfo.ConstantPoolMethodInfo;
import vavi.util.deobfuscation.common.constantpoolinfo.ConstantUtf8Info;
import vavi.util.deobfuscation.common.info.FieldInfo;
import vavi.util.deobfuscation.common.info.InterfaceInfo;
import vavi.util.deobfuscation.common.info.MethodInfo;


/**
 * These class encapsulates the java .class file With a few special methods
 * jammed in to help rename methods and fields (and refs)
 */
public class ClassFile {
    // my internal variables
    String thisClassName;

    String superClassName;

    // internal class file members as designated by Sun
    private int magic;

    private int minorVersion;

    private int majorVersion;

    // private int FConstantPoolCount;
    private ConstantPool constantPool;

    private AccessFlags accessFlags;

    private int thisClass;

    private int superClass;

    // private int FInterfacesCount;
    private Interfaces interfaces;

    // private int FFieldsCount;
    private Fields fields;

    // private int FMethodsCount;
    private Methods methods;

    // private int FAttributesCount;
    private Attributes attributes;

    // internal variables
    private File classFile = null;

    private DataInput reader = null;

    public ClassFile(File classFileName) {
        classFile = classFileName;
//        FHasBeenOpened = false;
        thisClassName = "";
        superClassName = "";
    }

    public boolean open() {
        if (classFile.exists()) {
            try {
                // read the .class file systematically
                InputStream fs = new FileInputStream(classFile);
                reader = new DataInputStream(fs);
                Common.position = 0;
                // read header
                magic = Common.readInt(reader);
                System.err.printf("magic: %08x\n", magic);

                if (magic != 0x0CAFEBABE) {
                    return false;
                }

                minorVersion = Common.readShort(reader);
                System.err.printf("minorVersion: %04x\n", minorVersion);
                majorVersion = Common.readShort(reader);
                System.err.printf("majorVersion: %04x\n", majorVersion);
                // read constant pool
                // this also reads the "FConstantPoolCount"
                // so instead use FConstantPool.MaxItems or somesuch
                constantPool = new ConstantPool(reader);
                // more constants
                accessFlags = AccessFlags.valueOf(Common.readShort(reader));
                thisClass = Common.readShort(reader);
                thisClass--;
                System.err.printf("thisClass: %04x\n", thisClass);
                superClass = Common.readShort(reader);
                superClass--;
                System.err.printf("superClass: %04x\n", superClass);

                thisClassName = ((ConstantClassInfo) constantPool.getItem(thisClass)).name;
                (constantPool.getItem(thisClass)).references++;
                superClassName = ((ConstantClassInfo) constantPool.getItem(superClass)).name;
                (constantPool.getItem(superClass)).references++;

                interfaces = new Interfaces(reader, constantPool);
                fields = new Fields(reader, constantPool);
                methods = new Methods(reader, constantPool);
                attributes = new Attributes(reader, constantPool);

                // FHasBeenOpened = true;

                fs.close();
                return true;
            } catch (Exception e) {
                // catch any unhandled exceptions here
                // and exit gracefully.
                // garbage collection does the rest ;D
                e.printStackTrace(System.err);
                return false;
            }
        }
        System.err.println("here2");
        return false;
    }

    public boolean save(String fileName) {
//         if (true) { // FHasBeenOpened)

        try {
            // read the .class file systematically
            OutputStream fs = new FileOutputStream(fileName);
            DataOutput writer = new DataOutputStream(fs);
            Common.position = 0;
            // write header
            Common.writeInt(writer, magic);

            Common.writeShort(writer, minorVersion);
            Common.writeShort(writer, majorVersion);
            // write constant pool
            // this also writes the "FConstantPoolCount"
            constantPool.write(writer);
            // more constants
            Common.writeShort(writer, accessFlags.value);
            Common.writeShort(writer, thisClass + 1);
            Common.writeShort(writer, superClass + 1);

            interfaces.write(writer);
            fields.write(writer);
            methods.write(writer);
            attributes.write(writer);

            fs.close();
            return true;
        } catch (Exception e) {
            // catch any unhandled exceptions here
            // and exit gracefully.
            // garbage collection does the rest ;D
            e.printStackTrace(System.err);
            return false;
        }
//         }
    }

    public int getMagic() {
        return magic;
    }

    public void setMagic(int value) {
        magic = value;
    }

    public String version() {
        return majorVersion + "." + minorVersion;
    }

    public File getFile() {
        return classFile;
    }

    public AccessFlags getAccessFlags() {
        return accessFlags;
    }

    public ConstantPool getConstantPool() {
        return constantPool;
    }

    public Interfaces getInterfaces() {
        return interfaces;
    }

    public Fields getFields() {
        return fields;
    }

    public Methods getMethods() {
        return methods;
    }

    public Attributes getAttributes() {
        return attributes;
    }

    public ChangeRecord changeMethodName(int methodNumber, String newName) {
        MethodInfo method = methods.getItems().get(methodNumber);
        // MethodInfo OriginalMethod = Method.Clone();
        // MethodInfo NewMethod = null;
        ChangeRecord result = null;
        ConstantMethodrefInfo methodRef = null;
        int newNameIndex;

        // first we need to loop through the constant pool for method
        // references that match our new method name
        for (int i = 0; i < constantPool.getMaxItems(); i++) {
            if (constantPool.getItem(i).tag == (byte) ConstantPoolInfoTag.ConstantMethodref.value) {
                methodRef = (ConstantMethodrefInfo) constantPool.getItem(i);
                if (methodRef.parentClass.name.equals(thisClassName) && methodRef.nameAndType.name.equals(method.getName().value) &&
                        methodRef.nameAndType.descriptor.equals(method.getDescriptor())) {
                    // jackpot, we found the reference!
                    // there should be only one, so we will break and fix it up
                    // after we generate the new name
                    break;
                }
            }

            methodRef = null;
        }

        method.getName().references--;
        // add a new String constant to the pool
        ConstantUtf8Info newUtf = new ConstantUtf8Info(newName);

        newNameIndex = getConstantPool().add(newUtf);

        // set the method its new name
        method.setName(newNameIndex, getConstantPool());
        method.getName().references = 1;

        // NewMethod = Method.Clone();

        if (methodRef == null)
            return result;

        if (methodRef.nameAndType.references <= 1) {
            // if this instanceof the only reference to the name/type descriptor
            // we can overwrite the value
            methodRef.nameAndType.setName(newNameIndex, constantPool);
        } else {
            // we have to make a new one !
            methodRef.nameAndType.references--;
            // add a new String constant to the pool
            ConstantNameAndTypeInfo newNaT = new ConstantNameAndTypeInfo(newNameIndex,
                                                                         methodRef.nameAndType.getTypeIndex(),
                                                                         constantPool);

            int newIndex = getConstantPool().add(newNaT);

            // set the method its new name
            methodRef.setNameAndType(newIndex, getConstantPool());
            methodRef.nameAndType.references = 1;
        }

        return result;
    }

    public ChangeRecord changeFieldName(int fieldNumber, String newName) {
        FieldInfo field = fields.getItems().get(fieldNumber);
//        FieldInfo originalFieldInfo = Field.clone();
//        FieldInfo newField = null;
        ChangeRecord result = null;
        ConstantFieldrefInfo fieldRef = null;
        int newNameIndex;

        // first we need to loop through the constant pool for method
        // references that match our new method name
        for (int i = 0; i < constantPool.getMaxItems(); i++) {
            if (constantPool.getItem(i).tag == (byte) ConstantPoolInfoTag.ConstantFieldref.value) {
                fieldRef = (ConstantFieldrefInfo) constantPool.getItem(i);
                if (fieldRef.parentClass.name.equals(thisClassName) && fieldRef.nameAndType.name.equals(field.getName().value) &&
                        fieldRef.nameAndType.descriptor.equals(field.getDescriptor())) {
                    // jackpot, we found the reference!
                    // there should be only one, so we will break and fix it up
                    // after we generate the new name
                    break;
                }
            }

            fieldRef = null;
        }

        field.getName().references--;

        // add a new String constant to the pool
        ConstantUtf8Info newUtf = new ConstantUtf8Info(newName);

        newNameIndex = getConstantPool().add(newUtf);

        // set the method its new name
        field.setName(newNameIndex, getConstantPool());
        field.getName().references = 1;

        // NewField = Field.Clone();

        if (fieldRef == null) {
            return result;
        }

        if (fieldRef.nameAndType.references <= 1) {
            // if this instanceof the only reference to the name/type descriptor
            // we can overwrite the value
            fieldRef.nameAndType.setName(newNameIndex, constantPool);
        } else {
            // we have to make a new one !
            fieldRef.nameAndType.references--;
            // add a new String constant to the pool
            ConstantNameAndTypeInfo NewNaT = new ConstantNameAndTypeInfo(newNameIndex,
                                                                         fieldRef.nameAndType.getTypeIndex(),
                                                                         constantPool);

            int NewIndex = getConstantPool().add(NewNaT);

            // set the method its new name
            fieldRef.setNameAndType(NewIndex, getConstantPool());
            fieldRef.nameAndType.references = 1;
        }

        return result;
    }

    public void changeConstantFieldName(int fieldNumber, String newName) {
        // takes an index into the constantpool
        // simple changes the name of a method/field in the constant pool
        // always create new name
        // TODO: check this!

        ConstantPoolMethodInfo fieldRef = (ConstantPoolMethodInfo) constantPool.getItem(fieldNumber);

        ConstantUtf8Info newNameString = new ConstantUtf8Info(newName);
        int newNameIndex = constantPool.add(newNameString);

        // we have to make a new one !
        fieldRef.nameAndType.references--;
        // add a new String constant to the pool
        ConstantNameAndTypeInfo newNaT = new ConstantNameAndTypeInfo(newNameIndex,
                                                                     fieldRef.nameAndType.getTypeIndex(),
                                                                     constantPool);

        int NewIndex = constantPool.add(newNaT);

        // set the method its new name
        fieldRef.setNameAndType(NewIndex, constantPool);
        fieldRef.nameAndType.references = 1;
    }

    public void changeConstantFieldParent(int fieldNumber, int parentNumber) {
        ConstantPoolMethodInfo fieldRef = (ConstantPoolMethodInfo) constantPool.getItem(fieldNumber);

        fieldRef.parentClass.references--;
        fieldRef.setParent(parentNumber, constantPool);
    }

    public void changeConstantFieldType(int fieldNumber, String oldParentName, String newParentName) {
        // takes an index into the constantpool
        // simple changes the name of a method/field in the constant pool
        // always create new name
        // TODO: check this!

        ConstantPoolMethodInfo fieldRef = (ConstantPoolMethodInfo) constantPool.getItem(fieldNumber);
        String oldName = fieldRef.nameAndType.descriptor;
        String newName = Common.fixDescriptor(fieldRef.nameAndType.descriptor, oldParentName, newParentName);

        if (oldName.equals(newName))
            return;

        ConstantUtf8Info newTypeString = new ConstantUtf8Info(newName);
        int newTypeIndex = constantPool.add(newTypeString);

        fieldRef.nameAndType.setType(newTypeIndex, constantPool);
    }

    public void changeFieldType(int fieldNumber, String oldParentName, String newParentName) {
        // takes an index into the constantpool
        // simple changes the name of a method/field in the constant pool
        // TODO: check this!

        FieldInfo fieldRef = fields.Item(fieldNumber);

        String oldName = fieldRef.getDescriptor();
        String newName = Common.fixDescriptor(fieldRef.getDescriptor(), oldParentName, newParentName);

        if (oldName.equals(newName))
            return;

        ConstantUtf8Info newTypeString = new ConstantUtf8Info(newName);
        int newTypeIndex = constantPool.add(newTypeString);

        // set the method its new name
        fieldRef.setType(newTypeIndex, constantPool);
    }

    public void changeMethodParam(int methodNumber, String oldParentName, String newParentName) {
        // takes an index into the constantpool
        // simple changes the name of a method/field in the constant pool
        // TODO: check this!

        MethodInfo methodRef = methods.item(methodNumber);

        String oldName = methodRef.getDescriptor();
        String newName = Common.fixDescriptor(methodRef.getDescriptor(), oldParentName, newParentName);

        if (oldName.equals(newName))
            return;

        ConstantUtf8Info newTypeString = new ConstantUtf8Info(newName);
        int newTypeIndex = constantPool.add(newTypeString);

        // set the method its new name
        methodRef.setType(newTypeIndex, constantPool);
    }

    public void changeInterfaceName(int interfaceNumber, String newName) {
        // takes an index into the interface list
        // simple changes the name of a method/field in the constant pool
        // TODO: check this!

        InterfaceInfo intInfo = interfaces.item(interfaceNumber);

        if (intInfo.getName().equals(newName))
            return;

        ConstantUtf8Info newTypeString = new ConstantUtf8Info(newName);
        int newTypeIndex = constantPool.add(newTypeString);

        // set the interface its new name
        ConstantClassInfo cci = (ConstantClassInfo) getConstantPool().getItem(intInfo.getValue());
        cci.setName(newTypeIndex, constantPool);
    }

    public String getThisClassName() {
        return thisClassName;
    }

    public String getSuperClassName() {
        return superClassName;
    }

    public String changeClassName(String name) {
        ConstantClassInfo classInfo = (ConstantClassInfo) constantPool.getItem(thisClass);
        ConstantUtf8Info utfInfo = (ConstantUtf8Info) constantPool.getItem(classInfo.nameIndex);

        // change the class name, not the directory structure
        name = Common.newClassName(getThisClassName(), name);

        // we have to make a new one !
        utfInfo.references--;
        // add a new String constant to the pool
        ConstantUtf8Info newUtf = new ConstantUtf8Info(name);

        int newIndex = getConstantPool().add(newUtf);

        // set the method its new name
        classInfo.setName(newIndex, constantPool);
        newUtf.references = 1;

        thisClassName = ((ConstantClassInfo) constantPool.getItem(thisClass)).name;

        return name;
    }

    public int changeSuperClassName(String newName) {
        ConstantClassInfo classInfo = (ConstantClassInfo) constantPool.getItem(superClass);
        ConstantUtf8Info utfInfo = (ConstantUtf8Info) constantPool.getItem(classInfo.nameIndex);

        // skip this coz we're already passing the full name in
        // NewName = Common.NewClassName(FSuperClassName, NewName);

        if (utfInfo.references <= 1) {
            // if this is the only reference to the name/type descriptor
            // we can overwrite the value
            utfInfo.setName(newName);
        } else {
            // we have to make a new one !
            utfInfo.references--;
            // add a new String constant to the pool
            ConstantUtf8Info NewUtf = new ConstantUtf8Info(newName);

            int NewIndex = getConstantPool().add(NewUtf);

            // set the method its new name
            classInfo.nameIndex = NewIndex;
            NewUtf.references = 1;
        }

        superClassName = ((ConstantClassInfo) constantPool.getItem(superClass)).name;

        return superClass;
    }

    public int addConstantClassName(String newName) {
        ConstantClassInfo ClassInfo = new ConstantClassInfo();
        ConstantUtf8Info UtfInfo = new ConstantUtf8Info();

        int newClassIndex = constantPool.add(ClassInfo);
        int newUtfIndex = constantPool.add(UtfInfo);

        UtfInfo.setName(newName);
        ClassInfo.setName(newUtfIndex, constantPool);

        return newClassIndex;
    }
}
