
package vavi.util.deobfuscation;

import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;


public class ChangeName extends JDialog {

    public ChangeName() {
        this.label1 = new JLabel();
        this.saveButton = new JButton();
        this.nameBox = new JTextField();
        this.cancelButton = new JButton();
        this.setLayout(null);
        // 
        // label1
        // 
//        this.label1.AutoSize = true;
        this.label1.setLocation(11, 22);
        this.label1.setName("label1");
        this.label1.setSize(new Dimension(101, 13));
//        this.label1.setTabIndex = 0;
        this.label1.setText("Component Name : ");
        // 
        // SaveButton
        // 
//        this.SaveButton.DialogResult = System.Windows.Forms.DialogResult.OK;
        this.saveButton.setLocation(368, 45);
        this.saveButton.setName("SaveButton");
        this.saveButton.setSize(new Dimension(83, 24));
//        this.SaveButton.TabIndex = 2;
        this.saveButton.setText("&Save");
//        this.SaveButton.UseVisualStyleBackColor = true;
        // 
        // NameBox
        // 
        this.nameBox.setLocation(119, 19);
        this.nameBox.setName("NameBox");
        this.nameBox.setSize(new Dimension(334, 20));
//        this.NameBox.TabIndex = 3;
        // 
        // CancelButton
        // 
//        this.CancelButton.DialogResult = System.Windows.Forms.DialogResult.Cancel;
        this.cancelButton.setLocation(270, 45);
        this.cancelButton.setName("CancelButton");
        this.cancelButton.setSize(new Dimension(83, 24));
//        this.CancelButton.TabIndex = 4;
        this.cancelButton.setText("&Cancel");
//        this.CancelButton.UseVisualStyleBackColor = true;
        // 
        // ChangeName
        // 
//        this.AcceptButton = this.SaveButton;
//        this.AutoScaleDimensions = new DimensionF(6F, 13F);
//        this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
        this.setSize(new Dimension(463, 78));
        add(this.cancelButton);
        add(this.nameBox);
        add(this.saveButton);
        add(this.label1);
//        this.FormBorderStyle = System.Windows.Forms.FormBorderStyle.FixedToolWindow;
        this.setName("ChangeName");
//        this.StartPosition = System.Windows.Forms.FormStartPosition.CenterScreen;
        this.setTitle("Change Name...");
        this.setLayout(null);
        this.pack();

    }

    private JLabel label1;
    private JButton saveButton;
    public JTextField nameBox;
    private JButton cancelButton;
}
