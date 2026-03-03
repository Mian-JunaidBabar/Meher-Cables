package com.example.labelmaker;

public class RateItem {
    private String productName;
    private String oldRate;
    private String newRate;

    public RateItem(String productName, String oldRate, String newRate) {
        this.productName = productName;
        this.oldRate = oldRate;
        this.newRate = newRate;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getOldRate() {
        return oldRate;
    }

    public void setOldRate(String oldRate) {
        this.oldRate = oldRate;
    }

    public String getNewRate() {
        return newRate;
    }

    public void setNewRate(String newRate) {
        this.newRate = newRate;
    }
}
