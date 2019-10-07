/*
 * https://github.com/fileoffset/JDO
 */

package vavi.util.deobfuscation.common;

public enum AccessFlags {
    ACC_PUBLIC(0x0001),
    ACC_FINAL(0x0010),
    ACC_SUPER(0x0020),
    ACC_INTERFACE(0x0200),
    ACC_ABSTRACT(0x0400),
    ACC_UNKNOWN(-1);
    public int value;

    AccessFlags(int value) {
        this.value = value;
    }

    public static AccessFlags valueOf(int value) {
        for (AccessFlags accessFlag : values()) {
            if (accessFlag.value == value) {
                return accessFlag;
            }
        }
        return ACC_UNKNOWN;
//        throw new IllegalArgumentException(String.valueOf(value));
    }
}
