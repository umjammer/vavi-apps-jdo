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


public class ExceptionTable {
    int startPC;

    int endPC;

    int handlerPC;

    int catchType;

    ConstantClassInfo catch_;

    public ExceptionTable(DataInput reader, ConstantPool constantPool) {
        startPC = 0;
        endPC = 0;
        handlerPC = 0;
        catchType = 0;
        catch_ = null;

        try {
            startPC = Common.readShort(reader);
            startPC--;
            endPC = Common.readShort(reader);
            endPC--;
            handlerPC = Common.readShort(reader);
            handlerPC--;
            catchType = Common.readShort(reader);
            catchType--;

            if (catchType >= 0) {
                catch_ = (ConstantClassInfo) constantPool.getItem(catchType);
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    public void write(DataOutput writer) throws IOException {
        Common.writeShort(writer, startPC + 1);
        Common.writeShort(writer, endPC + 1);
        Common.writeShort(writer, handlerPC + 1);
        Common.writeShort(writer, catchType + 1);
    }
}
/* */
