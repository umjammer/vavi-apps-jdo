
package vavi.util.deobfuscation;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ProgressMonitor;


/**
 * These class does the actual deobfuscation
 */
class DeObfuscator {

    // event delegates
    public static ProgressMonitor progress;

    // private variables
    private List<File> files;
    private List<ClassFile> classFiles;
//    private List<TInterfaces> interfaces;
    private List<Object> changeList;

    private boolean thoroughMode;
    private boolean cleanup;
    private boolean renameClasses;

    /**
     * The DeObfuscating engine
     *
     * @param files All of the files in the project. Must be full path
     * + filename
     */
    public DeObfuscator(List<File> files) {
        if (files == null)
            return;

        this.files = files;

        for (File f : files) {
            if (!f.exists())
                return;
        }

        cleanup = false;
        thoroughMode = true;
        renameClasses = true;
    }

    public boolean doRename(String name) {
        List<String> bad_names;

        bad_names = new ArrayList<>();
        bad_names.add("for");
        bad_names.add("char");
        bad_names.add("void");
        bad_names.add("byte");
        bad_names.add("do");
        bad_names.add("int");
        bad_names.add("long");
        bad_names.add("else");
        bad_names.add("case");
        bad_names.add("new");
        bad_names.add("goto");
        bad_names.add("try");
        bad_names.add("null");

        name = Common.getClassName(name);

        if (name.charAt(0) == '<')
            return false;

        if (name.length() > 0 && name.length() <= 2)
            return true;

        if (name.length() > 0 && name.length() <= 3 && name.indexOf("$") > 0)
            return true;

        for (String s : bad_names) {
            if (s == name)
                return true;
        }

        return false;
    }

    private boolean classNameExists(String name) {
        for (Object classFile : classFiles.toArray()) {
            if (((ClassFile) classFile).getThisClassName() == name)
                return true;
        }

        return false;
    }

    private List<Object> deObfuscateSingleFile(int index, RenameDatabase renameStore) {
        ClassFile classFile = classFiles.get(index);

        if (classFile == null)
            return null;

        // add the class name to the head of the changelist
        changeList = new ArrayList<>();
        changeList.add(classFile.getThisClassName());

        String originalClassName = classFile.getThisClassName();
        String originalClassAndType = classFile.getThisClassName() + " : " + classFile.getSuperClassName();

        // rename the class and add the new class name to the changelist at [1]
        if (renameClasses && renameStore.getNewClassNameOnly(originalClassAndType) != null) {
            // check if we need to use a user-supplied class name first
            String newClassName = renameStore.getNewClassNameOnly(originalClassAndType);

            while (classNameExists(newClassName)) {
                newClassName += "_";
            }
            changeList.add(classFile.changeClassName(newClassName));
        } else if (renameClasses && doRename(originalClassName)) {
            String newClassName = "Class_" + Common.getClassName(originalClassName);

            // test if the filename we are changing to hasnt already been used!
            while (classNameExists(newClassName)) {
                newClassName += "_";
            }
            changeList.add(classFile.changeClassName(newClassName));
        } else
            changeList.add(originalClassName);

        // process the Methods
        for (int i = 0; i < classFile.getMethods().getItems().size(); i++) {
            MethodInfo mi = classFile.getMethods().getItems().get(i);
            RenameData rd = renameStore.getNewMethodInfo(originalClassAndType, mi.getDescriptor(), mi.getName().value);

            // this is the rule for renaming
            if (doRename(mi.getName().value) || rd != null) {
                // clone the original method
                MethodChangeRecord mcr = new MethodChangeRecord(mi);
                // rename all of the functions something meaningful
                String newName;
                // if the offset is zero, it probably means its an abstract
                // method
                if (classFile.getAccessFlags() == AccessFlags.ACC_INTERFACE)
                    newName = String.format("sub_iface_%x", i);
                else if (mi.getOffset() != 0)
                    newName = String.format("sub_%x", mi.getOffset());
                else
                    newName = String.format("sub_null_%x", i);

//                 if (thoroughMode) {
//                     int j = 0;
//                     while (ClassFile.getMethods().methodNameExists(newName)) { // rename the method
//                         newName = newName + "_" + j;
//                         j++;
//                     }
//                 }

                // user supplied names take precedence
                if (rd != null) {
                    newName = rd.getFieldName();
                }

                // change the method name
                classFile.changeMethodName(i, newName);
                // set the
                mcr.changedTo(mi);
                changeList.add(mcr);
            }

            // fix the descriptor regardless
            classFile.changeMethodParam(i, originalClassName, classFile.getThisClassName());
        }

        // process the Fields
        for (int i = 0; i < classFile.getFields().getItems().size(); i++) {
            FieldInfo fi = classFile.getFields().getItems().get(i);
            RenameData rd = renameStore.getNewFieldInfo(originalClassAndType, fi.getDescriptor(), fi.getName().value);

            if (doRename(fi.getName().value) || rd != null) {
                // clone the original method
                FieldChangeRecord fcr = new FieldChangeRecord(fi);
                // rename all of the fields something meaningful
                String newName;
                // if the offset is zero, it probably means its a null/abstract
                // method
                if (fi.getOffset() != 0)
                    newName = String.format("var_%x", fi.getOffset());
                else
                    newName = String.format("var_null_%x", fi.getOffset());

//                if (thoroughMode) {
//                    int j = 0;
//                    while (classFile.getMethods().fieldNameExists(newName)) { // rename the field
//                        newName = newName + "_" + j;
//                        j++;
//                    }
//                }

                if (rd != null) {
                    newName = rd.getFieldName();
                }

                classFile.changeFieldName(i, newName);

                fcr.changedTo(fi);
                changeList.add(fcr);
            }

            // fix the descriptor regardless
            classFile.changeFieldType(i, originalClassName, classFile.getThisClassName());
        }

        return changeList;
    }

    /**
     * This function runs over a class, fixing up any references from a
     * deobfuscated file.
     *
     * @param Index This is the index of the ClassFile to have its
     * references updated
     * @param changeList This is a list of before/after values from a
     * previously deobfuscated file
     */
    private void fixReferencePass1(int Index, List<Object> changeList, List<Object> ownerChangeList) {
        // the first pass does the following: - replaces the Super Class name
        // (if it needs replacing) - replaces any constant method/field names
        // (if they need replacing) - replaces the class field names (if needed)
        // it does NOT change the original class name
        ClassFile classFile = classFiles.get(Index);

        if (classFile == null) {
            return;
        }

        // - changeList[0] is always a string, which is the parent name of the
        // deobfuscated class
        // - changeList[1] is always the deobfuscated (new) class name... yes i
        // know this is lame :P
        String oldParentName = (String) changeList.get(0);
        String newParentName = (String) changeList.get(1);

        // check the Super class name if it needs renaming
        if (classFile.getSuperClassName() == oldParentName) {
            classFile.changeSuperClassName(newParentName);
        }

        // loop through the constant pool for field/method references
        // check the parent of each, and if the parent is the class we have
        // just modified, try and match it to one of the changes
        // in the changearray
        for (int i = 0; i < classFile.getConstantPool().getMaxItems(); i++) {
            if (classFile.getConstantPool().getItem(i) instanceof ConstantPoolMethodInfo) {
                ConstantPoolMethodInfo ci = (ConstantPoolMethodInfo) classFile.getConstantPool().getItem(i);

                // check its parent
                if (ci.parentClass.name == oldParentName || ci.parentClass.name == newParentName) {
                    // check the descriptor
                    // - for fields this is the field type
                    // - for methods this is the parameter list

                    // if parents are the same, check the name and descriptor
                    // against the list of originals
                    for (int j = 2; j < changeList.size(); j++) {
                        if ((changeList.get(j) instanceof MethodChangeRecord) && (ci instanceof ConstantMethodrefInfo || ci instanceof ConstantInterfaceMethodrefInfo)) {
                            if (ci instanceof ConstantInterfaceMethodrefInfo) {
                                // handle interface references differently
                                MethodChangeRecord mcr = (MethodChangeRecord) changeList.get(j);

                                // if found update it to the overridden version
                                if (mcr.getOriginalMethod().getName().value == ci.nameAndType.name && mcr.getOriginalMethod().getDescriptor() == ci.nameAndType.descriptor) {
                                    // find the overridden version
                                    for (int k = 2; k < ownerChangeList.size(); k++) {
                                        if (ownerChangeList.get(k) instanceof MethodChangeRecord) {
                                            MethodChangeRecord mcr2 = (MethodChangeRecord) ownerChangeList.get(k);
                                            if (mcr2.getOriginalMethod().getName().value == mcr.getOriginalMethod().getName().value && mcr2.getOriginalMethod().getDescriptor() == mcr.getOriginalMethod().getDescriptor()) {
                                                classFile.changeConstantFieldName(i, mcr2.getNewMethod().getName().value);
                                                break;
                                            }
                                        }
                                    }
                                }
                            } else {
                                MethodChangeRecord mcr = (MethodChangeRecord) changeList.get(j);

                                // if found update it to the new version...
                                if (mcr.getOriginalMethod().getName().value == ci.nameAndType.name && mcr.getOriginalMethod().getDescriptor() == ci.nameAndType.descriptor) {
                                    classFile.changeConstantFieldName(i, mcr.getNewMethod().getName().value);
                                    break;
                                }
                            }
                        } else if ((changeList.get(j) instanceof FieldChangeRecord) && (ci instanceof ConstantFieldrefInfo)) {
                            FieldChangeRecord fcr = (FieldChangeRecord) changeList.get(j);

                            // if found update it to the new version...
                            if (fcr.getOriginalField().getName().value == ci.nameAndType.name && fcr.getOriginalField().getDescriptor() == ci.nameAndType.descriptor) {
                                classFile.changeConstantFieldName(i, fcr.getNewField().getName().value);
                                break;
                            }
                        }
                    }
                }
            }
        }

        // also loop through the Fields array to change all the Types
        for (int i = 0; i < classFile.getFields().maxItems(); i++) {
            classFile.changeFieldType(i, oldParentName, newParentName);
        }
        // do the same for methods (fix the parameter list)
        for (int i = 0; i < classFile.getMethods().maxItems(); i++) {
            classFile.changeMethodParam(i, oldParentName, newParentName);
        }
        // and the same for all the interfaces
        for (int i = 0; i < classFile.getInterfaces().getItems().size(); i++) {
            if (classFile.getInterfaces().item(i).getName() == oldParentName)
                classFile.changeInterfaceName(i, newParentName);
        }
    }

    /**
     * Stage 2 simply goes through the constant pool searching for a ClassInfo
     * structure that matches
     * the obfuscated class name, and replaces it with the de-obfuscated class
     * name.
     *
     * This instanceof to ensure that any field/variable that references that
     * class will be updated, simply by
     * changing the class info structure at the source.
     *
     * @param index Index of the class file we want to update
     * @param changeList The array of changes we made deobfuscating a
     * file
     */
    private void fixReferencePass2(int index_, List<Object> changeList) {
        ClassFile classFile = classFiles.get(index_);

        if (classFile == null)
            return;

        String oldParentName = (String) changeList.get(0);
        String newParentName = (String) changeList.get(1);

        // iterate through the constant pool looking for class references
        // that match the old class name
        for (int i = 0; i < classFile.getConstantPool().getMaxItems(); i++) {
            if (classFile.getConstantPool().getItem(i) instanceof ConstantClassInfo) {
                ConstantClassInfo ci = (ConstantClassInfo) classFile.getConstantPool().getItem(i);

                // if we found a ClassInfo constant with the same name as the
                // old name
                if (ci.name == oldParentName) {
                    // create a new UTF String constant
                    ConstantUtf8Info ui = new ConstantUtf8Info();
                    // set it to the new parent name
                    ui.setName(newParentName);
                    // add it to the constant pool
                    int index = classFile.getConstantPool().add(ui);
                    // set our original ClassInfo constant's name to the newly
                    // added UTF String constant
                    ci.setName(index, classFile.getConstantPool());
                }
                // special condition for array type references
                else if (ci.name.indexOf("L" + oldParentName + ";") >= 0) {
                    // create a new UTF String constant
                    ConstantUtf8Info ui = new ConstantUtf8Info();
                    // set it to the new parent name
                    ui.setName(ci.name.replace("L" + oldParentName + ";", "L" + newParentName + ";"));
                    // add it to the constant pool
                    int index = classFile.getConstantPool().add(ui);
                    // set our original ClassInfo constant's name to the newly
                    // added UTF String constant
                    ci.setName(index, classFile.getConstantPool());

                }
            } else if (classFile.getConstantPool().getItem(i) instanceof ConstantPoolMethodInfo) {
                // check the descriptor
                // - for fields this instanceof the field type
                // - for methods this instanceof the parameter list
                classFile.changeConstantFieldType(i, oldParentName, newParentName);
            }
        }
    }

    private void FixReferences(List<List<Object>> masterChangeList) {
        // loop through the change record's and apply them to each file
        // (except itself)
        for (int i = 0; i < classFiles.size(); i++) {
            for (int j = 0; j < masterChangeList.size(); j++) {
                fixReferencePass1(i, masterChangeList.get(j), masterChangeList.get(i));
            }
        }

        for (int i = 0; i < classFiles.size(); i++) {
            for (int j = 0; j < masterChangeList.size(); j++) {
                fixReferencePass2(i, masterChangeList.get(j));
            }
        }
    }

    /**
     * Find the index of the parent of the classfile, if it exists in the
     * project.
     *
     * @param index Index of class file to find parent of
     * @returns positive integer index if found, else -1 if not found
     */
    int findParent(int index) {
        String ParentName = classFiles.get(index).getSuperClassName();

        for (int i = 0; i < classFiles.size(); i++) {
            if (i != index && classFiles.get(i).getThisClassName() == ParentName) {
                return i;
            }
        }

        return -1;
    }

//     int findClass(String className) {
//        for (int i = 0; i < classFiles.size(); i++) {
//            if (((ClassFile) classFiles[i]).getThisClassName() == className) {
//                return i;
//            }
//        }
//
//        return -1;
//    }

    int findInterface(String className) {
        for (int i = 0; i < classFiles.size(); i++) {
            if (classFiles.get(i).getAccessFlags() == AccessFlags.ACC_INTERFACE && classFiles.get(i).getThisClassName() == className) {
                return i;
            }
        }

        return -1;
    }

    List<List<Object>> addInheritance(int index, List<List<Object>> masterChangeList) {
        int Parent = findParent(index);

        if (Parent >= 0) {
            List<Object> originalChangeList = masterChangeList.get(index);
            List<Object> parentChangeList = masterChangeList.get(Parent);

            for (int i = 2; i < parentChangeList.size(); i++) {
                // add the rest of the parent entries to the original
                originalChangeList.add(parentChangeList.get(i));
            }

            // last of all, if the parent has another parent, recurse and do it
            // all again
            if (findParent(Parent) >= 0) {
                masterChangeList = addInheritance(Parent, masterChangeList);
            }
        }

        return masterChangeList;
    }

    List<List<Object>> addInterfaces(int index, List<List<Object>> masterChangeList) {
        // this needs to work differently to inheritance
        // it does the following:
        // 1. loop through each interface
        // 2. check the MasterChangeList for a matching interface
        // 3. if found, for all methods in the deobfuscated interface, find
        // corresponding entry in
        // current classes change list, and update it
        //   
        ClassFile classFile = classFiles.get(index);

        // for each class file, check each of its interfaces
        for (int i = 0; i < classFile.getInterfaces().getItems().size(); i++) {
            // check each interface if it matches any deobfuscated
            // classfile/interface in the project
            for (int j = 0; j < classFiles.size(); j++) {
                String oldName = (String) masterChangeList.get(j).get(0);

                if (oldName == classFile.getInterfaces().item(i).getName()) {
                    List<Object> originalChangeList = masterChangeList.get(index);
                    List<Object> interfaceChangeList = masterChangeList.get(j);

                    for (int k = 2; k < interfaceChangeList.size(); k++) {
                        // add the rest of the parent entries to the original
                        // NOTE: this might work best if added to the START of
                        // the list!
                        originalChangeList.set(2, interfaceChangeList.get(k));
                    }

                    break;
                }
            }
        }

        return masterChangeList;
    }

    List<List<Object>> fixInheritance(List<List<Object>> masterChangeList) {
        for (int i = 0; i < classFiles.size(); i++) {
            masterChangeList = addInheritance(i, masterChangeList);
            // MasterChangeList = AddInterfaces(i, MasterChangeList);
        }

        return masterChangeList;
    }

    public List<File> deObfuscateAll() {
        return deObfuscateAll(null);
    }

    public List<File> deObfuscateAll(RenameDatabase renameStore) {
        classFiles = new ArrayList<>();
//        interfaces = new List<TInterfaces>();
        List<List<Object>> masterChangeList = new ArrayList<>();
        List<File> newFileNameList = new ArrayList<>();
        int curr_progress = 0;

        progress.setProgress(0);

        // open each class file and add to array
        for (File fn : files) {
            ClassFile cf = new ClassFile(fn);

            if (cf != null) {
                if (cf.open()) {
                    classFiles.add(cf);

                    progress.setProgress(++curr_progress);
                }
            }
        }

        // do all the work in memory
        for (int i = 0; i < classFiles.size(); i++) {
            // this deobfuscates a single class, and keeps a record of all the
            // changes
            // in an arraylist of ChangeRecords
            //
            // we need more here!
            //
            // first, if the file we deobfuscated had a parent, we have to add
            // the entire change list
            // from the parent to the end of the current (recursively), minus
            // the old/new name
            // note: this duplications of data fixes problems with inheritance
            //
            masterChangeList.add(deObfuscateSingleFile(i, renameStore));

            progress.setProgress(i + 1);
        }

        progress.setProgress(0);
        curr_progress = 0;

        // iterate through all the class files using the change records saved
        // after the deobfuscation was done
        masterChangeList = fixInheritance(masterChangeList);

        // iterate through all the class files using the change records saved
        // after the deobfuscation was done
        FixReferences(masterChangeList);

        // save all the class files
        for (ClassFile cf : classFiles) {
            // extract the actual filename from the path and replace it with the
            // new ClassName
            String file_name = cf.getFile().getParent() + File.separator + Common.getClassName(cf.getThisClassName()) + ".class";

            // file_name = file_name.Replace('/', '\\');

            if ((file_name != cf.getFile().getPath()) && cleanup) {
                cf.getFile().delete();
            }

            // if for some reason the directory doesn't exist, create it
            if (!new File(file_name).getParentFile().exists())
                new File(file_name).getParentFile().mkdir();

            cf.save(file_name);

            // return the new filename so the main gui knows what to reload
            newFileNameList.add(new File(file_name));

            progress.setProgress(++curr_progress);
        }

        return newFileNameList;
    }

    public boolean getThoroughMode() {
        return thoroughMode;
    }

    public void setThoroughMode(boolean value) {
        thoroughMode = value;
    }

    public boolean getCleanup() {
        return cleanup;
    }

    public void setCleanup(boolean value) {
        cleanup = value;
    }

    public boolean getRenameClasses() {
        return renameClasses;
    }

    public void setRenameClasses(boolean value) {
        renameClasses = value;
    }
}

/**
 * These class encapsulates the java .class file With a few special methods
 * jammed in to help rename methods and fields (and refs)
 */
class ClassFile {
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
                if (methodRef.parentClass.name == thisClassName && methodRef.nameAndType.name == method.getName().value && methodRef.nameAndType.descriptor == method.getDescriptor()) {
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
            ConstantNameAndTypeInfo newNaT = new ConstantNameAndTypeInfo(newNameIndex, methodRef.nameAndType.getTypeIndex(), constantPool);

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
                if (fieldRef.parentClass.name == thisClassName && fieldRef.nameAndType.name == field.getName().value && fieldRef.nameAndType.descriptor == field.getDescriptor()) {
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
            ConstantNameAndTypeInfo NewNaT = new ConstantNameAndTypeInfo(newNameIndex, fieldRef.nameAndType.getTypeIndex(), constantPool);

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
        ConstantNameAndTypeInfo newNaT = new ConstantNameAndTypeInfo(newNameIndex, fieldRef.nameAndType.getTypeIndex(), constantPool);

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
        String newName = Common.FixDescriptor(fieldRef.nameAndType.descriptor, oldParentName, newParentName);

        if (oldName == newName)
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
        String newName = Common.FixDescriptor(fieldRef.getDescriptor(), oldParentName, newParentName);

        if (oldName == newName)
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
        String newName = Common.FixDescriptor(methodRef.getDescriptor(), oldParentName, newParentName);

        if (oldName == newName)
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

        if (intInfo.getName() == newName)
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

        // skip this coz we already passing the full name in
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

// 
// CLASS CHANGE RECORD
// 
// These classes are used to keep track of all the changes i make during
// deobfuscation
// of a single class. They are then used to iterate through all the rest of the
// files
// in the current "project" and fix up any references to the methods/fields we
// changed
//

abstract class ChangeRecord {
}

class MethodChangeRecord extends ChangeRecord {
    // just a simple class to hold the information temporarily
    private MethodInfo originalMethod;

    private MethodInfo newMethod;

    public MethodChangeRecord(MethodInfo original) {
        originalMethod = (MethodInfo) original.clone();
    }

    public void changedTo(MethodInfo new_) {
        newMethod = (MethodInfo) new_.clone();
    }

    public MethodInfo getOriginalMethod() {

        return originalMethod;
    }

    public MethodInfo getNewMethod() {
        return newMethod;
    }
}

class FieldChangeRecord extends ChangeRecord {
    // just a simple class to hold the information temporarily
    private FieldInfo originalField;

    private FieldInfo newField;

    public FieldChangeRecord(FieldInfo original) {
        originalField = (FieldInfo) original.clone();
    }

    public void changedTo(FieldInfo new_) {
        newField = (FieldInfo) new_.clone();
    }

    public FieldInfo getOriginalField() {
        return originalField;
    }

    public FieldInfo getNewField() {
        return newField;
    }
}

//
// INDIVIDUAL CLASSES
//
// These are all used by TClassFile to import each of its major sections
//

class ConstantPool {
    DataInput reader;
    List<ConstantPoolInfo> items = null;
    int maxItems = 0;

    public ConstantPool(DataInput reader) throws IOException {
        this.reader = reader;

        maxItems = Common.readShort(reader) - 1;
System.err.printf("maxItems: %d\n", maxItems);
        items = new ArrayList<>();
        int count = 0;

        // goes from 1 -> constantpoolcount - 1
        while (count < maxItems) {
            int tag = Common.readByte(reader);
System.err.printf("tag: %d\n", tag);

            switch (ConstantPoolInfoTag.valueOf(tag)) {
            case ConstantClass: {
                ConstantClassInfo cc = new ConstantClassInfo();
                cc.read(tag, reader);
                items.add(cc);
                break;
            }
            case ConstantString: {
                ConstantStringInfo cc = new ConstantStringInfo();
                cc.read(tag, reader);
                items.add(cc);
                break;
            }
            case ConstantFieldref: {
                ConstantFieldrefInfo cc = new ConstantFieldrefInfo();
                cc.read(tag, reader);
                items.add(cc);
                break;
            }
            case ConstantMethodref: {
                ConstantMethodrefInfo cc = new ConstantMethodrefInfo();
                cc.read(tag, reader);
                items.add(cc);
                break;
            }
            case ConstantInterfaceMethodref: {
                ConstantInterfaceMethodrefInfo cc = new ConstantInterfaceMethodrefInfo();
                cc.read(tag, reader);
                items.add(cc);
                break;
            }
            case ConstantInteger: {
                ConstantIntegerInfo cc = new ConstantIntegerInfo();
                cc.read(tag, reader);
                items.add(cc);
                break;
            }
            case ConstantFloat: {
                ConstantFloatInfo cc = new ConstantFloatInfo();
                cc.read(tag, reader);
                items.add(cc);
                break;
            }
            case ConstantLong: {
                ConstantLongInfo cc = new ConstantLongInfo();
                cc.read(tag, reader);
                items.add(cc);
                // longs take up two entries in the pool table
                count++;
                items.add(cc);
                break;
            }
            case ConstantDouble: {
                ConstantDoubleInfo cc = new ConstantDoubleInfo();
                cc.read(tag, reader);
                items.add(cc);
                // so do doubles
                count++;
                items.add(cc);
                break;
            }
            case ConstantNameAndType: {
                ConstantNameAndTypeInfo cc = new ConstantNameAndTypeInfo();
                cc.read(tag, reader);
                items.add(cc);
                break;
            }
            case ConstantUtf8: {
                ConstantUtf8Info cc = new ConstantUtf8Info();
                cc.read(tag, reader);
                items.add(cc);
                break;
            }
            default:
                // fail safe ?
//System.err.printf("tag: %s\n", ConstantPoolInfoTag.valueOf(tag));
                count++;
                break;
            }

            count++;
        }

        for (ConstantPoolInfo cc : items) {
            cc.resolve(items);
        }
    }

    public void write(DataOutput writer) throws IOException {
        // i am assuming we have a valid constant pool list...
        // i dont do any error checking here except bare minimum!

        // write the number of constant pool entries
        Common.writeShort(writer, maxItems + 1);
        int count = 0;

        // goes from 1 -> constantpoolcount - 1
        while (count < maxItems) {
            ConstantPoolInfo Item = items.get(count);

            switch (ConstantPoolInfoTag.valueOf(Item.tag)) {
            case ConstantClass: {
                ConstantClassInfo cc = (ConstantClassInfo) Item;
                cc.write(writer);

                break;
            }
            case ConstantString: {
                ConstantStringInfo cc = (ConstantStringInfo) Item;
                cc.write(writer);

                break;
            }
            case ConstantFieldref: {
                ConstantFieldrefInfo cc = (ConstantFieldrefInfo) Item;
                cc.write(writer);

                break;
            }
            case ConstantMethodref: {
                ConstantMethodrefInfo cc = (ConstantMethodrefInfo) Item;
                cc.write(writer);

                break;
            }
            case ConstantInterfaceMethodref: {
                ConstantInterfaceMethodrefInfo cc = (ConstantInterfaceMethodrefInfo) Item;
                cc.write(writer);

                break;
            }
            case ConstantInteger: {
                ConstantIntegerInfo cc = (ConstantIntegerInfo) Item;
                cc.write(writer);

                break;
            }
            case ConstantFloat: {
                ConstantFloatInfo cc = (ConstantFloatInfo) Item;
                cc.write(writer);

                break;
            }
            case ConstantLong: {
                ConstantLongInfo cc = (ConstantLongInfo) Item;
                cc.write(writer);

                // longs take up two entries in the pool table
                count++;
                break;
            }
            case ConstantDouble: {
                ConstantDoubleInfo cc = (ConstantDoubleInfo) Item;
                cc.write(writer);

                // so do doubles
                count++;
                break;
            }
            case ConstantNameAndType: {
                ConstantNameAndTypeInfo cc = (ConstantNameAndTypeInfo) Item;
                cc.write(writer);

                break;
            }
            case ConstantUtf8: {
                ConstantUtf8Info cc = (ConstantUtf8Info) Item;
                cc.write(writer);

                break;
            }
            default:
                // fail safe ?
                // BADDDDDDDDDDDDDDDDDDDDD, prolly should check/fix this
                count++;
                break;
            }

            count++;
        }
    }

    public int getMaxItems() {
        return maxItems;
    }

    public ConstantPoolInfo getItem(int index) {
        if (items != null && index < maxItems) {
            return items.get(index);
        }
System.err.printf("index: %04x\n", index);
new Exception("*** DUMMY ***").printStackTrace(System.err);
        return null;
    }

    public int add(ConstantPoolInfo newItem) {
        items.add(newItem);
        maxItems++;
        return (items.size() - 1);
    }
}

class Interfaces {
    DataInput reader;
    List<InterfaceInfo> items = null;
    int maxItems = 0;

    public Interfaces(DataInput reader, ConstantPool constantPool) throws IOException {
        this.reader = reader;

        maxItems = Common.readShort(reader) - 1;
        items = new ArrayList<>();
        int count = 0;

        // goes from 1 -> interfacecount - 1
        while (count <= maxItems) {
            InterfaceInfo ii = new InterfaceInfo(reader, constantPool);
            items.add(ii);

            count++;
        }
    }

    public void write(DataOutput writer) throws IOException {
        Common.writeShort(writer, maxItems + 1);

        int count = 0;

        // goes from 1 -> interfacecount - 1
        while (count <= maxItems) {
            InterfaceInfo ii = items.get(count);
            ii.write(writer);

            count++;
        }
    }

    public int maxItems() {
        return maxItems;
    }

    public InterfaceInfo item(int index) {
        if (index >= 0 && index < items.size())
            return items.get(index);

        // TODO: fix this fucking gay piece of shit
        return items.get(0);
    }

    public List<InterfaceInfo> getItems() {
        return items;
    }
}

class Fields {
    DataInput reader;
    List<FieldInfo> items = null;
    int maxItems = 0;

    public Fields(DataInput reader, ConstantPool constantPool) throws IOException {
        this.reader = reader;

        maxItems = Common.readShort(reader);
        items = new ArrayList<>();
        int count = 0;

        // goes from 1 -> fieldcount - 1
        while (count < maxItems) {
            FieldInfo fi = new FieldInfo(reader, constantPool);
            items.add(fi);

            count++;
        }
    }

    public void write(DataOutput writer) throws IOException {
        Common.writeShort(writer, maxItems);

        int count = 0;

        // goes from 1 -> fieldcount - 1
        while (count < maxItems) {
            FieldInfo fi = items.get(count);
            fi.write(writer);

            count++;
        }
    }

    public int maxItems() {
        return maxItems;
    }

    public FieldInfo Item(int Index) {
        if (items != null && Index < maxItems)
            return items.get(Index);

        return null;
    }

    public List<FieldInfo> getItems() {
        return items;
    }

    public boolean fieldNameExists(String name) {
        for (int i = 0; i < maxItems; i++) {
            if (name == items.get(i).getName().value)
                return true;
        }

        return false;
    }
}

class Methods {
    DataInput reader;
    List<MethodInfo> items = null;
    int maxItems = 0;

    public Methods(DataInput reader, ConstantPool constantPool) throws IOException {
        this.reader = reader;

        maxItems = Common.readShort(reader);
        items = new ArrayList<>();
        int count = 0;

        // goes from 1 -> fieldcount - 1
        while (count < maxItems) {
            MethodInfo mi = new MethodInfo(reader, constantPool);
            items.add(mi);

            count++;
        }
    }

    public void write(DataOutput writer) throws IOException {
        Common.writeShort(writer, maxItems);

        int count = 0;

        // goes from 1 -> fieldcount - 1
        while (count < maxItems) {
            MethodInfo mi = items.get(count);
            mi.write(writer);

            count++;
        }
    }

    public int maxItems() {
        return maxItems;
    }

    public MethodInfo item(int index) {
        if (items != null && index < maxItems)
            return items.get(index);

        return null;
    }

    public List<MethodInfo> getItems() {
        return items;
    }

    public boolean methodNameExists(String name) {
        for (int i = 0; i < maxItems; i++) {
            if (name == items.get(i).getName().value)
                return true;
        }

        return false;
    }

}

class Attributes {
    DataInput reader;
    List<Object> items = null;
    int maxItems = 0;

    public Attributes(DataInput reader, ConstantPool constantPool) throws IOException {
        this.reader = reader;

        maxItems = Common.readShort(reader) - 1;
        items = new ArrayList<>();
        int count = 0;

        // goes from 1 -> attributescount - 1
        while (count <= maxItems) {
            int nameIndex = Common.readShort(reader);
            nameIndex--;
            ConstantUtf8Info Name = (ConstantUtf8Info) constantPool.getItem(nameIndex);

            switch (AttributeType.valueOf(Name.value)) {
            case Code: {
                CodeAttributeInfo ai = new CodeAttributeInfo(nameIndex, reader, constantPool);

                items.add(ai);
                break;
            }
            case ConstantValue: {
                ConstantValueAttributeInfo ai = new ConstantValueAttributeInfo(nameIndex, reader, constantPool);

                items.add(ai);
                break;
            }
            case Deprecated: {
                DeprecatedAttributeInfo ai = new DeprecatedAttributeInfo(nameIndex, reader, constantPool);

                items.add(ai);
                break;
            }
            case Exceptions: {
                ExceptionsAttributeInfo ai = new ExceptionsAttributeInfo(nameIndex, reader, constantPool);

                items.add(ai);
                break;
            }
            case InnerClasses: {
                InnerClassesAttributeInfo ai = new InnerClassesAttributeInfo(nameIndex, reader, constantPool);

                items.add(ai);
                break;
            }
            case LineNumberTable: {
                LineNumberAttributeInfo ai = new LineNumberAttributeInfo(nameIndex, reader, constantPool);

                items.add(ai);
                break;
            }
            case LocalVariableTable: {
                LocalVariablesAttributeInfo ai = new LocalVariablesAttributeInfo(nameIndex, reader, constantPool);

                items.add(ai);
                break;
            }
            case SourceFile: {
                SourceFileAttributeInfo ai = new SourceFileAttributeInfo(nameIndex, reader, constantPool);

                items.add(ai);
                break;
            }
            case Synthetic: {
                SyntheticAttributeInfo ai = new SyntheticAttributeInfo(nameIndex, reader, constantPool);

                items.add(ai);
                break;
            }
            default: {
                AttributeInfo ai = new UnknownAttributeInfo(nameIndex, reader, constantPool);

                items.add(ai);
                break;
            }
            }

            count++;
        }
    }

    public void write(DataOutput writer) throws IOException {
        Common.writeShort(writer, maxItems + 1);

        int count = 0;

        // goes from 1 -> attributescount - 1
        while (count <= maxItems) {
            AttributeInfo item = (AttributeInfo) items.get(count);

            item.write(writer);

            count++;
        }
    }

    public List<Object> getItems() {
        return items;
    }
}
