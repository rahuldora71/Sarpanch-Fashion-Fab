package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.Garment;
import com.sff.authapp.login.entities.GarmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * Cutting Queue queries - fetches garments that need to be cut,
 * sorted by due date for production planning.
 */
public interface CuttingQueueRepository
        extends JpaRepository<Garment, Long> {

    /**
     * Fetch all garments with FABRIC_CUT status (waiting to be cut),
     * including order, customer, service, measurement, and add-ons.
     * Sorted by due date ascending so urgent garments appear first.
     */
    @Query("""
        SELECT DISTINCT g
        FROM Garment g
        JOIN FETCH g.order o
        JOIN FETCH o.customer c
        JOIN FETCH g.service s
        LEFT JOIN FETCH g.garmentMeasurement gm
        LEFT JOIN FETCH g.addOns ga
        LEFT JOIN FETCH ga.addOn a
        WHERE g.status = :status
        ORDER BY g.dueDate ASC NULLS LAST, g.id ASC
        """)
    List<Garment> findGarmentsForCuttingQueue(
            @Param("status") GarmentStatus status
    );

    /**
     * Fetch garments pending cutting with due dates in a given range.
     * Used by the calendar view to show upcoming cutting work.
     */
    @Query("""
        SELECT DISTINCT g
        FROM Garment g
        JOIN FETCH g.order o
        JOIN FETCH o.customer c
        JOIN FETCH g.service s
        LEFT JOIN FETCH g.garmentMeasurement gm
        LEFT JOIN FETCH g.addOns ga
        LEFT JOIN FETCH ga.addOn a
        WHERE g.status = :status
          AND g.dueDate BETWEEN :start AND :end
        ORDER BY g.dueDate ASC, g.id ASC
        """)
    List<Garment> findGarmentsForCuttingQueueByDateRange(
            @Param("status") GarmentStatus status,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    /**
     * Count garments scheduled for delivery on a specific date
     * regardless of status - used for calendar capacity display.
     */
    @Query("""
        SELECT COUNT(g)
        FROM Garment g
        WHERE g.dueDate = :date
        """)
    long countGarmentsDueOnDate(@Param("date") LocalDate date);

    /**
     * Count completed/delivered garments on a specific date.
     */
    @Query("""
        SELECT COUNT(g)
        FROM Garment g
        WHERE g.dueDate = :date
          AND g.status IN ('DELIVERED', 'READY')
        """)
    long countCompletedGarmentsDueOnDate(@Param("date") LocalDate date);

    /**
     * Fetch a garment with all details needed for the cutting workspace.
     */
    @Query("""
        SELECT g
        FROM Garment g
        JOIN FETCH g.order o
        JOIN FETCH o.customer c
        JOIN FETCH g.service s
        LEFT JOIN FETCH g.garmentMeasurement gm
        LEFT JOIN FETCH g.addOns ga
        LEFT JOIN FETCH ga.addOn a
        WHERE g.id = :garmentId
        """)
    java.util.Optional<Garment> findGarmentWithFullDetails(
            @Param("garmentId") Long garmentId
    );

    /**
     * Fetch all garments of an order with full details for cutting workspace.
     */
    @Query("""
        SELECT DISTINCT g
        FROM Garment g
        JOIN FETCH g.order o
        JOIN FETCH o.customer c
        JOIN FETCH g.service s
        LEFT JOIN FETCH g.garmentMeasurement gm
        LEFT JOIN FETCH g.addOns ga
        LEFT JOIN FETCH ga.addOn a
        WHERE g.order.id = :orderId
        ORDER BY g.id ASC
        """)
    List<Garment> findGarmentsWithFullDetailsByOrderId(
            @Param("orderId") Long orderId
    );
}
