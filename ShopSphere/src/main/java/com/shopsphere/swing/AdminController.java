package com.shopsphere.swing;

import com.shopsphere.dao.Jdbc;
import com.shopsphere.service.ShopService;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.Callable;
import javax.swing.*;

public final class AdminController {
  private final ShopService service = new ShopService();
  private final long actor;
  private final AdminView view;

  public AdminController(long actor, String name) {
    this.actor = actor;
    view = new AdminView(name);
    view.refresh.addActionListener(e -> refresh());
    view.tabs.addChangeListener(e -> refresh());
    view.add.addActionListener(e -> edit(false));
    view.edit.addActionListener(e -> edit(true));
    view.action.addActionListener(e -> action());
    view.setVisible(true);
    refresh();
  }

  private void async(Callable<?> task, Runnable done) {
    view.status.setText("Working…");
    new SwingWorker<Object, Void>() {
      protected Object doInBackground() throws Exception {
        return task.call();
      }

      protected void done() {
        try {
          get();
          done.run();
        } catch (Exception e) {
          Throwable cause = e.getCause() == null ? e : e.getCause();
          JOptionPane.showMessageDialog(
              view, cause.getMessage(), "ShopSphere", JOptionPane.ERROR_MESSAGE);
          view.status.setText("Request failed");
        }
      }
    }.execute();
  }

  private void refresh() {
    String section = view.section();
    async(
        () -> {
          service.requireAdmin(actor);
          var rows = service.dao.list(section);
          SwingUtilities.invokeLater(() -> view.render(section, rows));
          return null;
        },
        () -> {});
  }

  private Map<String, String> form(String title, Map<String, String> defaults) {
    var panel = new JPanel(new GridLayout(0, 2, 12, 10));
    var fields = new LinkedHashMap<String, JTextField>();
    defaults.forEach(
        (label, value) -> {
          panel.add(new JLabel(label));
          var field = new JTextField(value, 28);
          panel.add(field);
          fields.put(label, field);
        });
    if (JOptionPane.showConfirmDialog(
            view, panel, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)
        != JOptionPane.OK_OPTION) return null;
    var result = new LinkedHashMap<String, String>();
    fields.forEach((k, v) -> result.put(k, v.getText().trim()));
    return result;
  }

  private static String value(Map<String, Object> row, String key, String fallback) {
    return row == null ? fallback : String.valueOf(row.get(key));
  }

  private static LinkedHashMap<String, String> fields(String... pairs) {
    var m = new LinkedHashMap<String, String>();
    for (int i = 0; i < pairs.length; i += 2) m.put(pairs[i], pairs[i + 1]);
    return m;
  }

  private void edit(boolean existing) {
    try {
      String section = view.section();
      Map<String, Object> row = existing ? view.selected() : null;
      long id = row == null ? 0 : Jdbc.id(row);
      Map<String, String> f;
      switch (section) {
        case "products":
          f =
              form(
                  "Product · Active is true or false",
                  fields(
                      "Name",
                      value(row, "name", ""),
                      "Brand",
                      value(row, "brand", ""),
                      "Description",
                      value(row, "description", ""),
                      "Category ID",
                      value(row, "category_id", "1"),
                      "Price",
                      value(row, "price", "1000"),
                      "Discount %",
                      value(row, "discount", "0"),
                      "Stock",
                      value(row, "stock", "10"),
                      "Image",
                      value(row, "image_url", "/assets/headphones.svg"),
                      "Active",
                      value(row, "status", "true")));
          if (f != null)
            async(
                () -> {
                  service.saveProduct(
                      actor,
                      id,
                      Long.parseLong(f.get("Category ID")),
                      f.get("Name"),
                      f.get("Brand"),
                      f.get("Description"),
                      new BigDecimal(f.get("Price")),
                      new BigDecimal(f.get("Discount %")),
                      Integer.parseInt(f.get("Stock")),
                      f.get("Image"),
                      Boolean.parseBoolean(f.get("Active")));
                  return null;
                },
                this::refresh);
          break;
        case "categories":
          f =
              form(
                  "Category",
                  fields(
                      "Name",
                      value(row, "name", ""),
                      "Description",
                      value(row, "description", ""),
                      "Active",
                      value(row, "status", "true")));
          if (f != null)
            async(
                () -> {
                  service.category(
                      actor,
                      id,
                      f.get("Name"),
                      f.get("Description"),
                      Boolean.parseBoolean(f.get("Active")));
                  return null;
                },
                this::refresh);
          break;
        case "coupons":
          f =
              form(
                  "Coupon · type PERCENT or FIXED",
                  fields(
                      "Code",
                      value(row, "code", ""),
                      "Type",
                      value(row, "discount_type", "PERCENT"),
                      "Discount",
                      value(row, "discount_value", "10"),
                      "Minimum",
                      value(row, "minimum_order", "1000"),
                      "Maximum",
                      value(row, "maximum_discount", "1000"),
                      "Expiry YYYY-MM-DD",
                      value(row, "expiry_date", "2030-12-31"),
                      "Active",
                      value(row, "status", "true")));
          if (f != null)
            async(
                () -> {
                  service.coupon(
                      actor,
                      id,
                      f.get("Code"),
                      f.get("Type"),
                      new BigDecimal(f.get("Discount")),
                      new BigDecimal(f.get("Minimum")),
                      new BigDecimal(f.get("Maximum")),
                      LocalDate.parse(f.get("Expiry YYYY-MM-DD")),
                      Boolean.parseBoolean(f.get("Active")));
                  return null;
                },
                this::refresh);
          break;
        default:
          JOptionPane.showMessageDialog(
              view, "Use Update status / stock for orders and customers.");
      }
    } catch (Exception e) {
      JOptionPane.showMessageDialog(view, e.getMessage());
    }
  }

  private void action() {
    try {
      String section = view.section();
      var row = view.selected();
      long id = Jdbc.id(row);
      switch (section) {
        case "products":
          {
            String value =
                JOptionPane.showInputDialog(view, "New stock quantity", row.get("stock"));
            if (value != null)
              async(() -> service.stock(actor, id, Integer.parseInt(value)), this::refresh);
            break;
          }
        case "orders":
          {
            String old = (String) row.get("order_status");
            String[] options =
                switch (old) {
                  case "PLACED" -> new String[] {"PROCESSING", "CANCELLED"};
                  case "PROCESSING" -> new String[] {"SHIPPED", "CANCELLED"};
                  case "SHIPPED" -> new String[] {"DELIVERED"};
                  default -> new String[0];
                };
            if (options.length == 0) {
              JOptionPane.showMessageDialog(view, "This order is complete.");
              break;
            }
            String next =
                (String)
                    JOptionPane.showInputDialog(
                        view,
                        "Next order status",
                        "Order #" + id,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[0]);
            if (next != null)
              async(
                  () -> {
                    service.orderStatus(actor, id, next);
                    return null;
                  },
                  this::refresh);
            break;
          }
        case "users":
          async(
              () -> {
                service.userStatus(actor, id, !Boolean.TRUE.equals(row.get("status")));
                return null;
              },
              this::refresh);
          break;
        default:
          edit(true);
      }
    } catch (Exception e) {
      JOptionPane.showMessageDialog(view, e.getMessage());
    }
  }
}
