/*
 * https://github.com/fileoffset/JDO
 */

package vavi.util.deobfuscation.common;

import vavi.util.deobfuscation.common.constantpoolinfo.ConstantClassInfo;
import vavi.util.deobfuscation.common.constantpoolinfo.ConstantDoubleInfo;
import vavi.util.deobfuscation.common.constantpoolinfo.ConstantFieldrefInfo;
import vavi.util.deobfuscation.common.constantpoolinfo.ConstantFloatInfo;
import vavi.util.deobfuscation.common.constantpoolinfo.ConstantIntegerInfo;
import vavi.util.deobfuscation.common.constantpoolinfo.ConstantInterfaceMethodrefInfo;
import vavi.util.deobfuscation.common.constantpoolinfo.ConstantLongInfo;
import vavi.util.deobfuscation.common.constantpoolinfo.ConstantMethodrefInfo;
import vavi.util.deobfuscation.common.constantpoolinfo.ConstantNameAndTypeInfo;
import vavi.util.deobfuscation.common.constantpoolinfo.ConstantStringInfo;
import vavi.util.deobfuscation.common.constantpoolinfo.ConstantUtf8Info;

public enum ConstantPoolInfoTag {
    ConstantClass(7) {
        @Override
        public ConstantPoolInfo getConstantPoolInfo() {
            return new ConstantClassInfo();
        }
    },
    ConstantFieldref(9) {
        @Override
        public ConstantPoolInfo getConstantPoolInfo() {
            return new ConstantFieldrefInfo();
        }
    },
    ConstantMethodref(10) {
        @Override
        public ConstantPoolInfo getConstantPoolInfo() {
            return new ConstantMethodrefInfo();
        }
    },
    ConstantInterfaceMethodref(11) {
        @Override
        public ConstantPoolInfo getConstantPoolInfo() {
            return new ConstantInterfaceMethodrefInfo();
        }
    },
    ConstantString(8) {
        @Override
        public ConstantPoolInfo getConstantPoolInfo() {
            return new ConstantStringInfo();
        }
    },
    ConstantInteger(3) {
        @Override
        public ConstantPoolInfo getConstantPoolInfo() {
            return new ConstantIntegerInfo();
        }
    },
    ConstantFloat(4) {
        @Override
        public ConstantPoolInfo getConstantPoolInfo() {
            return new ConstantFloatInfo();
        }
    },
    ConstantLong(5) {
        @Override
        public ConstantPoolInfo getConstantPoolInfo() {
            return new ConstantLongInfo();
        }
    },
    ConstantDouble(6) {
        @Override
        public ConstantPoolInfo getConstantPoolInfo() {
            return new ConstantDoubleInfo();
        }
    },
    ConstantNameAndType(12) {
        @Override
        public ConstantPoolInfo getConstantPoolInfo() {
            return new ConstantNameAndTypeInfo();
        }
    },
    ConstantUtf8(1) {
        @Override
        public ConstantPoolInfo getConstantPoolInfo() {
            return new ConstantUtf8Info();
        }
    },
    ConstantUnknown(-1) {
        @Override
        public ConstantPoolInfo getConstantPoolInfo() {
            return null;
        }
    };
    public final int value;

    ConstantPoolInfoTag(int value) {
        this.value = value;
    }

    public static ConstantPoolInfoTag valueOf(int value) {
        for (ConstantPoolInfoTag accessFlag : values()) {
            if (accessFlag.value == value) {
                return accessFlag;
            }
        }
        return ConstantUnknown;
//        throw new IllegalArgumentException(Integer.toHexString(value));
    }

    public abstract ConstantPoolInfo getConstantPoolInfo();
}
