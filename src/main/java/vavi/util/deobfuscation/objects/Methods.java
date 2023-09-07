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
import vavi.util.deobfuscation.common.info.MethodInfo;


public class Methods {
    DataInput reader;

    List<MethodInfo> items = null;

    int maxItems = 0;

    public Methods(DataInput reader, ConstantPool constantPool) throws IOException {
        this.reader = reader;

        maxItems = Common.readShort(reader);
        items = new ArrayList<>();
        int count = 0;

        // goes from 1 -> fieldcount - 1
        while (count < maxItems) {
            MethodInfo mi = new MethodInfo(reader, constantPool);
            items.add(mi);

            count++;
        }
    }

    public void write(DataOutput writer) throws IOException {
        Common.writeShort(writer, maxItems);

        int count = 0;

        // goes from 1 -> fieldcount - 1
        while (count < maxItems) {
            MethodInfo mi = items.get(count);
            mi.write(writer);

            count++;
        }
    }

    public int maxItems() {
        return maxItems;
    }

    public MethodInfo item(int index) {
        if (items != null && index < maxItems)
            return items.get(index);

        return null;
    }

    public List<MethodInfo> getItems() {
        return items;
    }

    public boolean methodNameExists(String name) {
        for (int i = 0; i < maxItems; i++) {
            if (name.equals(items.get(i).getName().value))
                return true;
        }

        return false;
    }
}
