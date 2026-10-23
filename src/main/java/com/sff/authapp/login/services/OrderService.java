package com.sff.authapp.login.services;

import com.sff.authapp.login.dtos.order.*;
import com.sff.authapp.login.entities.GarmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

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

    //    ===============
    //    Get Complete order
    //    ===============
    @Transactional(readOnly = true)
    OrderDetailsResponse getOrderDetails(Long orderId);

    Page<OrderResponse> getOrdersByCustomer(
            Long customerId,
            Pageable pageable
    );
    CompleteOrderResponse createCompleteOrder(
            CompleteOrderRequestDto request
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

    String getNextSampleBookPageNumber();

    CompleteOrderResponse editOrder(
            Long orderId,
            EditOrderRequestDto request
    );

    OrderDeliveryResponseDto deliverOrderGarments(
            Long orderId,
            OrderDeliveryRequestDto request
    );
}