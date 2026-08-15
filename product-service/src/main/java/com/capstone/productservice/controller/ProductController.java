package com.capstone.productservice.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.capstone.productservice.dto.ProductInventoryResponse;
import com.capstone.productservice.Product;
import com.capstone.productservice.service.ProductService;

import jakarta.validation.Valid;

@RestController
public class ProductController {

    private final ProductService productService;

    public ProductController(
            ProductService productService) {

        this.productService =
                productService;
    }


    // =====================================================
    // GET - PRODUCTS
    // Pagination + Sorting
    // =====================================================

    @GetMapping("/products")
    public Page<Product> getProducts(

            @RequestParam(
                    defaultValue = "0"
            )
            int page,

            @RequestParam(
                    defaultValue = "10"
            )
            int size,

            @RequestParam(
                    defaultValue = "id"
            )
            String sortBy,

            @RequestParam(
                    defaultValue = "asc"
            )
            String direction) {

        return productService.getProducts(
                page,
                size,
                sortBy,
                direction
        );
    }


    // =====================================================
    // SEARCH - ALL FIELDS / NAME / PRICE / QUANTITY
    // =====================================================

    @GetMapping("/products/search")
    public Page<Product> searchProducts(

            @RequestParam String field,

            @RequestParam String value,

            @RequestParam(
                    defaultValue = "0"
            )
            int page,

            @RequestParam(
                    defaultValue = "10"
            )
            int size,

            @RequestParam(
                    defaultValue = "id"
            )
            String sortBy,

            @RequestParam(
                    defaultValue = "asc"
            )
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
    // GET - PRODUCT BY ID
    // =====================================================

    @GetMapping("/products/{id}")
    public Product getProductById(
            @PathVariable Long id) {

        return productService
                .getProductById(id);
    }


    // =====================================================
    // POST - ADD PRODUCT
    // =====================================================

    @PostMapping("/products")
    public Product addProduct(
            @Valid
            @RequestBody Product product) {

        return productService
                .addProduct(product);
    }


    // =====================================================
    // PUT - UPDATE PRODUCT
    // =====================================================

    @PutMapping("/products/{id}")
    public Product updateProduct(

            @PathVariable Long id,

            @Valid
            @RequestBody Product product) {

        return productService
                .updateProduct(
                        id,
                        product
                );
    }


    // =====================================================
    // DELETE - DELETE PRODUCT
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