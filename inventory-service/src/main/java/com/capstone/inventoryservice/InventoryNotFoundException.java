package com.capstone.inventoryservice;

public class InventoryNotFoundException
        extends RuntimeException {

    public InventoryNotFoundException(
            String message) {

        super(message);
    }
}