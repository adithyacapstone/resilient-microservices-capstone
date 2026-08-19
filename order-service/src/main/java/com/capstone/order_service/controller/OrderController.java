package com.capstone.order_service.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.capstone.order_service.Order;
import com.capstone.order_service.service.OrderService;
import com.capstone.order_service.service.OrderService.InventoryResponse;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;


    public OrderController(OrderService orderService) {

        this.orderService = orderService;
    }


    // =====================================================
    // GET ALL ORDERS
    // =====================================================

    @GetMapping
    public List<Order> getAllOrders() {

        return orderService.getAllOrders();
    }


    // =====================================================
    // GET AVAILABLE PRODUCTS
    // =====================================================

    @GetMapping("/available-products")
    public List<InventoryResponse> getAvailableProducts() {

        return orderService.getAvailableProducts();
    }


    // =====================================================
    // GET ORDER BY ID
    // =====================================================

    @GetMapping("/{id}")
    public Order getOrderById(
            @PathVariable Long id) {

        return orderService.getOrderById(id);
    }


    // =====================================================
    // CREATE ORDER
    // =====================================================

    @PostMapping
    public ResponseEntity<Order> createOrder(
            @RequestBody Order order) {

        Order savedOrder =
                orderService.createOrder(order);

        return new ResponseEntity<>(
                savedOrder,
                HttpStatus.CREATED
        );
    }


    // =====================================================
    // CONFIRM ORDER
    // =====================================================

    @PutMapping("/{id}/confirm")
    public Order confirmOrder(
            @PathVariable Long id) {

        return orderService.confirmOrder(id);
    }


    // =====================================================
    // CANCEL ORDER
    // =====================================================

    @DeleteMapping("/{id}")
    public Order cancelOrder(
            @PathVariable Long id) {

        return orderService.cancelOrder(id);
    }
}