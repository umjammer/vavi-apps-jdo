/*
 * https://github.com/fileoffset/JDO
 */

package vavi.util.deobfuscation.objects;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import vavi.util.deobfuscation.common.Common;
import vavi.util.deobfuscation.common.info.FieldInfo;


public class Fields {
    DataInput reader;

    List<FieldInfo> items = null;

    int maxItems = 0;

    public Fields(DataInput reader, ConstantPool constantPool) throws IOException {
        this.reader = reader;

        maxItems = Common.readShort(reader);
        items = new ArrayList<>();
        int count = 0;

        // goes from 1 -> fieldcount - 1
        while (count < maxItems) {
            FieldInfo fi = new FieldInfo(reader, constantPool);
            items.add(fi);

            count++;
        }
    }

    public void write(DataOutput writer) throws IOException {
        Common.writeShort(writer, maxItems);

        int count = 0;

        // goes from 1 -> fieldcount - 1
        while (count < maxItems) {
            FieldInfo fi = items.get(count);
            fi.write(writer);

            count++;
        }
    }

    public int maxItems() {
        return maxItems;
    }

    public FieldInfo Item(int Index) {
        if (items != null && Index < maxItems)
            return items.get(Index);

        return null;
    }

    public List<FieldInfo> getItems() {
        return items;
    }

    public boolean fieldNameExists(String name) {
        for (int i = 0; i < maxItems; i++) {
            if (name.equals(items.get(i).getName().value))
                return true;
        }

        return false;
    }
}
