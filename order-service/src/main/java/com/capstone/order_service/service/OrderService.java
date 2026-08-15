package com.capstone.order_service.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import com.capstone.order_service.Order;
import com.capstone.order_service.repository.OrderRepository;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    private final RestClient restClient;

    public OrderService(OrderRepository orderRepository) {

        this.orderRepository = orderRepository;

        this.restClient = RestClient.builder()
                .baseUrl("http://localhost:8081")
                .build();
    }


    // =====================================================
    // GET ALL ORDERS
    // =====================================================

    public List<Order> getAllOrders() {

        return orderRepository.findAll();
    }


    // =====================================================
    // GET ORDER BY ID
    // =====================================================

    public Order getOrderById(Long id) {

        return orderRepository
                .findById(id)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Order with ID " + id + " not found"
                        )
                );
    }


    // =====================================================
    // CREATE ORDER
    // =====================================================

    public Order createOrder(Order order) {

        if (order.getQuantity() <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Quantity must be greater than 0"
            );
        }

        try {

            // Reserve stock in Inventory Service

            restClient
                    .put()
                    .uri(uriBuilder -> uriBuilder
                            .path("/inventory/product/{productId}/reserve")
                            .queryParam(
                                    "quantity",
                                    order.getQuantity()
                            )
                            .build(order.getProductId()))
                    .retrieve()
                    .toBodilessEntity();

        } catch (Exception e) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Insufficient stock"
            );
        }


        // Save order only after inventory reservation succeeds

        order.setStatus("CREATED");

        order.setCreatedAt(
                LocalDateTime.now()
        );

        return orderRepository.save(order);
    }


    // =====================================================
    // CANCEL ORDER
    // =====================================================

    public Order cancelOrder(Long id) {

        Order order = getOrderById(id);


        // Already cancelled?

        if ("CANCELLED".equals(order.getStatus())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Order is already cancelled"
            );
        }


        // Release reserved stock

        try {

            restClient
                    .put()
                    .uri(uriBuilder -> uriBuilder
                            .path("/inventory/product/{productId}/release")
                            .queryParam(
                                    "quantity",
                                    order.getQuantity()
                            )
                            .build(order.getProductId()))
                    .retrieve()
                    .toBodilessEntity();

        } catch (Exception e) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unable to release inventory"
            );
        }


        // Change order status

        order.setStatus("CANCELLED");

        return orderRepository.save(order);
    }
}