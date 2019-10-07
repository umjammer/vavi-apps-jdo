/*
 * https://github.com/fileoffset/JDO
 */

package vavi.util.deobfuscation.common.info;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import vavi.util.deobfuscation.common.Common;


public class TLineNumberTable {
    int startPC;

    int lineNumber;

    public TLineNumberTable(DataInput reader) {
        lineNumber = 0;
        startPC = 0;

        try {
            startPC = Common.readShort(reader);
            lineNumber = Common.readShort(reader);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            // do nothing
        }
    }

    public void write(DataOutput writer) throws IOException {
        Common.writeShort(writer, startPC);
        Common.writeShort(writer, lineNumber);
    }
}
/* */
