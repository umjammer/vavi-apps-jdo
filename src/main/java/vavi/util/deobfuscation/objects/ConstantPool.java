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
import vavi.util.deobfuscation.common.ConstantPoolInfo;
import vavi.util.deobfuscation.common.ConstantPoolInfoTag;


public class ConstantPool {
    DataInput reader;

    List<ConstantPoolInfo> items = null;

    int maxItems = 0;

    public ConstantPool(DataInput reader) throws IOException {
        this.reader = reader;

        maxItems = Common.readShort(reader) - 1;
        System.err.printf("maxItems: %d\n", maxItems);
        items = new ArrayList<>();
        int count = 0;

        // goes from 1 -> constantpoolcount - 1
        while (count < maxItems) {
            int tag = Common.readByte(reader);
System.err.printf("tag: %d\n", tag);

            ConstantPoolInfoTag cpiTag = ConstantPoolInfoTag.valueOf(tag);
            ConstantPoolInfo cc = cpiTag.getConstantPoolInfo();
            switch (cpiTag) {
            default: {
                cc.read(tag, reader);
                items.add(cc);
                break;
            }
            case ConstantLong:
            case ConstantDouble: {
                cc.read(tag, reader);
                items.add(cc);
                // longs take up two entries in the pool table
                // so do doubles
                count++;
                items.add(cc);
                break;
            }
            case ConstantUnknown:
                // fail safe ?
//System.err.printf("tag: %s\n", ConstantPoolInfoTag.valueOf(tag));
                count++;
                break;
            }

            count++;
        }

        for (ConstantPoolInfo cc : items) {
            cc.resolve(items);
        }
    }

    public void write(DataOutput writer) throws IOException {
        // i am assuming we have a valid constant pool list...
        // i dont do any error checking here except bare minimum!

        // write the number of constant pool entries
        Common.writeShort(writer, maxItems + 1);
        int count = 0;

        // goes from 1 -> constantpoolcount - 1
        while (count < maxItems) {
            ConstantPoolInfo item = items.get(count);

            switch (ConstantPoolInfoTag.valueOf(item.tag)) {
            default: {
                item.write(writer);

                break;
            }
            case ConstantLong:
            case ConstantDouble: {
                item.write(writer);

                // longs take up two entries in the pool table
                // so do doubles
                count++;
                break;
            }
            case ConstantUnknown:
                // fail safe ?
                // BADDDDDDDDDDDDDDDDDDDDD, prolly should check/fix this
                count++;
                break;
            }

            count++;
        }
    }

    public int getMaxItems() {
        return maxItems;
    }

    public ConstantPoolInfo getItem(int index) {
        if (items != null && index < maxItems) {
            return items.get(index);
        }
System.err.printf("index: %04x\n", index);
new Exception("*** DUMMY ***").printStackTrace(System.err);
        return null;
    }

    public int add(ConstantPoolInfo newItem) {
        items.add(newItem);
        maxItems++;
        return items.size() - 1;
    }
}
