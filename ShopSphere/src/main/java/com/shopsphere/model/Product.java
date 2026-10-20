package com.shopsphere.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;

public class Product implements Serializable {
  private static final long serialVersionUID = 1L;
  private final long id;
  private final String name;
  private final BigDecimal price;
  private final int stock;

  public Product(Map<String, Object> row) {
    id = ((Number) row.get("id")).longValue();
    name = (String) row.get("name");
    price = (BigDecimal) row.get("price");
    stock = ((Number) row.get("stock")).intValue();
  }

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public int getStock() {
    return stock;
  }

  public String toString() {
    return id + " | " + name + " | INR " + price + " | stock " + stock;
  }
}
