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
import vavi.util.deobfuscation.common.info.InterfaceInfo;


public class Interfaces {
    DataInput reader;

    List<InterfaceInfo> items = null;

    int maxItems = 0;

    public Interfaces(DataInput reader, ConstantPool constantPool) throws IOException {
        this.reader = reader;

        maxItems = Common.readShort(reader) - 1;
        items = new ArrayList<>();
        int count = 0;

        // goes from 1 -> interfacecount - 1
        while (count <= maxItems) {
            InterfaceInfo ii = new InterfaceInfo(reader, constantPool);
            items.add(ii);

            count++;
        }
    }

    public void write(DataOutput writer) throws IOException {
        Common.writeShort(writer, maxItems + 1);

        int count = 0;

        // goes from 1 -> interfacecount - 1
        while (count <= maxItems) {
            InterfaceInfo ii = items.get(count);
            ii.write(writer);

            count++;
        }
    }

    public int maxItems() {
        return maxItems;
    }

    public InterfaceInfo item(int index) {
        if (index >= 0 && index < items.size())
            return items.get(index);

        // TODO: fix this fucking gay piece of shit
        return items.get(0);
    }

    public List<InterfaceInfo> getItems() {
        return items;
    }
}
