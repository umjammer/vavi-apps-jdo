/*
 * https://github.com/fileoffset/JDO
 */

package vavi.util.deobfuscation.common;

import java.io.DataInput;

import vavi.util.deobfuscation.common.attrubuteinfo.CodeAttributeInfo;
import vavi.util.deobfuscation.common.attrubuteinfo.ConstantValueAttributeInfo;
import vavi.util.deobfuscation.common.attrubuteinfo.DeprecatedAttributeInfo;
import vavi.util.deobfuscation.common.attrubuteinfo.ExceptionsAttributeInfo;
import vavi.util.deobfuscation.common.attrubuteinfo.InnerClassesAttributeInfo;
import vavi.util.deobfuscation.common.attrubuteinfo.LineNumberAttributeInfo;
import vavi.util.deobfuscation.common.attrubuteinfo.LocalVariablesAttributeInfo;
import vavi.util.deobfuscation.common.attrubuteinfo.SourceFileAttributeInfo;
import vavi.util.deobfuscation.common.attrubuteinfo.SyntheticAttributeInfo;
import vavi.util.deobfuscation.common.attrubuteinfo.UnknownAttributeInfo;
import vavi.util.deobfuscation.objects.ConstantPool;

public enum AttributeType {
    ConstantValue {
        @Override
        public AttributeInfo getAttributeInfo(int nameIndex, DataInput reader, ConstantPool constantPool) {
            return new ConstantValueAttributeInfo(nameIndex, reader, constantPool);
        }
    },
    Code {
        @Override
        public AttributeInfo getAttributeInfo(int nameIndex, DataInput reader, ConstantPool constantPool) {
            return new CodeAttributeInfo(nameIndex, reader, constantPool);
        }
    },
    Exceptions {
        @Override
        public AttributeInfo getAttributeInfo(int nameIndex, DataInput reader, ConstantPool constantPool) {
            return new ExceptionsAttributeInfo(nameIndex, reader, constantPool);
        }
    },
    InnerClasses {
        @Override
        public AttributeInfo getAttributeInfo(int nameIndex, DataInput reader, ConstantPool constantPool) {
            return new InnerClassesAttributeInfo(nameIndex, reader, constantPool);
        }
    },
    Synthetic {
        @Override
        public AttributeInfo getAttributeInfo(int nameIndex, DataInput reader, ConstantPool constantPool) {
            return new SyntheticAttributeInfo(nameIndex, reader, constantPool);
        }
    },
    SourceFile {
        @Override
        public AttributeInfo getAttributeInfo(int nameIndex, DataInput reader, ConstantPool constantPool) {
            return new SourceFileAttributeInfo(nameIndex, reader, constantPool);
        }
    },
    LineNumberTable {
        @Override
        public AttributeInfo getAttributeInfo(int nameIndex, DataInput reader, ConstantPool constantPool) {
            return new LineNumberAttributeInfo(nameIndex, reader, constantPool);
        }
    },
    LocalVariableTable {
        @Override
        public AttributeInfo getAttributeInfo(int nameIndex, DataInput reader, ConstantPool constantPool) {
            return new LocalVariablesAttributeInfo(nameIndex, reader, constantPool);
        }
    },
    Deprecated {
        @Override
        public AttributeInfo getAttributeInfo(int nameIndex, DataInput reader, ConstantPool constantPool) {
            return new DeprecatedAttributeInfo(nameIndex, reader, constantPool);
        }
    },
    StackMapTable {
        @Override
        public AttributeInfo getAttributeInfo(int nameIndex, DataInput reader, ConstantPool constantPool) {
            return new UnknownAttributeInfo(nameIndex, reader, constantPool);
        }
    };
    public abstract AttributeInfo getAttributeInfo(int nameIndex, DataInput reader, ConstantPool constantPool);
}
