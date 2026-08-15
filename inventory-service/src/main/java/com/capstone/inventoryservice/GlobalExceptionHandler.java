package com.capstone.inventoryservice;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {


    // =====================================================
    // INVENTORY NOT FOUND - 404
    // =====================================================

    @ExceptionHandler(InventoryNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleInventoryNotFound(
            InventoryNotFoundException ex) {

        Map<String, Object> response =
                new HashMap<>();

        response.put(
                "status",
                404
        );

        response.put(
                "error",
                "Inventory Not Found"
        );

        response.put(
                "message",
                ex.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }


    // =====================================================
    // INSUFFICIENT STOCK / INVALID QUANTITY - 400
    // =====================================================

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientStock(
            InsufficientStockException ex) {

        Map<String, Object> response =
                new HashMap<>();

        response.put(
                "status",
                400
        );

        response.put(
                "error",
                "Bad Request"
        );

        response.put(
                "message",
                ex.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }
}