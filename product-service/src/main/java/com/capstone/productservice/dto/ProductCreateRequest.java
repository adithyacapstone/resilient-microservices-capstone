package com.capstone.productservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public class ProductCreateRequest {

    @NotBlank(message = "Product name cannot be empty")
    private String name;

    @NotBlank(message = "SKU cannot be empty")
    private String sku;

    @NotBlank(message = "Category cannot be empty")
    private String category;

    private String description;

    @Positive(message = "Price must be greater than 0")
    private double price;

    @Min(value = 0, message = "Initial quantity cannot be negative")
    private int initialQuantity;

    public ProductCreateRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getInitialQuantity() {
        return initialQuantity;
    }

    public void setInitialQuantity(int initialQuantity) {
        this.initialQuantity = initialQuantity;
    }
}