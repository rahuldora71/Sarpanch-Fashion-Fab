package com.sff.authapp.login.services;

import com.sff.authapp.login.dtos.production.ProductionCalendarDtos.*;
import com.sff.authapp.login.entities.BlockedDate;
import com.sff.authapp.login.entities.Holiday;
import com.sff.authapp.login.entities.ProductionCapacity;
import com.sff.authapp.login.entities.WorkerAttendance;

import java.time.LocalDate;
import java.util.List;

public interface ProductionCalendarService {

    ProductionCalendarResponse getMonthlyCalendar(int year, int month);

    DayDetailResponse getDayDetails(LocalDate date);

    // Blocked dates
    BlockedDate blockDate(BlockDateRequest request, Long userId);
    void unblockDate(Long id);
    List<BlockedDate> getAllBlockedDates();

    // Holidays
    Holiday createHoliday(HolidayRequest request);
    Holiday updateHoliday(Long id, HolidayRequest request);
    void deleteHoliday(Long id);
    List<Holiday> getAllHolidays();

    // Worker Attendance & Availability
    WorkerAttendance recordWorkerAttendance(WorkerAttendanceRequest request);
    List<WorkerAttendance> getWorkerAttendanceForDate(LocalDate date);

    // Capacity Adjustment
    ProductionCapacity setCapacityConfig(CapacityConfigRequest request);

    // Delivery date validation check
    boolean isDeliveryAllowed(LocalDate date);
}
