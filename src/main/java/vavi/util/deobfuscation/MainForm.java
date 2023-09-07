package vavi.util.deobfuscation;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import vavi.util.deobfuscation.common.ConstantPoolInfo;
import vavi.util.deobfuscation.common.RenameData;
import vavi.util.deobfuscation.common.RenameDatabase;
import vavi.util.deobfuscation.common.constantpoolinfo.ConstantClassInfo;
import vavi.util.deobfuscation.common.constantpoolinfo.ConstantFieldrefInfo;
import vavi.util.deobfuscation.common.constantpoolinfo.ConstantInterfaceMethodrefInfo;
import vavi.util.deobfuscation.common.constantpoolinfo.ConstantMethodrefInfo;
import vavi.util.deobfuscation.common.constantpoolinfo.ConstantPoolMethodInfo;
import vavi.util.deobfuscation.common.constantpoolinfo.ConstantPoolVariableInfo;
import vavi.util.deobfuscation.common.info.FieldInfo;
import vavi.util.deobfuscation.common.info.InterfaceInfo;
import vavi.util.deobfuscation.common.info.MethodInfo;
import vavi.util.deobfuscation.objects.ClassFile;
import vavi.util.deobfuscation.objects.DeObfuscator;


public class MainForm extends JFrame {

    DeObfuscator deObfuscator = null;

    List<File> files = null;

    RenameDatabase renameStore = null;

    private JLabel label1;

    private JFileChooser openFileDialog;

    private JTextField classFileTextField;

    private JButton fileBrowseButton;

    private JTree classTreeView;

    private JButton processButton;

//    private JToolTip toolTip;

    private JCheckBox renameClassCheckBox;

    private JCheckBox smartRenameMethods;

    private JProgressBar progressBar;

    private JCheckBox CleanupCheckBox;

    public MainForm() {
        this.classFileTextField = new JTextField(32);
        this.label1 = new JLabel();
        this.fileBrowseButton = new JButton();
        this.classTreeView = new JTree();
        this.openFileDialog = new JFileChooser();
        this.processButton = new JButton();
//        this.toolTip = new JToolTip(this.components);
        this.renameClassCheckBox = new JCheckBox();
        this.smartRenameMethods = new JCheckBox();
        this.progressBar = new JProgressBar();
        this.CleanupCheckBox = new JCheckBox();
        this.setLayout(new BorderLayout());
        //
        this.classFileTextField.setName("ClassFileTextBox");
        //
        this.label1.setName("label1");
        this.label1.setText("Add Class:");
        //
        this.fileBrowseButton.setName("ButtonFileBrowse");
        this.fileBrowseButton.setText("...");
        this.fileBrowseButton.addActionListener(this.button1_Click);
        //
        this.classTreeView.setName("TreeClassView");
        this.classTreeView.addMouseListener(this.treeClassViewNodeMouseClick);
        //
        // OpenFileDialog
        //
        this.openFileDialog.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.getName().matches(".+\\.class");
            }

            @Override
            public String getDescription() {
                return "Class Files";
            }
        });
        this.openFileDialog.setMultiSelectionEnabled(true);
        //
        this.processButton.setName("ProcessButton");
        this.processButton.setText("Deobfuscate");
        this.processButton.addActionListener(this.processButtonClick);
        //
//        this.toolTip.isBalloon(true);
        //
        this.renameClassCheckBox.setSelected(true);
        this.renameClassCheckBox.setName("RenameClassCheckBox");
        this.renameClassCheckBox.setText("Rename Classes");
        //
        this.smartRenameMethods.setSelected(true);
        this.smartRenameMethods.setEnabled(false);
        this.smartRenameMethods.setName("SmartRenameMethods");
        this.smartRenameMethods.setText("Smart Rename Methods");
        //
        this.progressBar.setName("Progress");
        this.progressBar.setVisible(false);
        this.progressBar.setPreferredSize(new Dimension(32, progressBar.getPreferredSize().height));
        //
        this.CleanupCheckBox.setSelected(true);
        this.CleanupCheckBox.setName("CleanupCheckBox");
        this.CleanupCheckBox.setText("Remove Old Files");
        //
        // MainForm
        //
        this.setPreferredSize(new Dimension(640, 480));
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(this.label1);
        panel.add(this.classFileTextField);
        panel.add(this.fileBrowseButton);
        add(panel, BorderLayout.NORTH);
        this.add(new JScrollPane(this.classTreeView), BorderLayout.CENTER);
        panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(this.CleanupCheckBox);
        panel.add(this.smartRenameMethods);
        panel.add(this.renameClassCheckBox);
        panel.add(this.progressBar);
        panel.add(this.processButton);
        add(panel, BorderLayout.SOUTH);
        this.setName("MainForm");
        this.setTitle("Java DeObfuscator v1.6b");
        this.addWindowListener(this.form1Load);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.pack();
        this.setVisible(true);
    }

    public static void main(String[] args) {
        MainForm main = new MainForm();
        main.openFileDialog.setCurrentDirectory(new File(args[0]).getParentFile());
    }

    private ActionListener button1_Click = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (openFileDialog.showOpenDialog(null) == JOptionPane.OK_OPTION) {
                if (files == null) {
                    files = new ArrayList<>();
                }
                files.addAll(Arrays.asList(openFileDialog.getSelectedFiles()));

                updateTree();

                classTreeView.expandRow(0);
            }
        }
    };

    private void updateTree() {
        ((DefaultTreeModel) classTreeView.getModel()).setRoot(null);

        deObfuscator = new DeObfuscator(files);

        for (File file : files) {
            ClassFile classFile = new ClassFile(file);

            if (!classFile.open()) {
                ((DefaultTreeModel) classTreeView.getModel())
                        .setRoot(new DefaultMutableTreeNode("Invalid class file: " + file));
                continue;
            }

            if (classFile != null) {
                DefaultMutableTreeNode bigRoot;

                // check if the user wants to rename the class file
                String originalClassName = classFile.getThisClassName() + " : " + classFile.getSuperClassName();
                String class_name = renameStore.getNewClassName(originalClassName);

                if (class_name == null) {
                    class_name = originalClassName;
                    bigRoot = new DefaultMutableTreeNode(class_name);
                    ((DefaultTreeModel) classTreeView.getModel()).setRoot(bigRoot);
                } else {
                    bigRoot = new DefaultMutableTreeNode(class_name);
                    ((DefaultTreeModel) classTreeView.getModel()).setRoot(bigRoot);
//                    bigroot.setBackGround(Color.blue);
                }

                bigRoot.setUserObject(originalClassName);

                DefaultMutableTreeNode root = new DefaultMutableTreeNode("Constants");
                bigRoot.add(root);
                DefaultMutableTreeNode methodsroot = new DefaultMutableTreeNode("Methods/Interfaces/Fields");
                root.add(methodsroot);
                DefaultMutableTreeNode methods = new DefaultMutableTreeNode("Methods");
                methodsroot.add(methods);
                DefaultMutableTreeNode interfaces = new DefaultMutableTreeNode("Interfaces");
                methodsroot.add(interfaces);
                DefaultMutableTreeNode fields = new DefaultMutableTreeNode("Fields");
                methodsroot.add(fields);
                DefaultMutableTreeNode variables = new DefaultMutableTreeNode("Values");
                root.add(variables);
                DefaultMutableTreeNode classes = new DefaultMutableTreeNode("Classes");
                root.add(classes);

                for (int i = 0; i < classFile.getConstantPool().getMaxItems(); i++) {
                    ConstantPoolInfo cc = classFile.getConstantPool().getItem(i);

                    if (cc instanceof ConstantPoolMethodInfo) {
                        if (cc instanceof ConstantMethodrefInfo) {
                            DefaultMutableTreeNode temp = new DefaultMutableTreeNode("\"" +
                                                                                     ((ConstantMethodrefInfo) cc).nameAndType.name +
                                                                                     "\"");
                            methods.add(temp);
                            temp.add(new DefaultMutableTreeNode("Descriptor = " +
                                                                ((ConstantMethodrefInfo) cc).nameAndType.descriptor));
                            temp.add(new DefaultMutableTreeNode("Parent = " + ((ConstantMethodrefInfo) cc).parentClass.name));

//                            if (DeObfuscator.DoRename(((ConstantMethodrefInfo) cc).NameAndType.Name))
//                                temp.setBackGround(Color.red);

                            continue;
                        }

                        if (cc instanceof ConstantInterfaceMethodrefInfo) {
                            DefaultMutableTreeNode temp = new DefaultMutableTreeNode("\"" +
                                                                                     ((ConstantInterfaceMethodrefInfo) cc).nameAndType.name +
                                                                                     "\"");
                            interfaces.add(temp);
                            temp.add(new DefaultMutableTreeNode("Descriptor = " +
                                                                ((ConstantInterfaceMethodrefInfo) cc).nameAndType.descriptor));
                            temp.add(new DefaultMutableTreeNode("Parent = " +
                                                                ((ConstantInterfaceMethodrefInfo) cc).parentClass.name));

//                            if (DeObfuscator.DoRename(((ConstantInterfaceMethodrefInfo) cc).NameAndType.Name))
//                                temp.setBackGround(Color.red);

                            continue;
                        }

                        if (cc instanceof ConstantFieldrefInfo) {
                            DefaultMutableTreeNode temp = new DefaultMutableTreeNode("\"" +
                                                                                     ((ConstantFieldrefInfo) cc).nameAndType.name +
                                                                                     "\"");
                            fields.add(temp);
                            temp.add(new DefaultMutableTreeNode("Descriptor = " +
                                                                ((ConstantFieldrefInfo) cc).nameAndType.descriptor));
                            if (((ConstantFieldrefInfo) cc).parentClass != null)
                                temp.add(new DefaultMutableTreeNode("Parent = " +
                                                                    ((ConstantFieldrefInfo) cc).parentClass.name));

//                            if (DeObfuscator.DoRename(((ConstantFieldrefInfo) cc).NameAndType.Name))
//                                temp.setBackGround(Color.red);

                            continue;
                        }
                    } else if (cc instanceof ConstantPoolVariableInfo) {
                        DefaultMutableTreeNode temp = new DefaultMutableTreeNode("\"" + ((ConstantPoolVariableInfo) cc).value +
                                                                                 "\"");
                        variables.add(temp);
                        temp.add(new DefaultMutableTreeNode("References = " + cc.references));
                    } else if (cc instanceof ConstantClassInfo) {
                        DefaultMutableTreeNode temp = new DefaultMutableTreeNode("\"" + ((ConstantClassInfo) cc).name + "\"");
                        classes.add(temp);
                        temp.add(new DefaultMutableTreeNode("References = " + cc.references));
                    }
                }

                root = new DefaultMutableTreeNode("Interfaces");
                bigRoot.add(root);
                for (InterfaceInfo ii : classFile.getInterfaces().getItems()) {
                    root.add(new DefaultMutableTreeNode(ii.getInterface().name));
                }

                root = new DefaultMutableTreeNode("Fields");
                bigRoot.add(root);
                for (FieldInfo fi : classFile.getFields().getItems()) {
                    RenameData rd = renameStore.getNewFieldInfo(originalClassName, fi.getDescriptor(), fi.getName().value);
                    if (rd != null) {
                        DefaultMutableTreeNode temp = new DefaultMutableTreeNode(rd.getFieldName());
                        root.add(temp);
                        temp.add(new DefaultMutableTreeNode(rd.getFieldType()));
//                        temp.setBackGround(Color.blue);
                    } else {
                        DefaultMutableTreeNode temp = new DefaultMutableTreeNode(fi.getName().value);
                        root.add(temp);
                        temp.add(new DefaultMutableTreeNode(fi.getDescriptor()));
                        temp.setUserObject(fi.getName().value);

//                        if (DeObfuscator.DoRename(fi.getName().Value))
//                            temp.setBackGround(Color.red);
                    }
                }

                root = new DefaultMutableTreeNode("Methods");
                bigRoot.add(root);
                for (MethodInfo mi : classFile.getMethods().getItems()) {
                    RenameData rd = renameStore.getNewMethodInfo(originalClassName, mi.getDescriptor(), mi.getName().value);
                    if (rd != null) {
                        DefaultMutableTreeNode temp = new DefaultMutableTreeNode(rd.getFieldName());
                        root.add(temp);
                        temp.add(new DefaultMutableTreeNode(rd.getFieldType()));
//                        temp.setBackGround(Color.blue);
                    } else {
                        DefaultMutableTreeNode temp = new DefaultMutableTreeNode(mi.getName().value);
                        root.add(temp);
                        temp.add(new DefaultMutableTreeNode(mi.getDescriptor()));
                        temp.setUserObject(mi.getName().value);
//                        temp.addChild(String.Format("Offset = {0:X}", mi.Offset));

//                        if (DeObfuscator.DoRename(mi.getName().Value))
//                            temp.setBackGround(Color.red);
                    }
                }
            }
        }
    }

    private ActionListener processButtonClick = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (files == null)
                return;

            deObfuscator = new DeObfuscator(files);

            deObfuscator.setCleanup(CleanupCheckBox.isSelected());
            deObfuscator.setRenameClasses(renameClassCheckBox.isSelected());

            progressBar.setMaximum(files.size());
            progressBar.setVisible(true);

            deObfuscator.setProgressive(new Progressive.SwingProgressive(progressBar));

            // update the classfile with the new deobfuscated version
            List<File> newFileList = deObfuscator.deObfuscateAll(renameStore);
            if (newFileList != null) {
                JOptionPane.showConfirmDialog(null, "DeObfuscated everything ok!", "DeObfuscator", JOptionPane.DEFAULT_OPTION);
                files = newFileList;
            } else
                JOptionPane.showConfirmDialog(null, "Error!!!", "DeObfuscator", JOptionPane.DEFAULT_OPTION);

            progressBar.setVisible(false);
            renameStore = new RenameDatabase();
            updateTree();
        }
    };

    private MouseListener treeClassViewNodeMouseClick = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            // detect right click on a valid member to popup a 'change name' box.
            if (e.getButton() == MouseEvent.BUTTON2 && ((TreeNode) e.getSource()).getParent() != null &&
                ((TreeNode) e.getSource()).getParent().getParent() != null) {
//                ChangeName FChangeName = new ChangeName();
//                FChangeName.NameBox.setText((MutableTreeNode) e.getSource()).toString());
                // get the full path of the node we clicked on, so we have all the
                // information
                // relating to it
                // get parentmost node
                TreeNode pn = ((TreeNode) e.getSource());
                while (pn.getParent() != null) {
                    pn = pn.getParent();
                }

                // get trailing node
                DefaultMutableTreeNode tn = ((DefaultMutableTreeNode) e.getSource());
                while (tn.getChildCount() > 0) {
                    tn = (DefaultMutableTreeNode) tn.getChildAt(0);
                }

                String class_name = pn.toString(); // classname

                Object[] sl = tn.getUserObjectPath();
                String type = (String) sl[1];
                String old_name = tn.getParent().toString();

                if (class_name == null || type == null || old_name == null) {
                    return;
                }

                // check which subsection we are in, so we can add it to the right
                // list
                Object result = JOptionPane.showInputDialog(null,
                                                            "Change Name...",
                                                            "ChangeName",
                        JOptionPane.INFORMATION_MESSAGE,
                                                            null,
                                                            null,
                                                            e.getSource().toString());
                if ((type.equals("Methods") || type.equals("Fields")) && // section
                    (result != null)) {
                    String old_descriptor = (String) sl[3];

                    if (old_descriptor == null)
                        return;

                    if (type.equals("Methods")) {
                        renameStore.addRenameMethod(class_name, old_descriptor, old_name, old_descriptor, (String) result);
                    } else if (type.equals("Fields")) {
                        renameStore.addRenameField(class_name, old_descriptor, old_name, old_descriptor, (String) result);
                    }

                    // update the tree without reloading it
                    ((DefaultMutableTreeNode) tn.getParent()).setUserObject(result);
//                    tn.getParent().ToolTipText = "was '" + tn.getParent().Tag.ToString() + "'";
//                    tn.getParent().setBackGround(Color.blue);
                }
            } else if (e.getButton() == MouseEvent.BUTTON2 && ((TreeNode) e.getSource()).getParent() == null) {
                ChangeNameDialog changeName = new ChangeNameDialog();
                String[] s = e.getSource().toString().split(":");

                String old_name = s[0].trim();
                String old_descriptor = s[1].trim();

                if (s.length == 0)
                    return;

                changeName.nameBox.setText(old_name);

                // change the class name, since it's a root node
                if (JOptionPane.showInputDialog(null, "Change Name...", "ChangeName", JOptionPane.WARNING_MESSAGE) != null) {
                    String new_name_and_type = changeName.nameBox.getText() + " : " + old_descriptor;
                    renameStore.addRenameClass(e.getSource().toString(), new_name_and_type);

//                    ((TreeNode) e.getSource()).setBackGround(Color.blue);
                    ((DefaultMutableTreeNode) e.getSource()).setUserObject(new_name_and_type);
//                    ((TreeNode) e.getSource()).ToolTipText = "was '" + e.Node.Tag.ToString() + "'";
                }
            }
        }
    };

    private WindowListener form1Load = new WindowAdapter() {
        @Override
        public void windowOpened(WindowEvent e) {
            renameStore = new RenameDatabase();
        }
    };
}
