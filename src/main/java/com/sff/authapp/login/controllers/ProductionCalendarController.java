package com.sff.authapp.login.controllers;

import com.sff.authapp.login.dtos.production.ProductionCalendarDtos.*;
import com.sff.authapp.login.entities.BlockedDate;
import com.sff.authapp.login.entities.Holiday;
import com.sff.authapp.login.entities.ProductionCapacity;
import com.sff.authapp.login.entities.User;
import com.sff.authapp.login.entities.WorkerAttendance;
import com.sff.authapp.login.services.ProductionCalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/production")
@RequiredArgsConstructor
@Tag(
        name = "Production & Delivery Calendar",
        description = "APIs for production capacity planning, delivery calendar, blocked dates, and holidays"
)
public class ProductionCalendarController {

    private final ProductionCalendarService calendarService;

    // ============================================================
    // MONTHLY PRODUCTION CALENDAR
    // ============================================================

    @GetMapping("/calendar")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(
            summary = "Get monthly production calendar",
            description = "Returns capacity, scheduled garments, and workload indicators for each day in month"
    )
    public ResponseEntity<ProductionCalendarResponse> getMonthlyCalendar(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        LocalDate now = LocalDate.now();
        int y = year != null ? year : now.getYear();
        int m = month != null ? month : now.getMonthValue();
        return ResponseEntity.ok(calendarService.getMonthlyCalendar(y, m));
    }

    // ============================================================
    // DAY DETAILS
    // ============================================================

    @GetMapping("/calendar/{date}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    @Operation(
            summary = "Get day production details",
            description = "Returns all garments due, worker attendance, and capacity reductions for a date"
    )
    public ResponseEntity<DayDetailResponse> getDayDetails(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(calendarService.getDayDetails(date));
    }

    // ============================================================
    // DELIVERY ALLOWED CHECK
    // ============================================================

    @GetMapping("/delivery-allowed/{date}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    public ResponseEntity<Boolean> isDeliveryAllowed(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(calendarService.isDeliveryAllowed(date));
    }

    // ============================================================
    // BLOCKED DATES
    // ============================================================

    @GetMapping("/blocked-dates")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    public ResponseEntity<List<BlockedDate>> getBlockedDates() {
        return ResponseEntity.ok(calendarService.getAllBlockedDates());
    }

    @PostMapping("/blocked-dates")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<BlockedDate> blockDate(
            @RequestBody BlockDateRequest request,
            Authentication authentication
    ) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(calendarService.blockDate(request, userId));
    }

    @DeleteMapping("/blocked-dates/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Void> unblockDate(@PathVariable Long id) {
        calendarService.unblockDate(id);
        return ResponseEntity.noContent().build();
    }

    // ============================================================
    // HOLIDAYS
    // ============================================================

    @GetMapping("/holidays")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    public ResponseEntity<List<Holiday>> getHolidays() {
        return ResponseEntity.ok(calendarService.getAllHolidays());
    }

    @PostMapping("/holidays")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Holiday> createHoliday(@RequestBody HolidayRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(calendarService.createHoliday(request));
    }

    @PutMapping("/holidays/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Holiday> updateHoliday(
            @PathVariable Long id,
            @RequestBody HolidayRequest request
    ) {
        return ResponseEntity.ok(calendarService.updateHoliday(id, request));
    }

    @DeleteMapping("/holidays/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Void> deleteHoliday(@PathVariable Long id) {
        calendarService.deleteHoliday(id);
        return ResponseEntity.noContent().build();
    }

    // ============================================================
    // WORKER ATTENDANCE
    // ============================================================

    @PostMapping("/attendance")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<WorkerAttendance> recordAttendance(
            @RequestBody WorkerAttendanceRequest request
    ) {
        return ResponseEntity.ok(calendarService.recordWorkerAttendance(request));
    }

    @GetMapping("/attendance/{date}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'WORKER')")
    public ResponseEntity<List<WorkerAttendance>> getAttendanceForDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(calendarService.getWorkerAttendanceForDate(date));
    }

    // ============================================================
    // CAPACITY ADJUSTMENT (e.g. POWER CUT)
    // ============================================================

    @PostMapping("/capacity-override")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ProductionCapacity> setCapacityOverride(
            @RequestBody CapacityConfigRequest request
    ) {
        return ResponseEntity.ok(calendarService.setCapacityConfig(request));
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user.getId();
        }
        return null;
    }
}
