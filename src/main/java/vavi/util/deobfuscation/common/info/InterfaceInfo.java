/*
 * https://github.com/fileoffset/JDO
 */

package vavi.util.deobfuscation.common.info;

import java.io.DataInput;
import java.io.DataOutput;

import vavi.util.deobfuscation.common.Common;
import vavi.util.deobfuscation.common.constantpoolinfo.ConstantClassInfo;
import vavi.util.deobfuscation.objects.ConstantPool;


public class InterfaceInfo {
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
