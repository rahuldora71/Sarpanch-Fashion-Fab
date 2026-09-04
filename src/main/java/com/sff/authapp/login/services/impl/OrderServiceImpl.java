package com.sff.authapp.login.services.impl;

import com.sff.authapp.login.dtos.order.OrderRequestDto;
import com.sff.authapp.login.dtos.order.OrderResponse;
import com.sff.authapp.login.entities.GarmentStatus;
import com.sff.authapp.login.entities.Order;
import com.sff.authapp.login.entities.User;
import com.sff.authapp.login.exceptions.ResourceNotFoundException;
import com.sff.authapp.login.repositories.OrderRepository;
import com.sff.authapp.login.repositories.UserRepository;
import com.sff.authapp.login.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    private final UserRepository userRepository;


    // ============================================================
    // CREATE
    // ============================================================

    @Override
    public OrderResponse createOrder(
            OrderRequestDto request
    ) {

        User customer = findCustomerById(
                request.customerId()
        );

        Order order = Order.builder()
                .customer(customer)
                .orderDate(request.orderDate())
                .sampleBookPageNumber(
                        normalize(request.sampleBookPageNumber())
                )
                .build();

        Order savedOrder =
                orderRepository.save(order);

        return toResponse(savedOrder);
    }


    // ============================================================
    // GET BY ID
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(
            Long orderId
    ) {

        return toResponse(
                findOrderById(orderId)
        );
    }


    // ============================================================
    // GET ALL
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(
            Pageable pageable
    ) {

        return orderRepository
                .findAll(pageable)
                .map(this::toResponse);
    }


    // ============================================================
    // GET BY CUSTOMER
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByCustomer(
            Long customerId,
            Pageable pageable
    ) {

        findCustomerById(customerId);

        return orderRepository
                .findByCustomerId(
                        customerId,
                        pageable
                )
                .map(this::toResponse);
    }


    // ============================================================
    // GET BY DATE
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByDate(
            LocalDate orderDate,
            Pageable pageable
    ) {

        return orderRepository
                .findByOrderDate(
                        orderDate,
                        pageable
                )
                .map(this::toResponse);
    }


    // ============================================================
    // GET BY DATE RANGE
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByDateRange(
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException(
                    "Start date and end date are required"
            );
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "Start date cannot be after end date"
            );
        }

        return orderRepository
                .findByOrderDateBetween(
                        startDate,
                        endDate,
                        pageable
                )
                .map(this::toResponse);
    }


    // ============================================================
    // SEARCH SAMPLE BOOK PAGE
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> searchOrdersBySampleBookPage(
            String keyword,
            Pageable pageable
    ) {

        String normalizedKeyword =
                normalize(keyword);

        if (normalizedKeyword == null) {
            return getAllOrders(pageable);
        }

        return orderRepository
                .findBySampleBookPageNumberContainingIgnoreCase(
                        normalizedKeyword,
                        pageable
                )
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByGarmentStatus(
            GarmentStatus status,
            Pageable pageable
    ) {

        if (status == null) {
            throw new IllegalArgumentException(
                    "Garment status is required"
            );
        }

        return orderRepository
                .findByGarmentStatus(status, pageable)
                .map(this::toResponse);
    }


    // ============================================================
    // UPDATE
    // ============================================================

    @Override
    public OrderResponse updateOrder(
            Long orderId,
            OrderRequestDto request
    ) {

        Order order =
                findOrderById(orderId);

        // --------------------------------------------------------
        // CUSTOMER
        // --------------------------------------------------------

        if (
                request.customerId() != null
                        && !request.customerId()
                        .equals(order.getCustomer().getId())
        ) {

            User customer =
                    findCustomerById(
                            request.customerId()
                    );

            order.setCustomer(customer);
        }


        // --------------------------------------------------------
        // ORDER DATE
        // --------------------------------------------------------

        if (request.orderDate() != null) {

            order.setOrderDate(
                    request.orderDate()
            );
        }


        // --------------------------------------------------------
        // SAMPLE BOOK PAGE
        // --------------------------------------------------------

        if (request.sampleBookPageNumber() != null) {

            order.setSampleBookPageNumber(
                    normalize(
                            request.sampleBookPageNumber()
                    )
            );
        }


        Order updatedOrder =
                orderRepository.save(order);

        return toResponse(updatedOrder);
    }


    // ============================================================
    // DELETE
    // ============================================================

    @Override
    public void deleteOrder(
            Long orderId
    ) {

        Order order =
                findOrderById(orderId);

        /*
         * Currently using hard delete.
         *
         * Before enabling this in production, we should decide
         * whether orders should be immutable/historically preserved.
         */
        orderRepository.delete(order);
    }


    // ============================================================
    // FIND ORDER
    // ============================================================

    private Order findOrderById(
            Long orderId
    ) {

        return orderRepository
                .findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order not found with id: "
                                        + orderId
                        )
                );
    }


    // ============================================================
    // FIND CUSTOMER
    // ============================================================

    private User findCustomerById(
            Long customerId
    ) {

        return userRepository
                .findById(customerId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Customer not found with id: "
                                        + customerId
                        )
                );
    }


    // ============================================================
    // MAPPER
    // ============================================================

    private OrderResponse toResponse(
            Order order
    ) {

        User customer =
                order.getCustomer();

        return new OrderResponse(
                order.getId(),
                customer != null
                        ? customer.getId()
                        : null,
                customer != null
                        ? customer.getName()
                        : null,
                customer != null
                        ? customer.getCustomerCode()
                        : null,
                order.getOrderDate(),
                order.getSampleBookPageNumber(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }


    // ============================================================
    // NORMALIZE
    // ============================================================

    private String normalize(
            String value
    ) {

        if (value == null) {
            return null;
        }

        String normalized =
                value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}