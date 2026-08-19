package com.capstone.productservice.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.capstone.productservice.Product;
import com.capstone.productservice.ProductNotFoundException;
import com.capstone.productservice.dto.ProductCreateRequest;
import com.capstone.productservice.dto.ProductInventoryResponse;
import com.capstone.productservice.repository.ProductRepository;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final RestClient restClient;

    private final String inventoryServiceUrl;

    public ProductService(
            ProductRepository productRepository,
            RestClient restClient,
            @Value("${inventory.service.url:http://localhost:8081}")
            String inventoryServiceUrl) {

        this.productRepository = productRepository;
        this.restClient = restClient;
        this.inventoryServiceUrl = inventoryServiceUrl;
    }


    // =====================================================
    // GET PRODUCTS - PAGINATION + SORTING
    // =====================================================

    public Page<Product> getProducts(
            int page,
            int size,
            String sortBy,
            String direction) {

        if (!isValidSortField(sortBy)) {
            sortBy = "id";
        }

        Sort.Direction sortDirection =
                direction.equalsIgnoreCase("desc")
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(sortDirection, sortBy)
        );

        return productRepository.findAll(pageable);
    }


    // =====================================================
    // SEARCH PRODUCTS
    // =====================================================

    public Page<Product> searchProducts(
            String field,
            String value,
            int page,
            int size,
            String sortBy,
            String direction) {

        if (!isValidSortField(sortBy)) {
            sortBy = "id";
        }

        Sort.Direction sortDirection =
                direction.equalsIgnoreCase("desc")
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(sortDirection, sortBy)
        );

        if (field.equalsIgnoreCase("id")) {

            try {
                Long id = Long.parseLong(value);

                Specification<Product> specification =
                        (root, query, criteriaBuilder) ->
                                criteriaBuilder.equal(
                                        root.get("id"),
                                        id
                                );

                return productRepository.findAll(
                        specification,
                        pageable
                );

            } catch (NumberFormatException e) {
                return Page.empty(pageable);
            }
        }


        if (field.equalsIgnoreCase("name")) {

            return productRepository
                    .findByNameContainingIgnoreCase(
                            value,
                            pageable
                    );
        }


        if (field.equalsIgnoreCase("sku")) {

            return productRepository
                    .findBySkuContainingIgnoreCase(
                            value,
                            pageable
                    );
        }


        if (field.equalsIgnoreCase("category")) {

            return productRepository
                    .findByCategoryContainingIgnoreCase(
                            value,
                            pageable
                    );
        }


        if (field.equalsIgnoreCase("price")) {

            try {

                double price =
                        Double.parseDouble(value);

                return productRepository
                        .findByPrice(
                                price,
                                pageable
                        );

            } catch (NumberFormatException e) {
                return Page.empty(pageable);
            }
        }


        if (field.equalsIgnoreCase("all")) {

            String searchValue =
                    value.trim();

            List<Specification<Product>> specifications =
                    new ArrayList<>();


            // Product name
            specifications.add(
                    (root, query, criteriaBuilder) ->
                            criteriaBuilder.like(
                                    criteriaBuilder.lower(
                                            root.get("name")
                                    ),
                                    "%"
                                    + searchValue.toLowerCase()
                                    + "%"
                            )
            );


            // SKU
            specifications.add(
                    (root, query, criteriaBuilder) ->
                            criteriaBuilder.like(
                                    criteriaBuilder.lower(
                                            root.get("sku")
                                    ),
                                    "%"
                                    + searchValue.toLowerCase()
                                    + "%"
                            )
            );


            // Category
            specifications.add(
                    (root, query, criteriaBuilder) ->
                            criteriaBuilder.like(
                                    criteriaBuilder.lower(
                                            root.get("category")
                                    ),
                                    "%"
                                    + searchValue.toLowerCase()
                                    + "%"
                            )
            );


            // ID
            try {

                Long id =
                        Long.parseLong(searchValue);

                specifications.add(
                        (root, query, criteriaBuilder) ->
                                criteriaBuilder.equal(
                                        root.get("id"),
                                        id
                                )
                );

            } catch (NumberFormatException ignored) {
            }


            // Price
            try {

                double price =
                        Double.parseDouble(searchValue);

                specifications.add(
                        (root, query, criteriaBuilder) ->
                                criteriaBuilder.equal(
                                        root.get("price"),
                                        price
                                )
                );

            } catch (NumberFormatException ignored) {
            }


            Specification<Product> finalSpecification =
                    specifications
                            .stream()
                            .reduce(
                                    Specification::or
                            )
                            .orElse(null);

            if (finalSpecification == null) {
                return Page.empty(pageable);
            }

            return productRepository.findAll(
                    finalSpecification,
                    pageable
            );
        }


        return productRepository.findAll(pageable);
    }


    // =====================================================
    // VALID SORT FIELDS
    // =====================================================

    private boolean isValidSortField(
            String sortBy) {

        return sortBy != null
                &&
                (
                    sortBy.equals("id")
                    ||
                    sortBy.equals("name")
                    ||
                    sortBy.equals("sku")
                    ||
                    sortBy.equals("category")
                    ||
                    sortBy.equals("price")
                );
    }


    // =====================================================
    // GET PRODUCT BY ID
    // =====================================================

    public Product getProductById(Long id) {

        return productRepository
                .findById(id)
                .orElseThrow(
                        () -> new ProductNotFoundException(
                                "Product with ID "
                                + id
                                + " not found"
                        )
                );
    }


    // =====================================================
    // ADD NEW PRODUCT + INITIAL STOCK
    // =====================================================

    public Product addProduct(
            ProductCreateRequest request) {

        Product product =
                new Product(
                        request.getName(),
                        request.getSku(),
                        request.getCategory(),
                        request.getDescription(),
                        request.getPrice()
                );

        Product savedProduct =
                productRepository.save(product);

        try {

            restClient
                    .post()
                    .uri(
                            inventoryServiceUrl
                                    + "/inventory"
                    )
                    .body(
                            new InventoryRequest(
                                    savedProduct.getId(),
                                    request.getInitialQuantity(),
                                    0,
                                    2
                            )
                    )
                    .retrieve()
                    .toBodilessEntity();

        } catch (Exception e) {

            // Prevent a Product without its initial
            // Inventory record.

            productRepository.delete(savedProduct);

            throw new RuntimeException(
                    "Product could not be added to Inventory Service: "
                    + e.getMessage()
            );
        }

        return savedProduct;
    }


    // =====================================================
    // RECEIVE ADDITIONAL STOCK
    // =====================================================

    public String receiveStock(
            Long productId,
            int quantity) {

        getProductById(productId);

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Received quantity must be greater than 0"
            );
        }

        return restClient
                .put()
                .uri(
                        inventoryServiceUrl
                                + "/inventory/product/"
                                + productId
                                + "/receive?quantity="
                                + quantity
                )
                .retrieve()
                .body(String.class);
    }


    // =====================================================
    // UPDATE PRODUCT
    // =====================================================

    public Product updateProduct(
            Long id,
            Product updatedProduct) {

        Product existingProduct =
                getProductById(id);

        existingProduct.setName(
                updatedProduct.getName()
        );

        existingProduct.setSku(
                updatedProduct.getSku()
        );

        existingProduct.setCategory(
                updatedProduct.getCategory()
        );

        existingProduct.setDescription(
                updatedProduct.getDescription()
        );

        existingProduct.setPrice(
                updatedProduct.getPrice()
        );

        return productRepository.save(
                existingProduct
        );
    }


    // =====================================================
    // DELETE PRODUCT
    // =====================================================

    public void deleteProduct(Long id) {

        Product product =
                getProductById(id);

        productRepository.delete(product);
    }


    // =====================================================
    // SEARCH BY NAME
    // =====================================================

    public List<Product> searchByName(
            String name) {

        return productRepository
                .findByNameContainingIgnoreCase(
                        name,
                        Pageable.unpaged()
                )
                .getContent();
    }


    // =====================================================
    // SEARCH BY PRICE
    // =====================================================

    public List<Product> searchByPrice(
            double price) {

        return productRepository
                .findByPrice(
                        price,
                        Pageable.unpaged()
                )
                .getContent();
    }


    // =====================================================
    // GET INVENTORY
    // =====================================================

    public String getInventory(Long productId) {

        return restClient
                .get()
                .uri(
                        inventoryServiceUrl
                                + "/inventory/product/"
                                + productId
                )
                .retrieve()
                .body(String.class);
    }


    // =====================================================
    // GET PRODUCT + INVENTORY DETAILS
    // =====================================================

    public ProductInventoryResponse getProductInventoryDetails(
            Long id) {

        Product product =
                getProductById(id);

        Object inventory =
                restClient
                        .get()
                        .uri(
                                inventoryServiceUrl
                                        + "/inventory/product/"
                                        + id
                        )
                        .retrieve()
                        .body(Object.class);

        return new ProductInventoryResponse(
                product,
                inventory
        );
    }


    // =====================================================
    // INTERNAL INVENTORY REQUEST
    // =====================================================

    private static class InventoryRequest {

        private Long productId;
        private int availableStock;
        private int reservedStock;
        private int reorderLevel;

        public InventoryRequest(
                Long productId,
                int availableStock,
                int reservedStock,
                int reorderLevel) {

            this.productId = productId;
            this.availableStock = availableStock;
            this.reservedStock = reservedStock;
            this.reorderLevel = reorderLevel;
        }

        public Long getProductId() {
            return productId;
        }

        public int getAvailableStock() {
            return availableStock;
        }

        public int getReservedStock() {
            return reservedStock;
        }

        public int getReorderLevel() {
            return reorderLevel;
        }
    }
}