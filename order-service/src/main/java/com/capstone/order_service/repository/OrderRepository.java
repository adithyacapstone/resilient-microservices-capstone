package com.capstone.order_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.capstone.order_service.Order;

public interface OrderRepository
        extends JpaRepository<Order, Long> {
}