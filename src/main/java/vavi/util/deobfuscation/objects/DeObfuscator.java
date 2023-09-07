/*
 * https://github.com/fileoffset/JDO
 */

package vavi.util.deobfuscation.objects;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import vavi.util.deobfuscation.Progressive;
import vavi.util.deobfuscation.common.AccessFlags;
import vavi.util.deobfuscation.common.Common;
import vavi.util.deobfuscation.common.RenameData;
import vavi.util.deobfuscation.common.RenameDatabase;
import vavi.util.deobfuscation.common.constantpoolinfo.ConstantClassInfo;
import vavi.util.deobfuscation.common.constantpoolinfo.ConstantFieldrefInfo;
import vavi.util.deobfuscation.common.constantpoolinfo.ConstantInterfaceMethodrefInfo;
import vavi.util.deobfuscation.common.constantpoolinfo.ConstantMethodrefInfo;
import vavi.util.deobfuscation.common.constantpoolinfo.ConstantPoolMethodInfo;
import vavi.util.deobfuscation.common.constantpoolinfo.ConstantUtf8Info;
import vavi.util.deobfuscation.common.info.FieldInfo;
import vavi.util.deobfuscation.common.info.MethodInfo;


/**
 * These class does the actual deobfuscation
 */
public class DeObfuscator {

    /** event delegates */
    private Progressive progressive;

    public void setProgressive(Progressive progressive) {
        this.progressive = progressive;
    }

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
     * @param files All the files in the project. Must be full path
     *            + filename
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

        if (!name.isEmpty() && name.length() <= 2)
            return true;

        if (!name.isEmpty() && name.length() <= 3 && name.indexOf("$") > 0)
            return true;

        for (String s : bad_names) {
            if (s.equals(name))
                return true;
        }

        return false;
    }

    private boolean classNameExists(String name) {
        for (Object classFile : classFiles.toArray()) {
            if (((ClassFile) classFile).getThisClassName().equals(name))
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

            // test if the filename we are changing to hasn't already been used!
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
                // rename all the functions something meaningful
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
     *            references updated
     * @param changeList This is a list of before/after values from a
     *            previously deobfuscated file
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
        if (classFile.getSuperClassName().equals(oldParentName)) {
            classFile.changeSuperClassName(newParentName);
        }

        // loop through the constant pool for field/method references
        // check the parent of each, and if the parent is the class we have
        // just modified, try and match it to one of the changes
        // in the changearray
        for (int i = 0; i < classFile.getConstantPool().getMaxItems(); i++) {
            if (classFile.getConstantPool().getItem(i) instanceof ConstantPoolMethodInfo ci) {

                // check its parent
                if (ci.parentClass.name.equals(oldParentName) || ci.parentClass.name.equals(newParentName)) {
                    // check the descriptor
                    // - for fields this is the field type
                    // - for methods this is the parameter list

                    // if parents are the same, check the name and descriptor
                    // against the list of originals
                    for (int j = 2; j < changeList.size(); j++) {
                        if ((changeList.get(j) instanceof MethodChangeRecord) &&
                            (ci instanceof ConstantMethodrefInfo || ci instanceof ConstantInterfaceMethodrefInfo)) {
                            if (ci instanceof ConstantInterfaceMethodrefInfo) {
                                // handle interface references differently
                                MethodChangeRecord mcr = (MethodChangeRecord) changeList.get(j);

                                // if found update it to the overridden version
                                if (mcr.getOriginalMethod().getName().value.equals(ci.nameAndType.name) &&
                                        mcr.getOriginalMethod().getDescriptor().equals(ci.nameAndType.descriptor)) {
                                    // find the overridden version
                                    for (int k = 2; k < ownerChangeList.size(); k++) {
                                        if (ownerChangeList.get(k) instanceof MethodChangeRecord mcr2) {
                                            if (mcr2.getOriginalMethod().getName().value.equals(mcr.getOriginalMethod()
                                                    .getName().value) &&
                                                    mcr2.getOriginalMethod().getDescriptor().equals(mcr.getOriginalMethod()
                                                            .getDescriptor())) {
                                                classFile.changeConstantFieldName(i, mcr2.getNewMethod().getName().value);
                                                break;
                                            }
                                        }
                                    }
                                }
                            } else {
                                MethodChangeRecord mcr = (MethodChangeRecord) changeList.get(j);

                                // if found update it to the new version...
                                if (mcr.getOriginalMethod().getName().value.equals(ci.nameAndType.name) &&
                                        mcr.getOriginalMethod().getDescriptor().equals(ci.nameAndType.descriptor)) {
                                    classFile.changeConstantFieldName(i, mcr.getNewMethod().getName().value);
                                    break;
                                }
                            }
                        } else if ((changeList.get(j) instanceof FieldChangeRecord fcr) && (ci instanceof ConstantFieldrefInfo)) {

                            // if found update it to the new version...
                            if (fcr.getOriginalField().getName().value.equals(ci.nameAndType.name) &&
                                    fcr.getOriginalField().getDescriptor().equals(ci.nameAndType.descriptor)) {
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
            if (classFile.getInterfaces().item(i).getName().equals(oldParentName))
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
     * @param index_ Index of the class file we want to update
     * @param changeList The array of changes we made deobfuscating a
     *            file
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
            if (classFile.getConstantPool().getItem(i) instanceof ConstantClassInfo ci) {

                // if we found a ClassInfo constant with the same name as the
                // old name
                if (ci.name.equals(oldParentName)) {
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
                else if (ci.name.contains("L" + oldParentName + ";")) {
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
            for (List<Object> objects : masterChangeList) {
                fixReferencePass1(i, objects, masterChangeList.get(i));
            }
        }

        for (int i = 0; i < classFiles.size(); i++) {
            for (List<Object> objects : masterChangeList) {
                fixReferencePass2(i, objects);
            }
        }
    }

    /**
     * Find the index of the parent of the classfile, if it exists in the
     * project.
     *
     * @param index Index of class file to find parent of
     * @return positive integer index if found, else -1 if not found
     */
    int findParent(int index) {
        String ParentName = classFiles.get(index).getSuperClassName();

        for (int i = 0; i < classFiles.size(); i++) {
            if (i != index && classFiles.get(i).getThisClassName().equals(ParentName)) {
                return i;
            }
        }

        return -1;
    }

     int findClass(String className) {
        for (int i = 0; i < classFiles.size(); i++) {
            if (classFiles.get(i).getThisClassName().equals(className)) {
                return i;
            }
        }

        return -1;
    }

    int findInterface(String className) {
        for (int i = 0; i < classFiles.size(); i++) {
            if (classFiles.get(i).getAccessFlags() == AccessFlags.ACC_INTERFACE &&
                    classFiles.get(i).getThisClassName().equals(className)) {
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

                if (oldName.equals(classFile.getInterfaces().item(i).getName())) {
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

        progressive.setProgress(0);

        // open each class file and add to array
        for (File fn : files) {
            ClassFile cf = new ClassFile(fn);

            if (cf != null) {
                if (cf.open()) {
                    classFiles.add(cf);

                    progressive.setProgress(++curr_progress);
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

            progressive.setProgress(i + 1);
        }

        progressive.setProgress(0);
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
            String file_name = cf.getFile().getParent() + File.separator + Common.getClassName(cf.getThisClassName()) +
                               ".class";

            // file_name = file_name.Replace('/', '\\');

            if ((!file_name.equals(cf.getFile().getPath())) && cleanup) {
                cf.getFile().delete();
            }

            // if for some reason the directory doesn't exist, create it
            if (!new File(file_name).getParentFile().exists())
                new File(file_name).getParentFile().mkdir();

            cf.save(file_name);

            // return the new filename so the main gui knows what to reload
            newFileNameList.add(new File(file_name));

            progressive.setProgress(++curr_progress);
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
