/*
 * https://github.com/fileoffset/JDO
 */

package vavi.util.deobfuscation.common.info;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import vavi.util.deobfuscation.common.Common;
import vavi.util.deobfuscation.common.constantpoolinfo.ConstantClassInfo;
import vavi.util.deobfuscation.objects.ConstantPool;

public class TException {
    int exceptionIndex;

    ConstantClassInfo Exception;

    public TException(DataInput reader, ConstantPool constantPool) {
        exceptionIndex = 0;

        try {
            exceptionIndex = Common.readShort(reader);
            exceptionIndex--;
            // resolve references
            Exception = (ConstantClassInfo) constantPool.getItem(exceptionIndex);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            Exception = null;
        }
    }

    public void write(DataOutput writer) throws IOException {
        Common.writeShort(writer, exceptionIndex + 1);
    }
}
