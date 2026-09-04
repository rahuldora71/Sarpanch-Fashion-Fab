package com.sff.authapp.login.controllers;

import com.sff.authapp.login.dtos.order.OrderRequestDto;
import com.sff.authapp.login.dtos.order.OrderResponse;
import com.sff.authapp.login.entities.GarmentStatus;
import com.sff.authapp.login.services.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(
        name = "Order Management",
        description = "APIs for managing customer orders"
)
public class OrderController {

    private final OrderService orderService;


    // ============================================================
    // CREATE
    // ============================================================

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(
            summary = "Create order",
            description = "Creates a new order for a customer."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Order created successfully",
                    content = @Content(
                            schema = @Schema(
                                    implementation = OrderResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Customer not found"
            )
    })
    public ResponseEntity<OrderResponse> createOrder(
            @Valid
            @RequestBody
            OrderRequestDto request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        orderService.createOrder(request)
                );
    }


    // ============================================================
    // GET ALL
    // ============================================================

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(
            summary = "Get all orders",
            description = "Returns a paginated list of orders."
    )
    public ResponseEntity<Page<OrderResponse>> getAllOrders(

            @PageableDefault(
                    size = 20,
                    sort = "createdAt"
            )
            Pageable pageable

    ) {

        return ResponseEntity.ok(
                orderService.getAllOrders(pageable)
        );
    }


    // ============================================================
    // GET BY ID
    // ============================================================

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(
            summary = "Get order by ID",
            description = "Returns a single order by its ID."
    )
    public ResponseEntity<OrderResponse> getOrderById(

            @Parameter(
                    description = "Order ID",
                    required = true
            )
            @PathVariable
            Long orderId

    ) {

        return ResponseEntity.ok(
                orderService.getOrderById(orderId)
        );
    }


    // ============================================================
    // GET BY CUSTOMER
    // ============================================================

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(
            summary = "Get customer orders",
            description = "Returns all orders belonging to a customer."
    )
    public ResponseEntity<Page<OrderResponse>> getOrdersByCustomer(

            @PathVariable
            Long customerId,

            @PageableDefault(
                    size = 20,
                    sort = "createdAt"
            )
            Pageable pageable

    ) {

        return ResponseEntity.ok(
                orderService.getOrdersByCustomer(
                        customerId,
                        pageable
                )
        );
    }


    // ============================================================
    // GET BY DATE
    // ============================================================

    @GetMapping("/date/{orderDate}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(
            summary = "Get orders by date",
            description = "Returns orders created for a specific order date."
    )
    public ResponseEntity<Page<OrderResponse>> getOrdersByDate(

            @PathVariable
            LocalDate orderDate,

            @PageableDefault(
                    size = 20,
                    sort = "createdAt"
            )
            Pageable pageable

    ) {

        return ResponseEntity.ok(
                orderService.getOrdersByDate(
                        orderDate,
                        pageable
                )
        );
    }


    // ============================================================
    // GET BY DATE RANGE
    // ============================================================

    @GetMapping("/date-range")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(
            summary = "Get orders by date range",
            description = "Returns orders between two order dates."
    )
    public ResponseEntity<Page<OrderResponse>> getOrdersByDateRange(

            @RequestParam
            LocalDate startDate,

            @RequestParam
            LocalDate endDate,

            @PageableDefault(
                    size = 20,
                    sort = "createdAt"
            )
            Pageable pageable

    ) {

        return ResponseEntity.ok(
                orderService.getOrdersByDateRange(
                        startDate,
                        endDate,
                        pageable
                )
        );
    }
//    =========================
//    GET BY GARMENT STATUS
//    ========================

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(
            summary = "Get orders by garment status",
            description = """
                Returns orders containing at least one garment
                with the specified garment status.
                """
    )
    public ResponseEntity<Page<OrderResponse>> getOrdersByGarmentStatus(

            @Parameter(
                    description = "Garment status",
                    required = true
            )
            @PathVariable
            GarmentStatus status,

            @PageableDefault(
                    size = 20,
                    sort = "createdAt"
            )
            Pageable pageable

    ) {

        return ResponseEntity.ok(
                orderService.getOrdersByGarmentStatus(
                        status,
                        pageable
                )
        );
    }


    // ============================================================
    // SEARCH
    // ============================================================

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(
            summary = "Search orders",
            description = "Searches orders using the sample book page number."
    )
    public ResponseEntity<Page<OrderResponse>> searchOrders(

            @RequestParam
            String keyword,

            @PageableDefault(
                    size = 20,
                    sort = "createdAt"
            )
            Pageable pageable

    ) {

        return ResponseEntity.ok(
                orderService.searchOrdersBySampleBookPage(
                        keyword,
                        pageable
                )
        );
    }


    // ============================================================
    // UPDATE
    // ============================================================

    @PatchMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(
            summary = "Update order",
            description = "Updates order-level information."
    )
    public ResponseEntity<OrderResponse> updateOrder(

            @PathVariable
            Long orderId,

            @Valid
            @RequestBody
            OrderRequestDto request

    ) {

        return ResponseEntity.ok(
                orderService.updateOrder(
                        orderId,
                        request
                )
        );
    }


    // ============================================================
    // DELETE
    // ============================================================

    @DeleteMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(
            summary = "Delete order",
            description = "Deletes an order."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Order deleted successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Order not found"
            )
    })
    public ResponseEntity<Void> deleteOrder(

            @PathVariable
            Long orderId

    ) {

        orderService.deleteOrder(orderId);

        return ResponseEntity.noContent().build();
    }
}