package com.example.apidemo.model;

import java.io.Serializable;

/**
 * Order class to hold order information
 */
public class Order implements Serializable {
    private String orderId;
    private String prodName;
    private String prodSize;
    private String prodColor;
    private int prodPrice;

    // Default constructor with demo data
    public Order() {
        this.orderId = "260115143";
        this.prodName = "나이키알파플라이3";
        this.prodSize = "265";
        this.prodColor = "블랙";
        this.prodPrice = 349000;
    }

    // Constructor with all fields
    public Order(String orderId, String prodName, String prodSize, String prodColor, int prodPrice) {
        this.orderId = orderId;
        this.prodName = prodName;
        this.prodSize = prodSize;
        this.prodColor = prodColor;
        this.prodPrice = prodPrice;
    }

    // Getters
    public String getOrderId() {
        return orderId;
    }

    public String getProdName() {
        return prodName;
    }

    public String getProdSize() {
        return prodSize;
    }

    public String getProdColor() {
        return prodColor;
    }

    public int getProdPrice() {
        return prodPrice;
    }

    // Setters
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public void setProdName(String prodName) {
        this.prodName = prodName;
    }

    public void setProdSize(String prodSize) {
        this.prodSize = prodSize;
    }

    public void setProdColor(String prodColor) {
        this.prodColor = prodColor;
    }

    public void setProdPrice(int prodPrice) {
        this.prodPrice = prodPrice;
    }

    // Display formatted option: "265 / 블랙"
    public String getDisplayOption() {
        return prodSize + " / " + prodColor;
    }
}
