package com.shopsphere.swing;

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public final class AdminView extends JFrame {
  final JTabbedPane tabs = new JTabbedPane();
  final JButton refresh = new JButton("Refresh"),
      add = new JButton("Add"),
      edit = new JButton("Edit selected"),
      action = new JButton("Update status / stock");
  final JLabel status = new JLabel("Ready");
  final Map<String, JTable> tables = new LinkedHashMap<>();
  final Map<String, List<Map<String, Object>>> data = new LinkedHashMap<>();

  public AdminView(String name) {
    super("ShopSphere · Desktop Administration");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setSize(1150, 720);
    setLocationRelativeTo(null);
    var top = new JPanel(new BorderLayout());
    top.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
    top.add(new JLabel("ShopSphere | Signed in as " + name), BorderLayout.WEST);
    var controls = new JPanel();
    for (var b : List.of(refresh, add, edit, action)) controls.add(b);
    top.add(controls, BorderLayout.EAST);
    add(top, BorderLayout.NORTH);
    for (String section : List.of("products", "categories", "orders", "users", "coupons")) {
      JTable table = new JTable();
      table.setAutoCreateRowSorter(true);
      table.setRowHeight(28);
      table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      tables.put(section, table);
      tabs.addTab(section, new JScrollPane(table));
    }
    add(tabs, BorderLayout.CENTER);
    status.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
    add(status, BorderLayout.SOUTH);
  }

  String section() {
    return tabs.getTitleAt(tabs.getSelectedIndex());
  }

  Map<String, Object> selected() {
    JTable table = tables.get(section());
    int row = table.getSelectedRow();
    if (row < 0) throw new IllegalArgumentException("Select a row first.");
    return data.get(section()).get(table.convertRowIndexToModel(row));
  }

  void render(String section, List<Map<String, Object>> rows) {
    data.put(section, rows);
    String[] columns =
        rows.isEmpty() ? new String[] {"No records"} : rows.get(0).keySet().toArray(String[]::new);
    var model =
        new DefaultTableModel(columns, 0) {
          public boolean isCellEditable(int r, int c) {
            return false;
          }
        };
    for (var row : rows) model.addRow(Arrays.stream(columns).map(row::get).toArray());
    tables.get(section).setModel(model);
    status.setText(
        rows.size() + " " + section + " · refreshed " + java.time.LocalTime.now().withNano(0));
  }
}
