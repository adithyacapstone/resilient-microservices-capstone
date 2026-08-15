package com.capstone.productservice.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import com.capstone.productservice.dto.ProductInventoryResponse;
import com.capstone.productservice.Product;
import com.capstone.productservice.ProductNotFoundException;
import com.capstone.productservice.repository.ProductRepository;
import org.springframework.web.client.RestClient;
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final RestClient restClient;

    public ProductService(
            ProductRepository productRepository,
            RestClient restClient) {

        this.productRepository = productRepository;
        this.restClient = restClient;
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
    // SEARCH PRODUCTS - PAGINATION + SORTING
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


        // =================================================
        // SEARCH BY PRODUCT ID
        // =================================================

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


        // =================================================
        // SEARCH BY PRODUCT NAME
        // =================================================

        if (field.equalsIgnoreCase("name")) {

            return productRepository
                    .findByNameContainingIgnoreCase(
                            value,
                            pageable
                    );
        }


        // =================================================
        // SEARCH BY PRICE
        // =================================================

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


        // =================================================
        // SEARCH BY QUANTITY
        // =================================================

        if (field.equalsIgnoreCase("quantity")) {

            try {

                int quantity =
                        Integer.parseInt(value);

                return productRepository
                        .findByQuantity(
                                quantity,
                                pageable
                        );

            } catch (NumberFormatException e) {

                return Page.empty(pageable);
            }
        }


        // =================================================
        // ALL FIELDS SEARCH
        // =================================================

        if (field.equalsIgnoreCase("all")) {

            String searchValue =
                    value.trim();

            List<Specification<Product>> specifications =
                    new ArrayList<>();


            // ---------------------------------------------
            // Always search PRODUCT NAME
            // ---------------------------------------------

            specifications.add(
                    (root, query, criteriaBuilder) ->
                            criteriaBuilder.like(
                                    criteriaBuilder.lower(
                                            root.get("name")
                                    ),
                                    "%" +
                                    searchValue.toLowerCase() +
                                    "%"
                            )
            );


            // ---------------------------------------------
            // If numeric, also search ID
            // ---------------------------------------------

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

                // Not a number.
                // No ID search required.
            }


            // ---------------------------------------------
            // If numeric, also search PRICE
            // ---------------------------------------------

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

                // Not a number.
                // No price search required.
            }


            // ---------------------------------------------
            // If integer, also search QUANTITY
            // ---------------------------------------------

            try {

                int quantity =
                        Integer.parseInt(searchValue);

                specifications.add(
                        (root, query, criteriaBuilder) ->
                                criteriaBuilder.equal(
                                        root.get("quantity"),
                                        quantity
                                )
                );

            } catch (NumberFormatException ignored) {

                // Not an integer.
                // No quantity search required.
            }


            // ---------------------------------------------
            // Combine all conditions with OR
            // ---------------------------------------------

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


        // =================================================
        // DEFAULT
        // =================================================

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
                    sortBy.equals("price")
                    ||
                    sortBy.equals("quantity")
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
    // ADD PRODUCT
    // =====================================================

    public Product addProduct(Product product) {

        return productRepository.save(product);
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

        existingProduct.setPrice(
                updatedProduct.getPrice()
        );

        existingProduct.setQuantity(
                updatedProduct.getQuantity()
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
    // OLD SEARCH METHODS
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


    public List<Product> searchByPrice(
            double price) {

        return productRepository
                .findByPrice(
                        price,
                        Pageable.unpaged()
                )
                .getContent();
    }


    public List<Product> searchByQuantity(
            int quantity) {

        return productRepository
                .findByQuantity(
                        quantity,
                        Pageable.unpaged()
                )
                .getContent();
    }
    
 // =====================================================
 // GET INVENTORY FROM INVENTORY SERVICE
 // =====================================================

 public String getInventory(Long productId) {

     return restClient
             .get()
             .uri(
                 "http://localhost:8081/inventory/product/"
                 + productId
             )
             .retrieve()
             .body(String.class);
 }
//=====================================================
//GET PRODUCT + INVENTORY DETAILS
//=====================================================

public ProductInventoryResponse getProductInventoryDetails(
      Long id) {

  Product product =
          getProductById(id);

  Object inventory =
          restClient
                  .get()
                  .uri(
                          "http://localhost:8081/inventory/product/"
                          + id
                  )
                  .retrieve()
                  .body(Object.class);

  return new ProductInventoryResponse(
          product,
          inventory
  );
}
}