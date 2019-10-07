/*
 * https://github.com/fileoffset/JDO
 */

package vavi.util.deobfuscation.common.info;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import vavi.util.deobfuscation.common.Common;
import vavi.util.deobfuscation.common.constantpoolinfo.ConstantClassInfo;
import vavi.util.deobfuscation.common.constantpoolinfo.ConstantUtf8Info;
import vavi.util.deobfuscation.objects.ConstantPool;


public class TClasses {
    int innerClassInfoIndex;

    ConstantClassInfo innerClassInfo;

    int outerClassInfoIndex;

    ConstantClassInfo outerClassInfo;

    int innerNameIndex;

    ConstantUtf8Info innerName;

    int innerClassAccessFlags;

    public TClasses(DataInput reader, ConstantPool constantPool) {
        innerClassInfoIndex = 0;
        innerClassInfo = null;
        outerClassInfoIndex = 0;

        outerClassInfo = null;
        innerNameIndex = 0;

        innerName = null;
        innerClassAccessFlags = 0;

        try {
            innerClassInfoIndex = Common.readShort(reader);
            innerClassInfoIndex--;
            outerClassInfoIndex = Common.readShort(reader);
            outerClassInfoIndex--;
            innerNameIndex = Common.readShort(reader);
            innerNameIndex--;
            innerClassAccessFlags = Common.readShort(reader);

            // resolve references
            if (innerNameIndex >= 0) {
                innerName = (ConstantUtf8Info) constantPool.getItem(innerNameIndex);
                innerName.references++;
            }
            if (innerNameIndex >= 0) {
                innerClassInfo = (ConstantClassInfo) constantPool.getItem(innerClassInfoIndex);
                innerClassInfo.references++;
            }
            if (innerNameIndex >= 0) {
                outerClassInfo = (ConstantClassInfo) constantPool.getItem(outerClassInfoIndex);
                outerClassInfo.references++;
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            // do nothing
        }
    }

    public void write(DataOutput writer) throws IOException {
        Common.writeShort(writer, innerClassInfoIndex + 1);
        Common.writeShort(writer, outerClassInfoIndex + 1);
        Common.writeShort(writer, innerNameIndex + 1);
        Common.writeShort(writer, innerClassAccessFlags);
    }
}
