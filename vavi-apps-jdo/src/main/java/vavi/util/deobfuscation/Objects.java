
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

import javax.swing.ProgressMonitor;


/**
 * These class does the actual deobfuscation
 */
class TDeObfuscator {

    // event delegates
    public static ProgressMonitor Progress;

    // private variables
    private ArrayList<File> FFiles;

    private ArrayList<TClassFile> FClassFiles;

//    private ArrayList<TInterfaces> FInterfaces;

    private ArrayList<Object> FChangeList;

    private boolean FThoroughMode;

    private boolean FCleanup;

    private boolean FRenameClasses;

    /**
     * The DeObfuscating engine
     *
     * @param Files All of the files in the project. Must be full path
     * + filename
     */
    public TDeObfuscator(ArrayList<File> Files) {
        if (Files == null)
            return;

        FFiles = Files;

        for (File f : FFiles) {
            if (!f.exists())
                return;
        }

        FCleanup = false;
        FThoroughMode = true;
        FRenameClasses = true;
    }

    public boolean DoRename(String Name) {
        ArrayList<String> bad_names;

        bad_names = new ArrayList<String>();
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

        Name = Common.GetClassName(Name);

        if (Name.charAt(0) == '<')
            return false;

        if (Name.length() > 0 && Name.length() <= 2)
            return true;

        if (Name.length() > 0 && Name.length() <= 3 && Name.indexOf("$") > 0)
            return true;

        for (String s : bad_names) {
            if (s == Name)
                return true;
        }

        return false;
    }

    private boolean ClassNameExists(String Name) {
        for (Object ClassFile : FClassFiles.toArray()) {
            if (((TClassFile) ClassFile).getThisClassName() == Name)
                return true;
        }

        return false;
    }

    private ArrayList<Object> DeObfuscateSingleFile(int index, RenameDatabase RenameStore) {
        TClassFile ClassFile = FClassFiles.get(index);

        if (ClassFile == null)
            return null;

        // add the class name to the head of the changelist
        FChangeList = new ArrayList<Object>();
        FChangeList.add(ClassFile.getThisClassName());

        String OriginalClassName = ClassFile.getThisClassName();
        String OriginalClassAndType = ClassFile.getThisClassName() + " : " + ClassFile.getSuperClassName();

        // rename the class and add the new class name to the changelist at [1]
        if (FRenameClasses && RenameStore.GetNewClassNameOnly(OriginalClassAndType) != null) {
            // check if we need to use a user-supplied class name first
            String NewClassName = RenameStore.GetNewClassNameOnly(OriginalClassAndType);

            while (ClassNameExists(NewClassName)) {
                NewClassName += "_";
            }
            FChangeList.add(ClassFile.changeClassName(NewClassName));
        } else if (FRenameClasses && DoRename(OriginalClassName)) {
            String NewClassName = "Class_" + Common.GetClassName(OriginalClassName);

            // test if the filename we are changing to hasnt already been used!
            while (ClassNameExists(NewClassName)) {
                NewClassName += "_";
            }
            FChangeList.add(ClassFile.changeClassName(NewClassName));
        } else
            FChangeList.add(OriginalClassName);

        // process the Methods
        for (int i = 0; i < ClassFile.getMethods().getItems().size(); i++) {
            MethodInfo mi = ClassFile.getMethods().getItems().get(i);
            RenameData rd = RenameStore.GetNewMethodInfo(OriginalClassAndType, mi.getDescriptor(), mi.getName().Value);

            // this is the rule for renaming
            if (DoRename(mi.getName().Value) || rd != null) {
                // clone the original method
                TMethodChangeRecord mcr = new TMethodChangeRecord(mi);
                // rename all of the functions something meaningful
                String NewName;
                // if the offset is zero, it probably means its an abstract
                // method
                if (ClassFile.getAccessFlags() == AccessFlags.ACC_INTERFACE)
                    NewName = String.format("sub_iface_%x", i);
                else if (mi.getOffset() != 0)
                    NewName = String.format("sub_%x", mi.getOffset());
                else
                    NewName = String.format("sub_null_%x", i);

//                 if (FThoroughMode) {
//                     int j = 0;
//                     while (ClassFile.getMethods().MethodNameExists(NewName)) { // rename the method
//                         NewName = NewName + "_" + j;
//                         j++;
//                     }
//                 }

                // user supplied names take precedence
                if (rd != null) {
                    NewName = rd.getFieldName();
                }

                // change the method name
                ClassFile.ChangeMethodName(i, NewName);
                // set the
                mcr.ChangedTo(mi);
                FChangeList.add(mcr);
            }

            // fix the descriptor regardless
            ClassFile.changeMethodParam(i, OriginalClassName, ClassFile.getThisClassName());
        }

        // process the Fields
        for (int i = 0; i < ClassFile.getFields().getItems().size(); i++) {
            FieldInfo fi = ClassFile.getFields().getItems().get(i);
            RenameData rd = RenameStore.GetNewFieldInfo(OriginalClassAndType, fi.getDescriptor(), fi.getName().Value);

            if (DoRename(fi.getName().Value) || rd != null) {
                // clone the original method
                TFieldChangeRecord fcr = new TFieldChangeRecord(fi);
                // rename all of the fields something meaningful
                String NewName;
                // if the offset is zero, it probably means its a null/abstract
                // method
                if (fi.getOffset() != 0)
                    NewName = String.format("var_%x", fi.getOffset());
                else
                    NewName = String.format("var_null_%x", fi.getOffset());

//                if (FThoroughMode) {
//                    int j = 0;
//                    while (ClassFile.getMethods().FieldNameExists(NewName)) { // rename the field
//                        NewName = NewName + "_" + j;
//                        j++;
//                    }
//                }

                if (rd != null) {
                    NewName = rd.getFieldName();
                }

                ClassFile.ChangeFieldName(i, NewName);

                fcr.ChangedTo(fi);
                FChangeList.add(fcr);
            }

            // fix the descriptor regardless
            ClassFile.ChangeFieldType(i, OriginalClassName, ClassFile.getThisClassName());
        }

        return FChangeList;
    }

    /**
     * This function runs over a class, fixing up any references from a
     * deobfuscated file.
     *
     * @param Index This is the index of the ClassFile to have its
     * references updated
     * @param ChangeList This is a list of before/after values from a
     * previously deobfuscated file
     */
    private void FixReferencePass1(int Index, ArrayList<Object> ChangeList, ArrayList<Object> OwnerChangeList) {
        // the first pass does the following: - replaces the Super Class name
        // (if it needs replacing) - replaces any constant method/field names
        // (if they need replacing) - replaces the class field names (if needed)
        // it does NOT change the original class name
        TClassFile ClassFile = FClassFiles.get(Index);

        if (ClassFile == null) {
            return;
        }

        // - ChangeList[0] is always a string, which is the parent name of the
        // deobfuscated class
        // - ChangeList[1] is always the deobfuscated (new) class name... yes i
        // know this is lame :P
        String OldParentName = (String) ChangeList.get(0);
        String NewParentName = (String) ChangeList.get(1);

        // check the Super class name if it needs renaming
        if (ClassFile.getSuperClassName() == OldParentName) {
            ClassFile.changeSuperClassName(NewParentName);
        }

        // loop through the constant pool for field/method references
        // check the parent of each, and if the parent is the class we have
        // just modified, try and match it to one of the changes
        // in the changearray
        for (int i = 0; i < ClassFile.getConstantPool().getMaxItems(); i++) {
            if (ClassFile.getConstantPool().getItem(i) instanceof ConstantPoolMethodInfo) {
                ConstantPoolMethodInfo ci = (ConstantPoolMethodInfo) ClassFile.getConstantPool().getItem(i);

                // check its parent
                if (ci.ParentClass.Name == OldParentName || ci.ParentClass.Name == NewParentName) {
                    // check the descriptor
                    // - for fields this is the field type
                    // - for methods this is the parameter list

                    // if parents are the same, check the name and descriptor
                    // against the list of originals
                    for (int j = 2; j < ChangeList.size(); j++) {
                        if ((ChangeList.get(j) instanceof TMethodChangeRecord) && (ci instanceof ConstantMethodrefInfo || ci instanceof ConstantInterfaceMethodrefInfo)) {
                            if (ci instanceof ConstantInterfaceMethodrefInfo) {
                                // handle interface references differently
                                TMethodChangeRecord mcr = (TMethodChangeRecord) ChangeList.get(j);

                                // if found update it to the overridden version
                                if (mcr.getOriginalMethod().getName().Value == ci.NameAndType.Name && mcr.getOriginalMethod().getDescriptor() == ci.NameAndType.Descriptor) {
                                    // find the overridden version
                                    for (int k = 2; k < OwnerChangeList.size(); k++) {
                                        if (OwnerChangeList.get(k) instanceof TMethodChangeRecord) {
                                            TMethodChangeRecord mcr2 = (TMethodChangeRecord) OwnerChangeList.get(k);
                                            if (mcr2.getOriginalMethod().getName().Value == mcr.getOriginalMethod().getName().Value && mcr2.getOriginalMethod().getDescriptor() == mcr.getOriginalMethod().getDescriptor()) {
                                                ClassFile.ChangeConstantFieldName(i, mcr2.getNewMethod().getName().Value);
                                                break;
                                            }
                                        }
                                    }
                                }
                            } else {
                                TMethodChangeRecord mcr = (TMethodChangeRecord) ChangeList.get(j);

                                // if found update it to the new version...
                                if (mcr.getOriginalMethod().getName().Value == ci.NameAndType.Name && mcr.getOriginalMethod().getDescriptor() == ci.NameAndType.Descriptor) {
                                    ClassFile.ChangeConstantFieldName(i, mcr.getNewMethod().getName().Value);
                                    break;
                                }
                            }
                        } else if ((ChangeList.get(j) instanceof TFieldChangeRecord) && (ci instanceof ConstantFieldrefInfo)) {
                            TFieldChangeRecord fcr = (TFieldChangeRecord) ChangeList.get(j);

                            // if found update it to the new version...
                            if (fcr.getOriginalField().getName().Value == ci.NameAndType.Name && fcr.getOriginalField().getDescriptor() == ci.NameAndType.Descriptor) {
                                ClassFile.ChangeConstantFieldName(i, fcr.getNewField().getName().Value);
                                break;
                            }
                        }
                    }
                }
            }
        }

        // also loop through the Fields array to change all the Types
        for (int i = 0; i < ClassFile.getFields().MaxItems(); i++) {
            ClassFile.ChangeFieldType(i, OldParentName, NewParentName);
        }
        // do the same for methods (fix the parameter list)
        for (int i = 0; i < ClassFile.getMethods().MaxItems(); i++) {
            ClassFile.changeMethodParam(i, OldParentName, NewParentName);
        }
        // and the same for all the interfaces
        for (int i = 0; i < ClassFile.getInterfaces().getItems().size(); i++) {
            if (ClassFile.getInterfaces().Item(i).getName() == OldParentName)
                ClassFile.changeInterfaceName(i, NewParentName);
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
     * @param Index">Index of the class file we want to update</param>
     * @param ChangeList">The array of changes we made deobfuscating a
     * file</param>
     */
    private void FixReferencePass2(int Index, ArrayList<Object> ChangeList) {
        TClassFile ClassFile = FClassFiles.get(Index);

        if (ClassFile == null)
            return;

        String OldParentName = (String) ChangeList.get(0);
        String NewParentName = (String) ChangeList.get(1);

        // iterate through the constant pool looking for class references
        // that match the old class name
        for (int i = 0; i < ClassFile.getConstantPool().getMaxItems(); i++) {
            if (ClassFile.getConstantPool().getItem(i) instanceof ConstantClassInfo) {
                ConstantClassInfo ci = (ConstantClassInfo) ClassFile.getConstantPool().getItem(i);

                // if we found a ClassInfo constant with the same name as the
                // old name
                if (ci.Name == OldParentName) {
                    // create a new UTF String constant
                    ConstantUtf8Info ui = new ConstantUtf8Info();
                    // set it to the new parent name
                    ui.SetName(NewParentName);
                    // add it to the constant pool
                    int index = ClassFile.getConstantPool().Add(ui);
                    // set our original ClassInfo constant's name to the newly
                    // added UTF String constant
                    ci.SetName(index, ClassFile.getConstantPool());
                }
                // special condition for array type references
                else if (ci.Name.indexOf("L" + OldParentName + ";") >= 0) {
                    // create a new UTF String constant
                    ConstantUtf8Info ui = new ConstantUtf8Info();
                    // set it to the new parent name
                    ui.SetName(ci.Name.replace("L" + OldParentName + ";", "L" + NewParentName + ";"));
                    // add it to the constant pool
                    int index = ClassFile.getConstantPool().Add(ui);
                    // set our original ClassInfo constant's name to the newly
                    // added UTF String constant
                    ci.SetName(index, ClassFile.getConstantPool());

                }
            } else if (ClassFile.getConstantPool().getItem(i) instanceof ConstantPoolMethodInfo) {
                // check the descriptor
                // - for fields this instanceof the field type
                // - for methods this instanceof the parameter list
                ClassFile.ChangeConstantFieldType(i, OldParentName, NewParentName);
            }
        }
    }

    private void FixReferences(ArrayList<ArrayList<Object>> MasterChangeList) {
        // loop through the change record's and apply them to each file
        // (except itself)
        for (int i = 0; i < FClassFiles.size(); i++) {
            for (int j = 0; j < MasterChangeList.size(); j++) {
                FixReferencePass1(i, MasterChangeList.get(j), MasterChangeList.get(i));
            }
        }

        for (int i = 0; i < FClassFiles.size(); i++) {
            for (int j = 0; j < MasterChangeList.size(); j++) {
                FixReferencePass2(i, MasterChangeList.get(j));
            }
        }
    }

    /**
     * Find the index of the parent of the classfile, if it exists in the
     * project.
     *
     * @param Index Index of class file to find parent of
     * @returns positive integer index if found, else -1 if not found
     */
    int FindParent(int Index) {
        String ParentName = FClassFiles.get(Index).getSuperClassName();

        for (int i = 0; i < FClassFiles.size(); i++) {
            if (i != Index && FClassFiles.get(i).getThisClassName() == ParentName) {
                return i;
            }
        }

        return -1;
    }

//     int FindClass(String ClassName) {
//        for (int i = 0; i < FClassFiles.size(); i++) {
//            if (((TClassFile) FClassFiles[i]).getThisClassName() == ClassName) {
//                return i;
//            }
//        }
//
//        return -1;
//    }

    int FindInterface(String ClassName) {
        for (int i = 0; i < FClassFiles.size(); i++) {
            if (FClassFiles.get(i).getAccessFlags() == AccessFlags.ACC_INTERFACE && FClassFiles.get(i).getThisClassName() == ClassName) {
                return i;
            }
        }

        return -1;
    }

    ArrayList<ArrayList<Object>> AddInheritance(int Index, ArrayList<ArrayList<Object>> MasterChangeList) {
        int Parent = FindParent(Index);

        if (Parent >= 0) {
            ArrayList<Object> OriginalChangeList = MasterChangeList.get(Index);
            ArrayList<Object> ParentChangeList = MasterChangeList.get(Parent);

            for (int i = 2; i < ParentChangeList.size(); i++) {
                // add the rest of the parent entries to the original
                OriginalChangeList.add(ParentChangeList.get(i));
            }

            // last of all, if the parent has another parent, recurse and do it
            // all again
            if (FindParent(Parent) >= 0) {
                MasterChangeList = AddInheritance(Parent, MasterChangeList);
            }
        }

        return MasterChangeList;
    }

    ArrayList<ArrayList<Object>> AddInterfaces(int Index, ArrayList<ArrayList<Object>> MasterChangeList) {
        // this needs to work differently to inheritance
        // it does the following:
        // 1. loop through each interface
        // 2. check the MasterChangeList for a matching interface
        // 3. if found, for all methods in the deobfuscated interface, find
        // corresponding entry in
        // current classes change list, and update it
        //   
        TClassFile ClassFile = FClassFiles.get(Index);

        // for each class file, check each of its interfaces
        for (int i = 0; i < ClassFile.getInterfaces().getItems().size(); i++) {
            // check each interface if it matches any deobfuscated
            // classfile/interface in the project
            for (int j = 0; j < FClassFiles.size(); j++) {
                String OldName = (String) MasterChangeList.get(j).get(0);

                if (OldName == ClassFile.getInterfaces().Item(i).getName()) {
                    ArrayList<Object> OriginalChangeList = MasterChangeList.get(Index);
                    ArrayList<Object> InterfaceChangeList = MasterChangeList.get(j);

                    for (int k = 2; k < InterfaceChangeList.size(); k++) {
                        // add the rest of the parent entries to the original
                        // NOTE: this might work best if added to the START of
                        // the list!
                        OriginalChangeList.set(2, InterfaceChangeList.get(k));
                    }

                    break;
                }
            }
        }

        return MasterChangeList;
    }

    ArrayList<ArrayList<Object>> FixInheritance(ArrayList<ArrayList<Object>> MasterChangeList) {
        for (int i = 0; i < FClassFiles.size(); i++) {
            MasterChangeList = AddInheritance(i, MasterChangeList);
            // MasterChangeList = AddInterfaces(i, MasterChangeList);
        }

        return MasterChangeList;
    }

    public ArrayList<File> DeObfuscateAll() {
        return DeObfuscateAll(null);
    }

    public ArrayList<File> DeObfuscateAll(RenameDatabase RenameStore) {
        FClassFiles = new ArrayList<TClassFile>();
//        FInterfaces = new ArrayList<TInterfaces>();
        ArrayList<ArrayList<Object>> MasterChangeList = new ArrayList<ArrayList<Object>>();
        ArrayList<File> NewFileNameList = new ArrayList<File>();
        int curr_progress = 0;

        Progress.setProgress(0);

        // open each class file and add to array
        for (File fn : FFiles) {
            TClassFile cf = new TClassFile(fn);

            if (cf != null) {
                if (cf.open()) {
                    FClassFiles.add(cf);

                    Progress.setProgress(++curr_progress);
                }
            }
        }

        // do all the work in memory
        for (int i = 0; i < FClassFiles.size(); i++) {
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
            MasterChangeList.add(DeObfuscateSingleFile(i, RenameStore));

            Progress.setProgress(i + 1);
        }

        Progress.setProgress(0);
        curr_progress = 0;

        // iterate through all the class files using the change records saved
        // after the deobfuscation was done
        MasterChangeList = FixInheritance(MasterChangeList);

        // iterate through all the class files using the change records saved
        // after the deobfuscation was done
        FixReferences(MasterChangeList);

        // save all the class files
        for (TClassFile cf : FClassFiles) {
            // extract the actual filename from the path and replace it with the
            // new ClassName
            String file_name = cf.getFile().getParent() + File.separator + Common.GetClassName(cf.getThisClassName()) + ".class";

            // file_name = file_name.Replace('/', '\\');

            if ((file_name != cf.getFile().getPath()) && FCleanup) {
                cf.getFile().delete();
            }

            // if for some reason the directory doesn't exist, create it
            if (!new File(file_name).getParentFile().exists())
                new File(file_name).getParentFile().mkdir();

            cf.Save(file_name);

            // return the new filename so the main gui knows what to reload
            NewFileNameList.add(new File(file_name));

            Progress.setProgress(++curr_progress);
        }

        return NewFileNameList;
    }

    public boolean getThoroughMode() {
        return FThoroughMode;
    }

    public void setThoroughMode(boolean value) {
        FThoroughMode = value;
    }

    public boolean getCleanup() {
        return FCleanup;
    }

    public void setCleanup(boolean value) {
        FCleanup = value;
    }

    public boolean getRenameClasses() {
        return FRenameClasses;
    }

    public void setRenameClasses(boolean value) {
        FRenameClasses = value;
    }
}

/**
 * These class encapsulates the java .class file With a few special methods
 * jammed in to help rename methods and fields (and refs)
 */
class TClassFile {
    // my internal variables
    String fThisClassName;

    String fSuperClassName;

    // internal class file members as designated by Sun
    private int FMagic;

    private int FMinorVersion;

    private int FMajorVersion;

    // private int FConstantPoolCount;
    private TConstantPool FConstantPool;

    private AccessFlags FAccessFlags;

    private int FThisClass;

    private int FSuperClass;

    // private int FInterfacesCount;
    private TInterfaces FInterfaces;

    // private int FFieldsCount;
    private TFields FFields;

    // private int FMethodsCount;
    private TMethods FMethods;

    // private int FAttributesCount;
    private TAttributes FAttributes;

    // internal variables
    private File FClassFile = null;

    private DataInput FReader = null;

    public TClassFile(File ClassFileName) {
        FClassFile = ClassFileName;
//        FHasBeenOpened = false;
        fThisClassName = "";
        fSuperClassName = "";
    }

    public boolean open() {
        if (FClassFile.exists()) {
            try {
                // read the .class file systematically
                InputStream fs = new FileInputStream(FClassFile);
                FReader = new DataInputStream(fs);
                Common.position = 0;
                // read header
                FMagic = Common.readInt(FReader);
System.err.printf("magic: %08x\n", FMagic);

                if (FMagic != 0x0CAFEBABE) {
                    return false;
                }

                FMinorVersion = Common.readShort(FReader);
System.err.printf("minorVersion: %04x\n", FMinorVersion);
                FMajorVersion = Common.readShort(FReader);
System.err.printf("majorVersion: %04x\n", FMajorVersion);
                // read constant pool
                // this also reads the "FConstantPoolCount"
                // so instead use FConstantPool.MaxItems or somesuch
                FConstantPool = new TConstantPool(FReader);
                // more constants
                FAccessFlags = AccessFlags.valueOf(Common.readShort(FReader));
                FThisClass = Common.readShort(FReader);
                FThisClass--;
System.err.printf("thisClass: %04x\n", FThisClass);
                FSuperClass = Common.readShort(FReader);
                FSuperClass--;
System.err.printf("superClass: %04x\n", FSuperClass);

                fThisClassName = ((ConstantClassInfo) FConstantPool.getItem(FThisClass)).Name;
                (FConstantPool.getItem(FThisClass)).references++;
                fSuperClassName = ((ConstantClassInfo) FConstantPool.getItem(FSuperClass)).Name;
                (FConstantPool.getItem(FSuperClass)).references++;

                FInterfaces = new TInterfaces(FReader, FConstantPool);
                FFields = new TFields(FReader, FConstantPool);
                FMethods = new TMethods(FReader, FConstantPool);
                FAttributes = new TAttributes(FReader, FConstantPool);

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

    public boolean Save(String FileName) {
//         if (true) { // FHasBeenOpened)

        try {
            // read the .class file systematically
            OutputStream fs = new FileOutputStream(FileName);
            DataOutput FWriter = new DataOutputStream(fs);
            Common.position = 0;
            // write header
            Common.writeInt(FWriter, FMagic);

            Common.writeShort(FWriter, FMinorVersion);
            Common.writeShort(FWriter, FMajorVersion);
            // write constant pool
            // this also writes the "FConstantPoolCount"
            FConstantPool.write(FWriter);
            // more constants
            Common.writeShort(FWriter, FAccessFlags.value);
            Common.writeShort(FWriter, FThisClass + 1);
            Common.writeShort(FWriter, FSuperClass + 1);

            FInterfaces.Write(FWriter);
            FFields.Write(FWriter);
            FMethods.Write(FWriter);
            FAttributes.Write(FWriter);

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
        return FMagic;
    }

    public void setMagic(int value) {
        FMagic = value;
    }

    public String Version() {
        return FMajorVersion + "." + FMinorVersion;
    }

    public File getFile() {
        return FClassFile;
    }

    public AccessFlags getAccessFlags() {
        return FAccessFlags;
    }

    public TConstantPool getConstantPool() {
        return FConstantPool;
    }

    public TInterfaces getInterfaces() {
        return FInterfaces;
    }

    public TFields getFields() {
        return FFields;
    }

    public TMethods getMethods() {
        return FMethods;
    }

    public TAttributes getAttributes() {
        return FAttributes;
    }

    public TChangeRecord ChangeMethodName(int MethodNumber, String NewName) {
        MethodInfo Method = FMethods.getItems().get(MethodNumber);
        // MethodInfo OriginalMethod = Method.Clone();
        // MethodInfo NewMethod = null;
        TChangeRecord Result = null;
        ConstantMethodrefInfo MethodRef = null;
        int NewNameIndex;

        // first we need to loop through the constant pool for method
        // references that match our new method name
        for (int i = 0; i < FConstantPool.getMaxItems(); i++) {
            if (FConstantPool.getItem(i).tag == (byte) ConstantPoolInfoTag.ConstantMethodref.value) {
                MethodRef = (ConstantMethodrefInfo) FConstantPool.getItem(i);
                if (MethodRef.ParentClass.Name == fThisClassName && MethodRef.NameAndType.Name == Method.getName().Value && MethodRef.NameAndType.Descriptor == Method.getDescriptor()) {
                    // jackpot, we found the reference!
                    // there should be only one, so we will break and fix it up
                    // after we generate the new name
                    break;
                }
            }

            MethodRef = null;
        }

        Method.getName().references--;
        // add a new String constant to the pool
        ConstantUtf8Info NewUtf = new ConstantUtf8Info(NewName);

        NewNameIndex = getConstantPool().Add(NewUtf);

        // set the method its new name
        Method.SetName(NewNameIndex, getConstantPool());
        Method.getName().references = 1;

        // NewMethod = Method.Clone();

        if (MethodRef == null)
            return Result;

        if (MethodRef.NameAndType.references <= 1) {
            // if this instanceof the only reference to the name/type descriptor
            // we can overwrite the value
            MethodRef.NameAndType.SetName(NewNameIndex, FConstantPool);
        } else {
            // we have to make a new one !
            MethodRef.NameAndType.references--;
            // add a new String constant to the pool
            ConstantNameAndTypeInfo NewNaT = new ConstantNameAndTypeInfo(NewNameIndex, MethodRef.NameAndType.getTypeIndex(), FConstantPool);

            int NewIndex = getConstantPool().Add(NewNaT);

            // set the method its new name
            MethodRef.SetNameAndType(NewIndex, getConstantPool());
            MethodRef.NameAndType.references = 1;
        }

        return Result;
    }

    public TChangeRecord ChangeFieldName(int FieldNumber, String NewName) {
        FieldInfo Field = FFields.getItems().get(FieldNumber);
        // FieldInfo OriginalFieldInfo = Field.Clone();
        // FieldInfo NewField = null;
        TChangeRecord Result = null;
        ConstantFieldrefInfo FieldRef = null;
        int NewNameIndex;

        // first we need to loop through the constant pool for method
        // references that match our new method name
        for (int i = 0; i < FConstantPool.getMaxItems(); i++) {
            if (FConstantPool.getItem(i).tag == (byte) ConstantPoolInfoTag.ConstantFieldref.value) {
                FieldRef = (ConstantFieldrefInfo) FConstantPool.getItem(i);
                if (FieldRef.ParentClass.Name == fThisClassName && FieldRef.NameAndType.Name == Field.getName().Value && FieldRef.NameAndType.Descriptor == Field.getDescriptor()) {
                    // jackpot, we found the reference!
                    // there should be only one, so we will break and fix it up
                    // after we generate the new name
                    break;
                }
            }

            FieldRef = null;
        }

        Field.getName().references--;

        // add a new String constant to the pool
        ConstantUtf8Info NewUtf = new ConstantUtf8Info(NewName);

        NewNameIndex = getConstantPool().Add(NewUtf);

        // set the method its new name
        Field.SetName(NewNameIndex, getConstantPool());
        Field.getName().references = 1;

        // NewField = Field.Clone();

        if (FieldRef == null) {
            return Result;
        }

        if (FieldRef.NameAndType.references <= 1) {
            // if this instanceof the only reference to the name/type descriptor
            // we can overwrite the value
            FieldRef.NameAndType.SetName(NewNameIndex, FConstantPool);
        } else {
            // we have to make a new one !
            FieldRef.NameAndType.references--;
            // add a new String constant to the pool
            ConstantNameAndTypeInfo NewNaT = new ConstantNameAndTypeInfo(NewNameIndex, FieldRef.NameAndType.getTypeIndex(), FConstantPool);

            int NewIndex = getConstantPool().Add(NewNaT);

            // set the method its new name
            FieldRef.SetNameAndType(NewIndex, getConstantPool());
            FieldRef.NameAndType.references = 1;
        }

        return Result;
    }

    public void ChangeConstantFieldName(int FieldNumber, String NewName) {
        // takes an index into the constantpool
        // simple changes the name of a method/field in the constant pool
        // always create new name
        // TODO: check this!

        ConstantPoolMethodInfo FieldRef = (ConstantPoolMethodInfo) FConstantPool.getItem(FieldNumber);

        ConstantUtf8Info NewNameString = new ConstantUtf8Info(NewName);
        int NewNameIndex = FConstantPool.Add(NewNameString);

        // we have to make a new one !
        FieldRef.NameAndType.references--;
        // add a new String constant to the pool
        ConstantNameAndTypeInfo NewNaT = new ConstantNameAndTypeInfo(NewNameIndex, FieldRef.NameAndType.getTypeIndex(), FConstantPool);

        int NewIndex = FConstantPool.Add(NewNaT);

        // set the method its new name
        FieldRef.SetNameAndType(NewIndex, FConstantPool);
        FieldRef.NameAndType.references = 1;
    }

    public void ChangeConstantFieldParent(int FieldNumber, int ParentNumber) {
        ConstantPoolMethodInfo FieldRef = (ConstantPoolMethodInfo) FConstantPool.getItem(FieldNumber);

        FieldRef.ParentClass.references--;
        FieldRef.SetParent(ParentNumber, FConstantPool);
    }

    public void ChangeConstantFieldType(int FieldNumber, String OldParentName, String NewParentName) {
        // takes an index into the constantpool
        // simple changes the name of a method/field in the constant pool
        // always create new name
        // TODO: check this!

        ConstantPoolMethodInfo FieldRef = (ConstantPoolMethodInfo) FConstantPool.getItem(FieldNumber);
        String OldName = FieldRef.NameAndType.Descriptor;
        String NewName = Common.FixDescriptor(FieldRef.NameAndType.Descriptor, OldParentName, NewParentName);

        if (OldName == NewName)
            return;

        ConstantUtf8Info NewTypeString = new ConstantUtf8Info(NewName);
        int NewTypeIndex = FConstantPool.Add(NewTypeString);

        FieldRef.NameAndType.SetType(NewTypeIndex, FConstantPool);
    }

    public void ChangeFieldType(int FieldNumber, String OldParentName, String NewParentName) {
        // takes an index into the constantpool
        // simple changes the name of a method/field in the constant pool
        // TODO: check this!

        FieldInfo FieldRef = FFields.Item(FieldNumber);

        String OldName = FieldRef.getDescriptor();
        String NewName = Common.FixDescriptor(FieldRef.getDescriptor(), OldParentName, NewParentName);

        if (OldName == NewName)
            return;

        ConstantUtf8Info NewTypeString = new ConstantUtf8Info(NewName);
        int NewTypeIndex = FConstantPool.Add(NewTypeString);

        // set the method its new name
        FieldRef.SetType(NewTypeIndex, FConstantPool);
    }

    public void changeMethodParam(int methodNumber, String oldParentName, String newParentName) {
        // takes an index into the constantpool
        // simple changes the name of a method/field in the constant pool
        // TODO: check this!

        MethodInfo methodRef = FMethods.Item(methodNumber);

        String oldName = methodRef.getDescriptor();
        String newName = Common.FixDescriptor(methodRef.getDescriptor(), oldParentName, newParentName);

        if (oldName == newName)
            return;

        ConstantUtf8Info newTypeString = new ConstantUtf8Info(newName);
        int newTypeIndex = FConstantPool.Add(newTypeString);

        // set the method its new name
        methodRef.SetType(newTypeIndex, FConstantPool);
    }

    public void changeInterfaceName(int InterfaceNumber, String NewName) {
        // takes an index into the interface list
        // simple changes the name of a method/field in the constant pool
        // TODO: check this!

        InterfaceInfo intInfo = FInterfaces.Item(InterfaceNumber);

        if (intInfo.getName() == NewName)
            return;

        ConstantUtf8Info newTypeString = new ConstantUtf8Info(NewName);
        int newTypeIndex = FConstantPool.Add(newTypeString);

        // set the interface its new name
        ConstantClassInfo cci = (ConstantClassInfo) getConstantPool().getItem(intInfo.getValue());
        cci.SetName(newTypeIndex, FConstantPool);
    }

    public String getThisClassName() {
        return fThisClassName;
    }

    public String getSuperClassName() {
        return fSuperClassName;
    }

    public String changeClassName(String Name) {
        ConstantClassInfo ClassInfo = (ConstantClassInfo) FConstantPool.getItem(FThisClass);
        ConstantUtf8Info UtfInfo = (ConstantUtf8Info) FConstantPool.getItem(ClassInfo.NameIndex);

        // change the class name, not the directory structure
        Name = Common.NewClassName(getThisClassName(), Name);

        // we have to make a new one !
        UtfInfo.references--;
        // add a new String constant to the pool
        ConstantUtf8Info NewUtf = new ConstantUtf8Info(Name);

        int NewIndex = getConstantPool().Add(NewUtf);

        // set the method its new name
        ClassInfo.SetName(NewIndex, FConstantPool);
        NewUtf.references = 1;

        fThisClassName = ((ConstantClassInfo) FConstantPool.getItem(FThisClass)).Name;

        return Name;
    }

    public int changeSuperClassName(String newName) {
        ConstantClassInfo ClassInfo = (ConstantClassInfo) FConstantPool.getItem(FSuperClass);
        ConstantUtf8Info UtfInfo = (ConstantUtf8Info) FConstantPool.getItem(ClassInfo.NameIndex);

        // skip this coz we already passing the full name in
        // NewName = Common.NewClassName(FSuperClassName, NewName);

        if (UtfInfo.references <= 1) {
            // if this is the only reference to the name/type descriptor
            // we can overwrite the value
            UtfInfo.SetName(newName);
        } else {
            // we have to make a new one !
            UtfInfo.references--;
            // add a new String constant to the pool
            ConstantUtf8Info NewUtf = new ConstantUtf8Info(newName);

            int NewIndex = getConstantPool().Add(NewUtf);

            // set the method its new name
            ClassInfo.NameIndex = NewIndex;
            NewUtf.references = 1;
        }

        fSuperClassName = ((ConstantClassInfo) FConstantPool.getItem(FSuperClass)).Name;

        return FSuperClass;
    }

    public int AddConstantClassName(String NewName) {
        ConstantClassInfo ClassInfo = new ConstantClassInfo();
        ConstantUtf8Info UtfInfo = new ConstantUtf8Info();

        int NewClassIndex = FConstantPool.Add(ClassInfo);
        int NewUtfIndex = FConstantPool.Add(UtfInfo);

        UtfInfo.SetName(NewName);
        ClassInfo.SetName(NewUtfIndex, FConstantPool);

        return NewClassIndex;
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

abstract class TChangeRecord {
}

class TMethodChangeRecord extends TChangeRecord {
    // just a simple class to hold the information temporarily
    private MethodInfo FOriginalMethod;

    private MethodInfo FNewMethod;

    public TMethodChangeRecord(MethodInfo Original) {
        FOriginalMethod = (MethodInfo) Original.clone();
    }

    public void ChangedTo(MethodInfo New) {
        FNewMethod = (MethodInfo) New.clone();
    }

    public MethodInfo getOriginalMethod() {

        return FOriginalMethod;
    }

    public MethodInfo getNewMethod() {
        return FNewMethod;
    }
}

class TFieldChangeRecord extends TChangeRecord {
    // just a simple class to hold the information temporarily
    private FieldInfo FOriginalField;

    private FieldInfo FNewField;

    public TFieldChangeRecord(FieldInfo Original) {
        FOriginalField = (FieldInfo) Original.clone();
    }

    public void ChangedTo(FieldInfo New) {
        FNewField = (FieldInfo) New.clone();
    }

    public FieldInfo getOriginalField() {

        return FOriginalField;
    }

    public FieldInfo getNewField() {
        return FNewField;
    }
}

//
// INDIVIDUAL CLASSES
//
// These are all used by TClassFile to import each of its major sections
//

class TConstantPool {
    DataInput FReader;

    ArrayList<ConstantPoolInfo> fItems = null;

    int fMaxItems = 0;

    public TConstantPool(DataInput Reader) throws IOException {
        FReader = Reader;

        fMaxItems = Common.readShort(FReader) - 1;
System.err.printf("maxItems: %d\n", fMaxItems);
        fItems = new ArrayList<ConstantPoolInfo>();
        int count = 0;

        // goes from 1 -> constantpoolcount - 1
        while (count < fMaxItems) {
            int tag = Common.readByte(FReader);
System.err.printf("tag: %d\n", tag);

            switch (ConstantPoolInfoTag.valueOf(tag)) {
            case ConstantClass: {
                ConstantClassInfo cc = new ConstantClassInfo();
                cc.read(tag, FReader);
                fItems.add(cc);
                break;
            }
            case ConstantString: {
                ConstantStringInfo cc = new ConstantStringInfo();
                cc.read(tag, FReader);
                fItems.add(cc);
                break;
            }
            case ConstantFieldref: {
                ConstantFieldrefInfo cc = new ConstantFieldrefInfo();
                cc.read(tag, FReader);
                fItems.add(cc);
                break;
            }
            case ConstantMethodref: {
                ConstantMethodrefInfo cc = new ConstantMethodrefInfo();
                cc.read(tag, FReader);
                fItems.add(cc);
                break;
            }
            case ConstantInterfaceMethodref: {
                ConstantInterfaceMethodrefInfo cc = new ConstantInterfaceMethodrefInfo();
                cc.read(tag, FReader);
                fItems.add(cc);
                break;
            }
            case ConstantInteger: {
                ConstantIntegerInfo cc = new ConstantIntegerInfo();
                cc.read(tag, FReader);
                fItems.add(cc);
                break;
            }
            case ConstantFloat: {
                ConstantFloatInfo cc = new ConstantFloatInfo();
                cc.read(tag, FReader);
                fItems.add(cc);
                break;
            }
            case ConstantLong: {
                ConstantLongInfo cc = new ConstantLongInfo();
                cc.read(tag, FReader);
                fItems.add(cc);
                // longs take up two entries in the pool table
                count++;
                fItems.add(cc);
                break;
            }
            case ConstantDouble: {
                ConstantDoubleInfo cc = new ConstantDoubleInfo();
                cc.read(tag, FReader);
                fItems.add(cc);
                // so do doubles
                count++;
                fItems.add(cc);
                break;
            }
            case ConstantNameAndType: {
                ConstantNameAndTypeInfo cc = new ConstantNameAndTypeInfo();
                cc.read(tag, FReader);
                fItems.add(cc);
                break;
            }
            case ConstantUtf8: {
                ConstantUtf8Info cc = new ConstantUtf8Info();
                cc.read(tag, FReader);
                fItems.add(cc);
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

        for (ConstantPoolInfo cc : fItems) {
            cc.resolve(fItems);
        }
    }

    public void write(DataOutput writer) throws IOException {
        // i am assuming we have a valid constant pool list...
        // i dont do any error checking here except bare minimum!

        // write the number of constant pool entries
        Common.writeShort(writer, fMaxItems + 1);
        int count = 0;

        // goes from 1 -> constantpoolcount - 1
        while (count < fMaxItems) {
            ConstantPoolInfo Item = fItems.get(count);

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
        return fMaxItems;
    }

    public ConstantPoolInfo getItem(int index) {
        if (fItems != null && index < fMaxItems) {
            return fItems.get(index);
        }
System.err.printf("index: %04x\n", index);
new Exception("*** DUMMY ***").printStackTrace(System.err);
        return null;
    }

    public int Add(ConstantPoolInfo NewItem) {
        fItems.add(NewItem);
        fMaxItems++;
        return (fItems.size() - 1);
    }
}

class TInterfaces {
    DataInput FReader;

    ArrayList<InterfaceInfo> FItems = null;

    int FMaxItems = 0;

    public TInterfaces(DataInput Reader, TConstantPool ConstantPool) throws IOException {
        FReader = Reader;

        FMaxItems = Common.readShort(FReader) - 1;
        FItems = new ArrayList<InterfaceInfo>();
        int count = 0;

        // goes from 1 -> interfacecount - 1
        while (count <= FMaxItems) {
            InterfaceInfo ii = new InterfaceInfo(FReader, ConstantPool);
            FItems.add(ii);

            count++;
        }
    }

    public void Write(DataOutput Writer) throws IOException {
        Common.writeShort(Writer, FMaxItems + 1);

        int count = 0;

        // goes from 1 -> interfacecount - 1
        while (count <= FMaxItems) {
            InterfaceInfo ii = FItems.get(count);
            ii.write(Writer);

            count++;
        }
    }

    public int MaxItems() {
        return FMaxItems;
    }

    public InterfaceInfo Item(int Index) {
        if (Index >= 0 && Index < FItems.size())
            return FItems.get(Index);

        // TODO: fix this fucking gay piece of shit
        return FItems.get(0);
    }

    public ArrayList<InterfaceInfo> getItems() {
        return FItems;
    }
}

class TFields {
    DataInput FReader;

    ArrayList<FieldInfo> FItems = null;

    int FMaxItems = 0;

    public TFields(DataInput Reader, TConstantPool ConstantPool) throws IOException {
        FReader = Reader;

        FMaxItems = Common.readShort(FReader);
        FItems = new ArrayList<FieldInfo>();
        int count = 0;

        // goes from 1 -> fieldcount - 1
        while (count < FMaxItems) {
            FieldInfo fi = new FieldInfo(FReader, ConstantPool);
            FItems.add(fi);

            count++;
        }
    }

    public void Write(DataOutput Writer) throws IOException {
        Common.writeShort(Writer, FMaxItems);

        int count = 0;

        // goes from 1 -> fieldcount - 1
        while (count < FMaxItems) {
            FieldInfo fi = FItems.get(count);
            fi.write(Writer);

            count++;
        }
    }

    public int MaxItems() {
        return FMaxItems;
    }

    public FieldInfo Item(int Index) {
        if (FItems != null && Index < FMaxItems)
            return FItems.get(Index);

        return null;
    }

    public ArrayList<FieldInfo> getItems() {
        return FItems;
    }

    public boolean FieldNameExists(String Name) {
        for (int i = 0; i < FMaxItems; i++) {
            if (Name == FItems.get(i).getName().Value)
                return true;
        }

        return false;
    }
}

class TMethods {
    DataInput FReader;

    ArrayList<MethodInfo> FItems = null;

    int FMaxItems = 0;

    public TMethods(DataInput Reader, TConstantPool ConstantPool) throws IOException {
        FReader = Reader;

        FMaxItems = Common.readShort(FReader);
        FItems = new ArrayList<MethodInfo>();
        int count = 0;

        // goes from 1 -> fieldcount - 1
        while (count < FMaxItems) {
            MethodInfo mi = new MethodInfo(FReader, ConstantPool);
            FItems.add(mi);

            count++;
        }
    }

    public void Write(DataOutput Writer) throws IOException {
        Common.writeShort(Writer, FMaxItems);

        int count = 0;

        // goes from 1 -> fieldcount - 1
        while (count < FMaxItems) {
            MethodInfo mi = FItems.get(count);
            mi.write(Writer);

            count++;
        }
    }

    public int MaxItems() {
        return FMaxItems;
    }

    public MethodInfo Item(int Index) {
        if (FItems != null && Index < FMaxItems)
            return FItems.get(Index);

        return null;
    }

    public ArrayList<MethodInfo> getItems() {
        return FItems;
    }

    public boolean MethodNameExists(String Name) {
        for (int i = 0; i < FMaxItems; i++) {
            if (Name == FItems.get(i).getName().Value)
                return true;
        }

        return false;
    }

}

class TAttributes {
    DataInput FReader;

    ArrayList<Object> FItems = null;

    int FMaxItems = 0;

    public TAttributes(DataInput Reader, TConstantPool ConstantPool) throws IOException {
        FReader = Reader;

        FMaxItems = Common.readShort(FReader) - 1;
        FItems = new ArrayList<Object>();
        int count = 0;

        // goes from 1 -> attributescount - 1
        while (count <= FMaxItems) {
            int NameIndex = Common.readShort(FReader);
            NameIndex--;
            ConstantUtf8Info Name = (ConstantUtf8Info) ConstantPool.getItem(NameIndex);

            switch (AttributeType.valueOf(Name.Value)) {
            case Code: {
                CodeAttributeInfo ai = new CodeAttributeInfo(NameIndex, FReader, ConstantPool);

                FItems.add(ai);
                break;
            }
            case ConstantValue: {
                ConstantValueAttributeInfo ai = new ConstantValueAttributeInfo(NameIndex, FReader, ConstantPool);

                FItems.add(ai);
                break;
            }
            case Deprecated: {
                DeprecatedAttributeInfo ai = new DeprecatedAttributeInfo(NameIndex, FReader, ConstantPool);

                FItems.add(ai);
                break;
            }
            case Exceptions: {
                ExceptionsAttributeInfo ai = new ExceptionsAttributeInfo(NameIndex, FReader, ConstantPool);

                FItems.add(ai);
                break;
            }
            case InnerClasses: {
                InnerClassesAttributeInfo ai = new InnerClassesAttributeInfo(NameIndex, FReader, ConstantPool);

                FItems.add(ai);
                break;
            }
            case LineNumberTable: {
                LineNumberAttributeInfo ai = new LineNumberAttributeInfo(NameIndex, FReader, ConstantPool);

                FItems.add(ai);
                break;
            }
            case LocalVariableTable: {
                LocalVariablesAttributeInfo ai = new LocalVariablesAttributeInfo(NameIndex, FReader, ConstantPool);

                FItems.add(ai);
                break;
            }
            case SourceFile: {
                SourceFileAttributeInfo ai = new SourceFileAttributeInfo(NameIndex, FReader, ConstantPool);

                FItems.add(ai);
                break;
            }
            case Synthetic: {
                SyntheticAttributeInfo ai = new SyntheticAttributeInfo(NameIndex, FReader, ConstantPool);

                FItems.add(ai);
                break;
            }
            default: {
                AttributeInfo ai = new UnknownAttributeInfo(NameIndex, FReader, ConstantPool);

                FItems.add(ai);
                break;
            }
            }

            count++;
        }
    }

    public void Write(DataOutput Writer) throws IOException {
        Common.writeShort(Writer, FMaxItems + 1);

        int count = 0;

        // goes from 1 -> attributescount - 1
        while (count <= FMaxItems) {
            AttributeInfo Item = (AttributeInfo) FItems.get(count);

            Item.write(Writer);

            count++;
        }
    }

    public ArrayList<Object> getItems() {
        return FItems;
    }
}
