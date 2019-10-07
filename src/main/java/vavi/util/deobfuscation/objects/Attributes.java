/*
 * https://github.com/fileoffset/JDO
 */

package vavi.util.deobfuscation.objects;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import vavi.util.deobfuscation.common.AttributeInfo;
import vavi.util.deobfuscation.common.AttributeType;
import vavi.util.deobfuscation.common.Common;
import vavi.util.deobfuscation.common.constantpoolinfo.ConstantUtf8Info;


//
//INDIVIDUAL CLASSES
//
//These are all used by TClassFile to import each of its major sections
//

public class Attributes {
    DataInput reader;

    List<Object> items = null;

    int maxItems = 0;

    public Attributes(DataInput reader, ConstantPool constantPool) throws IOException {
        this.reader = reader;

        maxItems = Common.readShort(reader) - 1;
        items = new ArrayList<>();
        int count = 0;

        // goes from 1 -> attributescount - 1
        while (count <= maxItems) {
            int nameIndex = Common.readShort(reader);
            nameIndex--;
            ConstantUtf8Info Name = (ConstantUtf8Info) constantPool.getItem(nameIndex);

            AttributeInfo ai = AttributeType.valueOf(Name.value).getAttributeInfo(nameIndex, reader, constantPool);
            items.add(ai);

            count++;
        }
    }

    public void write(DataOutput writer) throws IOException {
        Common.writeShort(writer, maxItems + 1);

        int count = 0;

        // goes from 1 -> attributescount - 1
        while (count <= maxItems) {
            AttributeInfo item = (AttributeInfo) items.get(count);

            item.write(writer);

            count++;
        }
    }

    public List<Object> getItems() {
        return items;
    }
}
