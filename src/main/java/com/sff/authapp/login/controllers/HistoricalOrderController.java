package com.sff.authapp.login.controllers;

import com.sff.authapp.login.dtos.order.HistoricalCustomerSummaryDto;
import com.sff.authapp.login.dtos.order.HistoricalOrderRequestDto;
import com.sff.authapp.login.dtos.order.HistoricalOrderResponseDto;
import com.sff.authapp.login.dtos.order.NextOrderNumberResponseDto;
import com.sff.authapp.login.entities.User;
import com.sff.authapp.login.repositories.UserRepository;
import com.sff.authapp.login.services.HistoricalOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders/historical")
@RequiredArgsConstructor
@Tag(
        name = "Historical Orders Entry",
        description = "High-speed endpoints for rapid legacy paper slip digitization"
)
public class HistoricalOrderController {

    private final HistoricalOrderService historicalOrderService;
    private final UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(summary = "Create historical order", description = "Digitize a previous paper order slip with customer matching and slip image")
    public ResponseEntity<HistoricalOrderResponseDto> createHistoricalOrder(
            @Valid @RequestBody HistoricalOrderRequestDto request,
            Authentication authentication
    ) {
        Long authenticatedUserId = extractUserId(authentication);
        HistoricalOrderResponseDto response = historicalOrderService.createHistoricalOrder(request, authenticatedUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/next-number")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(summary = "Get next suggested order number", description = "Returns the next auto-incrementing order number")
    public ResponseEntity<NextOrderNumberResponseDto> getNextOrderNumber() {
        return ResponseEntity.ok(historicalOrderService.getNextOrderNumber());
    }

    @GetMapping("/customers-by-phone")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(summary = "Find customers by phone", description = "Searches customers matching a phone number substring for rapid auto-selection")
    public ResponseEntity<List<HistoricalCustomerSummaryDto>> findCustomersByPhone(
            @RequestParam String phone
    ) {
        return ResponseEntity.ok(historicalOrderService.findCustomersByPhone(phone));
    }

    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(summary = "Get recent digitized orders", description = "Retrieves recent digitized historical orders for the live session table")
    public ResponseEntity<List<HistoricalOrderResponseDto>> getRecentHistoricalOrders(
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(historicalOrderService.getRecentHistoricalOrders(limit));
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        if (authentication.getPrincipal() instanceof User user) {
            return user.getId();
        }
        if (authentication.getName() != null) {
            return userRepository.findByEmail(authentication.getName())
                    .map(User::getId)
                    .orElse(null);
        }
        return null;
    }
}
