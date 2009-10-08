
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
import javax.swing.ProgressMonitor;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;


public class MainForm extends JFrame {

    TDeObfuscator deObfuscator = null;
    ArrayList<File> files = null;
    RenameDatabase renameStore = null;
    private JLabel label1;
    private JFileChooser openFileDialog;
    private JTextField ClassFileTextBox;
    private JButton ButtonFileBrowse;
    private JTree TreeClassView;
    private JButton ProcessButton;
//    private JToolTip ToolTip;
    private JCheckBox RenameClassCheckBox;
    private JCheckBox SmartRenameMethods;
    private JProgressBar Progress;
    private JCheckBox CleanupCheckBox;

    public MainForm() {
        this.ClassFileTextBox = new JTextField(32);
        this.label1 = new JLabel();
        this.ButtonFileBrowse = new JButton();
        this.TreeClassView = new JTree();
        this.openFileDialog = new JFileChooser();
        this.ProcessButton = new JButton();
//        this.ToolTip = new JToolTip(this.components);
        this.RenameClassCheckBox = new JCheckBox();
        this.SmartRenameMethods = new JCheckBox();
        this.Progress = new JProgressBar();
        this.CleanupCheckBox = new JCheckBox();
        this.setLayout(new BorderLayout());
        // 
        // ClassFileTextBox
        // 
        this.ClassFileTextBox.setName("ClassFileTextBox");
        // 
        // label1
        // 
        this.label1.setName("label1");
        this.label1.setText("Add Class:");
        // 
        // ButtonFileBrowse
        // 
        this.ButtonFileBrowse.setName("ButtonFileBrowse");
        this.ButtonFileBrowse.setText("...");
        this.ButtonFileBrowse.addActionListener(this.button1_Click);
        // 
        // TreeClassView
        // 
        this.TreeClassView.setName("TreeClassView");
        this.TreeClassView.addMouseListener(this.TreeClassView_NodeMouseClick);
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
        // ProcessButton
        // 
        this.ProcessButton.setName("ProcessButton");
        this.ProcessButton.setText("Deobfuscate");
        this.ProcessButton.addActionListener(this.ProcessButton_Click);
        // 
        // ToolTip
        // 
//        this.ToolTip.IsBalloon(true);
        // 
        // RenameClassCheckBox
        // 
        this.RenameClassCheckBox.setSelected(true);
        this.RenameClassCheckBox.setName("RenameClassCheckBox");
        this.RenameClassCheckBox.setText("Rename Classes");
        // 
        // SmartRenameMethods
        // 
        this.SmartRenameMethods.setSelected(true);
        this.SmartRenameMethods.setEnabled(false);
        this.SmartRenameMethods.setName("SmartRenameMethods");
        this.SmartRenameMethods.setText("Smart Rename Methods");
        // 
        // Progress
        // 
        this.Progress.setName("Progress");
        this.Progress.setVisible(false);
        this.Progress.setPreferredSize(new Dimension(32, Progress.getPreferredSize().height));
        // 
        // CleanupCheckBox
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
        panel.add(this.ClassFileTextBox);
        panel.add(this.ButtonFileBrowse);
        add(panel, BorderLayout.NORTH);
        this.add(new JScrollPane(this.TreeClassView), BorderLayout.CENTER);
        panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(this.CleanupCheckBox);
        panel.add(this.SmartRenameMethods);
        panel.add(this.RenameClassCheckBox);
        panel.add(this.Progress);
        panel.add(this.ProcessButton);
        add(panel, BorderLayout.SOUTH);
        this.setName("MainForm");
        this.setTitle("Java DeObfuscator v1.6b");
        this.addWindowListener(this.Form1_Load);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.pack();
        this.setVisible(true);
    }

    public static void main(String[] args) {
        MainForm main = new MainForm();
        main.openFileDialog.setCurrentDirectory(new File(args[0]).getParentFile());
    }

    private ActionListener button1_Click = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            if (openFileDialog.showOpenDialog(null) == JOptionPane.OK_OPTION) {
                if (files == null) {
                    files = new ArrayList<File>();
                }
                for (File fn : openFileDialog.getSelectedFiles()) {
                    files.add(fn);
                }
    
                UpdateTree();

                TreeClassView.expandRow(0);
            }
        }
    };

    private void UpdateTree() {
        ((DefaultTreeModel) TreeClassView.getModel()).setRoot(null);

        deObfuscator = new TDeObfuscator(files);

        for (File file : files) {
            TClassFile classFile = new TClassFile(file);

            if (!classFile.open()) {
                ((DefaultTreeModel) TreeClassView.getModel()).setRoot(new DefaultMutableTreeNode("Invalid class file: " + file));
                continue;
            }

            if (classFile != null) {
                DefaultMutableTreeNode bigroot;

                // check if the user wants to rename the class file
                String original_class_name = classFile.getThisClassName() + " : " + classFile.getSuperClassName();
                String class_name = renameStore.GetNewClassName(original_class_name);

                if (class_name == null) {
                    class_name = original_class_name;
                    bigroot = new DefaultMutableTreeNode(class_name);((DefaultTreeModel) TreeClassView.getModel()).setRoot(bigroot);
                } else {
                    bigroot = new DefaultMutableTreeNode(class_name);((DefaultTreeModel) TreeClassView.getModel()).setRoot(bigroot);
//                    bigroot.setBackGround(Color.blue);
                }

                bigroot.setUserObject(original_class_name);

                DefaultMutableTreeNode root = new DefaultMutableTreeNode("Constants");bigroot.add(root);
                DefaultMutableTreeNode methodsroot = new DefaultMutableTreeNode("Methods/Interfaces/Fields");root.add(methodsroot);
                DefaultMutableTreeNode methods = new DefaultMutableTreeNode("Methods");methodsroot.add(methods);
                DefaultMutableTreeNode interfaces = new DefaultMutableTreeNode("Interfaces");methodsroot.add(interfaces);
                DefaultMutableTreeNode fields = new DefaultMutableTreeNode("Fields");methodsroot.add(fields);
                DefaultMutableTreeNode variables = new DefaultMutableTreeNode("Values");root.add(variables);
                DefaultMutableTreeNode classes = new DefaultMutableTreeNode("Classes");root.add(classes);

                for (int i = 0; i < classFile.getConstantPool().getMaxItems(); i++) {
                    ConstantPoolInfo cc = classFile.getConstantPool().getItem(i);

                    if (cc instanceof ConstantPoolMethodInfo) {
                        if (cc instanceof ConstantMethodrefInfo) {
                            DefaultMutableTreeNode temp = new DefaultMutableTreeNode("\"" + ((ConstantMethodrefInfo) cc).NameAndType.Name + "\"");methods.add(temp);
                            temp.add(new DefaultMutableTreeNode("Descriptor = " + ((ConstantMethodrefInfo) cc).NameAndType.Descriptor));
                            temp.add(new DefaultMutableTreeNode("Parent = " + ((ConstantMethodrefInfo) cc).ParentClass.Name));

//                            if (DeObfuscator.DoRename(((ConstantMethodrefInfo) cc).NameAndType.Name))
//                                temp.setBackGround(Color.red);

                            continue;
                        }

                        if (cc instanceof ConstantInterfaceMethodrefInfo) {
                            DefaultMutableTreeNode temp = new DefaultMutableTreeNode("\"" + ((ConstantInterfaceMethodrefInfo) cc).NameAndType.Name + "\"");interfaces.add(temp);
                            temp.add(new DefaultMutableTreeNode("Descriptor = " + ((ConstantInterfaceMethodrefInfo) cc).NameAndType.Descriptor));
                            temp.add(new DefaultMutableTreeNode("Parent = " + ((ConstantInterfaceMethodrefInfo) cc).ParentClass.Name));

//                            if (DeObfuscator.DoRename(((ConstantInterfaceMethodrefInfo) cc).NameAndType.Name))
//                                temp.setBackGround(Color.red);

                            continue;
                        }

                        if (cc instanceof ConstantFieldrefInfo) {
                            DefaultMutableTreeNode temp = new DefaultMutableTreeNode("\"" + ((ConstantFieldrefInfo) cc).NameAndType.Name + "\"");fields.add(temp);
                            temp.add(new DefaultMutableTreeNode("Descriptor = " + ((ConstantFieldrefInfo) cc).NameAndType.Descriptor));
                            if (((ConstantFieldrefInfo) cc).ParentClass != null)
                                temp.add(new DefaultMutableTreeNode("Parent = " + ((ConstantFieldrefInfo) cc).ParentClass.Name));

//                            if (DeObfuscator.DoRename(((ConstantFieldrefInfo) cc).NameAndType.Name))
//                                temp.setBackGround(Color.red);

                            continue;
                        }
                    } else if (cc instanceof ConstantPoolVariableInfo) {
                        DefaultMutableTreeNode temp = new DefaultMutableTreeNode("\"" + ((ConstantPoolVariableInfo) cc).Value + "\"");variables.add(temp);
                        temp.add(new DefaultMutableTreeNode("References = " + cc.references));
                    } else if (cc instanceof ConstantClassInfo) {
                        DefaultMutableTreeNode temp = new DefaultMutableTreeNode("\"" + ((ConstantClassInfo) cc).Name + "\"");classes.add(temp);
                        temp.add(new DefaultMutableTreeNode("References = " + cc.references));
                    }
                }

                root = new DefaultMutableTreeNode("Interfaces");bigroot.add(root);
                for (InterfaceInfo ii : classFile.getInterfaces().getItems()) {
                    root.add(new DefaultMutableTreeNode(ii.getInterface().Name));
                }

                root = new DefaultMutableTreeNode("Fields");bigroot.add(root);
                for (FieldInfo fi : classFile.getFields().getItems()) {
                    RenameData rd = renameStore.GetNewFieldInfo(original_class_name, fi.getDescriptor(), fi.getName().Value);
                    if (rd != null) {
                        DefaultMutableTreeNode temp = new DefaultMutableTreeNode(rd.getFieldName());root.add(temp);
                        temp.add(new DefaultMutableTreeNode(rd.getFieldType()));
//                        temp.setBackGround(Color.blue);
                    } else {
                        DefaultMutableTreeNode temp = new DefaultMutableTreeNode(fi.getName().Value);root.add(temp);
                        temp.add(new DefaultMutableTreeNode(fi.getDescriptor()));
                        temp.setUserObject(fi.getName().Value);

//                        if (DeObfuscator.DoRename(fi.getName().Value))
//                            temp.setBackGround(Color.red);
                    }
                }

                root = new DefaultMutableTreeNode("Methods");bigroot.add(root);
                for (MethodInfo mi : classFile.getMethods().getItems()) {
                    RenameData rd = renameStore.GetNewMethodInfo(original_class_name, mi.getDescriptor(), mi.getName().Value);
                    if (rd != null) {
                        DefaultMutableTreeNode temp = new DefaultMutableTreeNode(rd.getFieldName());root.add(temp);
                        temp.add(new DefaultMutableTreeNode(rd.getFieldType()));
//                        temp.setBackGround(Color.blue);
                    } else {
                        DefaultMutableTreeNode temp = new DefaultMutableTreeNode(mi.getName().Value);root.add(temp);
                        temp.add(new DefaultMutableTreeNode(mi.getDescriptor()));
                        temp.setUserObject(mi.getName().Value);
//                        temp.addChild(String.Format("Offset = {0:X}", mi.Offset));

//                        if (DeObfuscator.DoRename(mi.getName().Value))
//                            temp.setBackGround(Color.red);
                    }
                }
            }
        }
    }

    private ActionListener ProcessButton_Click = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
        if (files == null)
            return;

        deObfuscator = new TDeObfuscator(files);

        deObfuscator.setCleanup(CleanupCheckBox.isSelected());
        deObfuscator.setRenameClasses(RenameClassCheckBox.isSelected());

        Progress.setMaximum(files.size());
        Progress.setVisible(true);

        TDeObfuscator.Progress = new ProgressMonitor(Progress, null, null, 0, 100);

        // update the classfile with the new deobfuscated version
        ArrayList<File> NewFileList = deObfuscator.DeObfuscateAll(renameStore);
        if (NewFileList != null) {
            JOptionPane.showConfirmDialog(null, "DeObfuscated everything ok!", "DeObfuscator", JOptionPane.DEFAULT_OPTION);
            files = NewFileList;
        } else
            JOptionPane.showConfirmDialog(null, "Error!!!", "DeObfuscator", JOptionPane.DEFAULT_OPTION);

        Progress.setVisible(false);
        renameStore = new RenameDatabase();
        UpdateTree();
    }
    };

    private MouseListener TreeClassView_NodeMouseClick = new MouseAdapter() {
        public void mouseClicked(MouseEvent e) {
            // detect right click on a valid member to popup a 'change name' box.
            if (e.getButton() == MouseEvent.BUTTON2 && ((TreeNode) e.getSource()).getParent() != null && ((TreeNode) e.getSource()).getParent().getParent() != null) {
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
                Object result = JOptionPane.showInputDialog(null, "Change Name...", "ChangeName", JOptionPane.YES_NO_CANCEL_OPTION, null, null, ((TreeNode) e.getSource()).toString());
                if ((type == "Methods" || type == "Fields") && // section
                    (result != null)) {
                    String old_descriptor = (String) sl[3];
    
                    if (old_descriptor == null)
                        return;
    
                    if (type == "Methods") {
                        renameStore.AddRenameMethod(class_name, old_descriptor, old_name, old_descriptor, (String) result);
                    } else if (type == "Fields") {
                        renameStore.AddRenameField(class_name, old_descriptor, old_name, old_descriptor, (String) result);
                    }
    
                    // update the tree without reloading it
                    ((DefaultMutableTreeNode) tn.getParent()).setUserObject(result);
//                    tn.getParent().ToolTipText = "was '" + tn.getParent().Tag.ToString() + "'";
//                    tn.getParent().setBackGround(Color.blue);
                }
            } else if (e.getButton() == MouseEvent.BUTTON2 && ((TreeNode) e.getSource()).getParent() == null) {
                ChangeName FChangeName = new ChangeName();
                String[] s = ((TreeNode) e.getSource()).toString().split(":");
    
                String old_name = s[0].trim();
                String old_descriptor = s[1].trim();
    
                if (s.length == 0)
                    return;
    
                FChangeName.NameBox.setText(old_name);
    
                // change the class name, since its a root node
                if (JOptionPane.showInputDialog(null, "Change Name...", "ChangeName", JOptionPane.OK_CANCEL_OPTION) != null) {
                    String new_name_and_type = FChangeName.NameBox.getText() + " : " + old_descriptor;
                    renameStore.AddRenameClass(((MutableTreeNode) e.getSource()).toString(), new_name_and_type);
    
//                    ((TreeNode) e.getSource()).setBackGround(Color.blue);
                    ((DefaultMutableTreeNode) e.getSource()).setUserObject(new_name_and_type);
//                    ((TreeNode) e.getSource()).ToolTipText = "was '" + e.Node.Tag.ToString() + "'";
                }
            }
        }
    };

    private WindowListener Form1_Load = new WindowAdapter() {
        @Override
        public void windowOpened(WindowEvent e) {
            renameStore = new RenameDatabase();
        }
    };
}
