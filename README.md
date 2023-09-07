[![Release](https://jitpack.io/v/umjammer/vavi-apps-jdo.svg)](https://jitpack.io/#umjammer/vavi-apps-jdo)
[![Java CI](https://github.com/umjammer/vavi-apps-jdo/actions/workflows/maven.yml/badge.svg)](https://github.com/umjammer/vavi-apps-jdo/actions/workflows/maven.yml)
[![CodeQL](https://github.com/umjammer/vavi-apps-jdo/actions/workflows/codeql.yml/badge.svg)](https://github.com/umjammer/vavi-apps-jdo/actions/workflows/codeql.yml)
![Java](https://img.shields.io/badge/Java-17-b07219)

# Java DeObfuscator (aka JDO)

When reversing Java, you will invariably run up against a fairly common form of data obscuring techniques: obfuscated code.

See, it goes like this:

Java programs come in a 'class' format, which can be decompiled by decompilers like Jad and DJ Java Decompiler, which in the majority of cases produce extremely good replications of the original source code. Due to this, reversers (and well.. almost anyone really) could decompile Java program and poke through the entire source tree, often able to make changes and recompile the code for their own research.

In came data obfuscation to the rescue! Not only does it make it more difficult to Reverse Engineer and decompile java applications, it also shrinks the size of the class.

Data Obfuscation attempts to obscure and obfuscate the variable names, class names, method names, strings and sometimes even the actual byte code in an attempt to thwart decompilers. And some of them even work.

Consider the following code, taken from a freely available (obfuscated) java library and decompiled with Jad:

```java
    private static int _mthchar(int i1) {
        int j1 = 0;
        int k1 = (1 << i1) - 1;
        if(i1 > _fldchar) {
            i1 -= _fldchar;
            j1 = _fldnew[bL++] << i1 & k1;
            _fldchar = 32;
        }
        _fldchar -= i1;
        j1 |= _fldnew[bL] >> _fldchar & (1 << i1) - 1;
        j1 &= k1;
        return j1;
    }
```
  
Due to Jad's solid engine, it is able to create correct code by prepending '_fld' to indicate the variable is a field and '_mth' to indicate its a method. A lesser decompiler may have got confused and produced garbled results or crashed, at the very least confusing the reader with interesting snippets of code like:

```java
    private static int char(int i1) {
        int j1 = 0;
        int k1 = (1 << i1) - 1;
        if(i1 > char) {
            i1 -= char;
            j1 = new[bL++] << i1 & k1;
            char = 32;
        }
        char -= i1;
        j1 |= new[bL] >> char & (1 << i1) - 1;
        j1 &= k1;
        return j1;
    }
```

As you can see this is not exactly easy to reverse! One gets stuck constantly backtracking, resolving variables, methods and classes to their proper variables instead of getting to the heart of the problem. Jad does well to make it a step easier with its extra labels but its still quite tiresome to reverse. Obfuscation is a fairly common method to "cheaply" thwart the impatient and slap-dash reverse engineers, but with patience anything can be unravelled. Fortunately JDO makes it that little bit easier.

For example, this is the exact same method after running the class files through JDO: 

```java
    private static int sub_2374(int i) {
        int j = 0;
        int k = (1 << i) - 1;
        if(i > var_2354) {
            i -= var_2354;
            j = var_1dd4[var_234c++] << i & k;
            var_2354 = 32;
        }
        var_2354 -= i;
        j |= var_1dd4[var_234c] >> var_2354 & (1 << i) - 1;
        j &= k;
        return j;
    }
```

As you can see the code structure does not change, but suddenly it seems that little bit easier to understand.

Currently JDO does the following:
- renames obfuscated methods, variables, constants and class names to be unique and more indicative of their type
- propogates changes throughout the entire source tree (beta)
- has an easy to use GUI
- allow you to specify the name for a field, method and class (new feature!)

Currently JDO does not do the following (but it might one day)
- modify method bytecode in any way

## Instructions

The program is easy to use. If you have a single .class file, u can simply open it and select [Deobfuscate] the file will be automatically deobfuscated and overwritten.

However if you have references to other obfuscated files in the same project, they will not be properly deobfuscated (nor changes from one file propogated). So if you have a few files in your project (or a .jar file) simply:

1. Unzip the .jar to a temp directory (if your files didn't come in a .jar, skip this step)
2. Run JDO
3. Click the '...' button and Add all the class files to the project (including any in sub directories) 
4. Repeat Step 3 until all class files from the project are listed in the display
5. Click [Deobfuscate] it should give you a successful message and all of the files will be automatically deobfuscated and overwritten

Note: To specify the name that a method, field or class gets renamed to, simply right click the node in the tree, specify the new name and hit 'Save'. It will turn Blue in the display, this is to indicate that the field will be forced renamed on next deobfuscation. Fields or Methods displayed in Red indicate they will be auto-renamed on the next deobfuscation.

Thats it!

## Comments and Suggestions

Feel free to email any comments or suggestions or bugs or reversing info to me by reversing this:
moc.tesffoelif (ta) odj

