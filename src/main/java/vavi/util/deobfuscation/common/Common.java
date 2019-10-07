/*
 * https://github.com/fileoffset/JDO
 */

package vavi.util.deobfuscation.common;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;


public class Common {
    public static long position = 0;

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
//            String oldName = originalClassName.substring(originalClassName.lastIndexOf('/') + 1, originalClassName.length() - originalClassName.lastIndexOf('/') - 1);
            originalClassName = originalClassName.substring(0, originalClassName.lastIndexOf('/')) +
                                originalClassName.substring(originalClassName.length() - originalClassName.lastIndexOf('/'));
//            originalClassName += newName + oldName;
            originalClassName += newName;

            return originalClassName;
        }

//        return newName + originalClassName;
        return newName;
    }

    public static String fixDescriptor(String descriptor, String oldClassName, String newClassName) {
        return descriptor.replace("L" + oldClassName + ";", "L" + newClassName + ";");
    }

//    private static int swapBytes(int value) {
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
