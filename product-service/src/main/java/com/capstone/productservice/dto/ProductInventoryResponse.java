package com.capstone.productservice.dto;

import com.capstone.productservice.Product;

public class ProductInventoryResponse {

    private Product product;

    private Object inventory;


    public ProductInventoryResponse() {
    }


    public ProductInventoryResponse(
            Product product,
            Object inventory) {

        this.product = product;
        this.inventory = inventory;
    }


    public Product getProduct() {
        return product;
    }


    public void setProduct(Product product) {
        this.product = product;
    }


    public Object getInventory() {
        return inventory;
    }


    public void setInventory(Object inventory) {
        this.inventory = inventory;
    }
}