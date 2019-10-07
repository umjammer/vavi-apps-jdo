/*
 * https://github.com/fileoffset/JDO
 */

package vavi.util.deobfuscation.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class RenameDatabase {
    private Map<String, List<RenameData>> renameMethods = null;

    private Map<String, List<RenameData>> renameFields = null;

    private Map<String, String> renameClass = null;

    public RenameDatabase() {
        renameMethods = new HashMap<>();
        renameFields = new HashMap<>();
        renameClass = new HashMap<>();
    }

    public void addRename(Map<String, List<RenameData>> destTable,
                          String className,
                          String oldDescriptor,
                          String oldName,
                          String newDescriptor,
                          String newName) {
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

    public RenameData getRenameInfo(Map<String, List<RenameData>> destTable,
                                    String className,
                                    String oldDescriptor,
                                    String oldName) {
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
