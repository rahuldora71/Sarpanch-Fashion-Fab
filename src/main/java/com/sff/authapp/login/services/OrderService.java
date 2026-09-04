package com.sff.authapp.login.services;

import com.sff.authapp.login.dtos.order.OrderRequestDto;
import com.sff.authapp.login.dtos.order.OrderResponse;
import com.sff.authapp.login.entities.GarmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface OrderService {

    OrderResponse createOrder(
            OrderRequestDto request
    );

    OrderResponse getOrderById(
            Long orderId
    );

    Page<OrderResponse> getAllOrders(
            Pageable pageable
    );

    Page<OrderResponse> getOrdersByCustomer(
            Long customerId,
            Pageable pageable
    );

    Page<OrderResponse> getOrdersByDate(
            LocalDate orderDate,
            Pageable pageable
    );

    Page<OrderResponse> getOrdersByDateRange(
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );

    Page<OrderResponse> searchOrdersBySampleBookPage(
            String keyword,
            Pageable pageable
    );

    Page<OrderResponse> getOrdersByGarmentStatus(
            GarmentStatus status,
            Pageable pageable
    );

    OrderResponse updateOrder(
            Long orderId,
            OrderRequestDto request
    );

    void deleteOrder(
            Long orderId
    );
}