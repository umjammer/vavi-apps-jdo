/*
 * https://github.com/fileoffset/JDO
 */

package vavi.util.deobfuscation.common;

import java.io.DataOutput;
import java.io.IOException;


public abstract class AttributeInfo {
//     int attributeNameIndex;
//     ConstantUtf8Info attributeName;
//     int attributeLength;
//     byte[] bytes;

    public abstract void write(DataOutput writer) throws IOException;
}
