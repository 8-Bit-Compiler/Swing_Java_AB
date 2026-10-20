package com.shopsphere.swing;

import com.shopsphere.dao.Jdbc;
import com.shopsphere.service.ShopService;
import com.shopsphere.util.Database;
import java.awt.*;
import java.util.Arrays;
import javax.swing.*;

public final class AdminApp {
  public static void main(String[] args) throws Exception {
    Database.initialize();
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    SwingUtilities.invokeLater(AdminApp::login);
  }

  private static void login() {
    var frame = new JFrame("ShopSphere · Admin sign in");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    var panel = new JPanel(new GridLayout(0, 1, 10, 10));
    panel.setBorder(BorderFactory.createEmptyBorder(30, 35, 30, 35));
    var email = new JTextField("admin@shopsphere.local", 27);
    var password = new JPasswordField();
    var show = new JCheckBox("Show password");
    show.addActionListener(e -> password.setEchoChar(show.isSelected() ? (char) 0 : '•'));
    var mode = new JPanel();
    mode.add(new JRadioButton("Administrator", true));
    var submit = new JButton("Sign in");
    panel.add(new JLabel("ShopSphere · Desktop management"));
    panel.add(new JLabel("Email"));
    panel.add(email);
    panel.add(new JLabel("Password"));
    panel.add(password);
    panel.add(show);
    panel.add(mode);
    panel.add(submit);
    frame.add(panel);
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
    submit.addActionListener(
        e -> {
          submit.setEnabled(false);
          String mail = email.getText();
          char[] secret = password.getPassword();
          new SwingWorker<java.util.Map<String, Object>, Void>() {
            protected java.util.Map<String, Object> doInBackground() throws Exception {
              try {
                var service = new ShopService();
                var u = service.login(mail, new String(secret));
                service.requireAdmin(Jdbc.id(u));
                return u;
              } finally {
                Arrays.fill(secret, '\0');
              }
            }

            protected void done() {
              submit.setEnabled(true);
              password.setText("");
              try {
                var u = get();
                frame.dispose();
                new AdminController(Jdbc.id(u), (String) u.get("name"));
              } catch (Exception ex) {
                JOptionPane.showMessageDialog(
                    frame, "Sign in failed. Verify your administrator credentials.");
              }
            }
          }.execute();
        });
  }
}
