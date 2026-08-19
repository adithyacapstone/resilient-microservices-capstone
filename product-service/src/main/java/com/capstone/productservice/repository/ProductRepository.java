package com.capstone.productservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.capstone.productservice.Product;

public interface ProductRepository
        extends JpaRepository<Product, Long>,
                JpaSpecificationExecutor<Product> {

    Page<Product> findByNameContainingIgnoreCase(
            String name,
            Pageable pageable);

    Page<Product> findByPrice(
            double price,
            Pageable pageable);

    Page<Product> findBySkuContainingIgnoreCase(
            String sku,
            Pageable pageable);

    Page<Product> findByCategoryContainingIgnoreCase(
            String category,
            Pageable pageable);
}