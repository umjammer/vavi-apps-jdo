/*
 * https://github.com/fileoffset/JDO
 */

package vavi.util.deobfuscation.common;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;

public abstract class ConstantPoolInfo {
    public int tag;

    public int references;

    public abstract void read(int tag, DataInput Reader) throws IOException;

    public abstract boolean resolve(List<?> FItems) throws IOException;

    public abstract void write(DataOutput writer) throws IOException;
}
