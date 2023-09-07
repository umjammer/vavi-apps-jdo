/*
 * https://github.com/fileoffset/JDO
 */

package vavi.util.deobfuscation.common;

enum FieldAccessFlags {
    /** Declared public; may be accessed from outside its package. */
    ACC_PUBLIC(0x0001),
    /** Declared private; usable only within the defining class. */
    ACC_PRIVATE(0x0002),
    /** Declared protected; may be accessed within subclasses. */
    ACC_PROTECTED(0x0004),
    /** Declared static. */
    ACC_STATIC(0x0008),
    /** Declared final; no further assignment after initialization. */
    ACC_FINAL(0x0010),
    /** Declared volatile; cannot be cached. */
    ACC_VOLATILE(0x0040),
    /**
     * Declared transient; not written or read by a persistent object manager.
     */
    ACC_TRANSIENT(0x0080);
    final int value;

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
/* */
