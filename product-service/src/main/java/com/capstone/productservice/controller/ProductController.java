package com.capstone.productservice.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.capstone.productservice.dto.ProductCreateRequest;
import com.capstone.productservice.dto.ProductInventoryResponse;
import com.capstone.productservice.Product;
import com.capstone.productservice.service.ProductService;

import jakarta.validation.Valid;

@RestController
public class ProductController {

    private final ProductService productService;

    public ProductController(
            ProductService productService) {

        this.productService = productService;
    }


    // =====================================================
    // GET PRODUCTS
    // =====================================================

    @GetMapping("/products")
    public Page<Product> getProducts(

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            @RequestParam(defaultValue = "id")
            String sortBy,

            @RequestParam(defaultValue = "asc")
            String direction) {

        return productService.getProducts(
                page,
                size,
                sortBy,
                direction
        );
    }


    // =====================================================
    // SEARCH PRODUCTS
    // =====================================================

    @GetMapping("/products/search")
    public Page<Product> searchProducts(

            @RequestParam String field,

            @RequestParam String value,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            @RequestParam(defaultValue = "id")
            String sortBy,

            @RequestParam(defaultValue = "asc")
            String direction) {

        return productService.searchProducts(
                field,
                value,
                page,
                size,
                sortBy,
                direction
        );
    }


    // =====================================================
    // GET PRODUCT BY ID
    // =====================================================

    @GetMapping("/products/{id}")
    public Product getProductById(
            @PathVariable Long id) {

        return productService.getProductById(id);
    }


    // =====================================================
    // ADD PRODUCT + INITIAL STOCK
    // =====================================================

    @PostMapping("/products")
    public Product addProduct(
            @Valid
            @RequestBody ProductCreateRequest request) {

        return productService.addProduct(request);
    }


    // =====================================================
    // UPDATE PRODUCT DETAILS
    // =====================================================

    @PutMapping("/products/{id}")
    public Product updateProduct(

            @PathVariable Long id,

            @Valid
            @RequestBody Product product) {

        return productService.updateProduct(
                id,
                product
        );
    }


    // =====================================================
    // RECEIVE ADDITIONAL STOCK
    // =====================================================

    @PutMapping("/products/{id}/receive-stock")
    public String receiveStock(

            @PathVariable Long id,

            @RequestParam int quantity) {

        return productService.receiveStock(
                id,
                quantity
        );
    }


    // =====================================================
    // DELETE PRODUCT
    // =====================================================

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long id) {

        productService.deleteProduct(id);

        return ResponseEntity
                .noContent()
                .build();
    }


    // =====================================================
    // GET INVENTORY FOR PRODUCT
    // =====================================================

    @GetMapping("/products/{id}/inventory")
    public String getInventory(
            @PathVariable Long id) {

        return productService.getInventory(id);
    }


    // =====================================================
    // GET PRODUCT + INVENTORY DETAILS
    // =====================================================

    @GetMapping("/products/{id}/details")
    public ProductInventoryResponse getProductInventoryDetails(
            @PathVariable Long id) {

        return productService
                .getProductInventoryDetails(id);
    }
}