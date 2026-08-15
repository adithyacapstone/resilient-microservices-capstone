package com.capstone.inventoryservice;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

@Entity
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Positive(message = "Product ID must be greater than 0")
    private Long productId;

    @Min(value = 0, message = "Available stock cannot be negative")
    private int availableStock;

    @Min(value = 0, message = "Reserved stock cannot be negative")
    private int reservedStock;

    @Min(value = 0, message = "Reorder level cannot be negative")
    private int reorderLevel;


    // Default constructor
    public Inventory() {
    }


    // Parameterized constructor
    public Inventory(
            Long productId,
            int availableStock,
            int reservedStock,
            int reorderLevel) {

        this.productId = productId;
        this.availableStock = availableStock;
        this.reservedStock = reservedStock;
        this.reorderLevel = reorderLevel;
    }


    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }


    public int getAvailableStock() {
        return availableStock;
    }

    public void setAvailableStock(int availableStock) {
        this.availableStock = availableStock;
    }


    public int getReservedStock() {
        return reservedStock;
    }

    public void setReservedStock(int reservedStock) {
        this.reservedStock = reservedStock;
    }


    public int getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(int reorderLevel) {
        this.reorderLevel = reorderLevel;
    }
}