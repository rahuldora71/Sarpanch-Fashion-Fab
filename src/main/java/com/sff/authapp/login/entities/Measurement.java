//package com.sff.authapp.login.entities;
//
//import com.fasterxml.jackson.annotation.JsonInclude;
//import jakarta.persistence.*;
//import lombok.*;
//import org.hibernate.annotations.JdbcTypeCode;
//import org.hibernate.type.SqlTypes;
//
//import java.time.LocalDateTime;
//import java.util.LinkedHashMap;
//import java.util.Map;
//
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//@Entity
//@Table(
//        name = "measurements",
//        indexes = {
//
//                @Index(
//                        name = "idx_measurement_user",
//                        columnList = "user_id"
//                ),
//
//                @Index(
//                        name = "idx_measurement_service",
//                        columnList = "service_id"
//                ),
//
//                @Index(
//                        name = "idx_measurement_user_service_created",
//                        columnList = "user_id, service_id, created_at"
//                ),
//
//                @Index(
//                        name = "idx_measurement_taken_by",
//                        columnList = "taken_by_user_id"
//                )
//        }
//)
//public class Measurement {
//
//    // ============================================================
//    // PRIMARY KEY
//    // ============================================================
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "measurement_id")
//    private Long id;
//
//
//    // ============================================================
//    // CUSTOMER
//    // ============================================================
//
//    @ManyToOne(fetch = FetchType.LAZY, optional = false)
//    @JoinColumn(
//            name = "user_id",
//            nullable = false
//    )
//    private User user;
//
//
//    // ============================================================
//    // SERVICE
//    // ============================================================
//
//    /**
//     * Service for which this measurement was taken.
//     *
//     * Example:
//     *
//     * Shirt
//     * Coat
//     * Pant
//     */
//    @ManyToOne(fetch = FetchType.LAZY, optional = false)
//    @JoinColumn(
//            name = "service_id",
//            nullable = false
//    )
//    private TailoringService service;
//
//
//    // ============================================================
//    // MEASUREMENT VALUES
//    // ============================================================
//
//    /**
//     * Measurement values keyed by Field ID.
//     *
//     * Example:
//     *
//     * {
//     *     "1": 38,
//     *     "2": 17,
//     *     "3": 24.5,
//     *     "4": 29
//     * }
//     *
//     * Where:
//     *
//     * 1 = Chest
//     * 2 = Shoulder
//     * 3 = Sleeve
//     * 4 = Shirt Length
//     */
//    @JdbcTypeCode(SqlTypes.JSON)
//    @Column(
//            name = "measurement_fields",
//            columnDefinition = "json"
//    )
//    @Builder.Default
//    @JsonInclude(JsonInclude.Include.NON_EMPTY)
//    private Map<String, Object> measurementFields =
//            new LinkedHashMap<>();
//
//
//    // ============================================================
//    // TAKEN BY
//    // ============================================================
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "taken_by_user_id")
//    private User takenBy;
//
//
//    // ============================================================
//    // INFORMATION
//    // ============================================================
//
//    @Column(
//            nullable = false
//    )
//    @Builder.Default
//    private boolean editable = true;
//
//
//    @Column(
//            columnDefinition = "TEXT"
//    )
//    private String note;
//
//
//    @Column(
//            name = "alteration_note",
//            columnDefinition = "TEXT"
//    )
//    private String alterationNote;
//
//
//    // ============================================================
//    // AUDIT
//    // ============================================================
//
//    @Column(
//            name = "created_at",
//            nullable = false,
//            updatable = false
//    )
//    private LocalDateTime createdAt;
//
//
//    @Column(
//            name = "updated_at",
//            nullable = false
//    )
//    private LocalDateTime updatedAt;
//
//
//    // ============================================================
//    // JPA LIFECYCLE
//    // ============================================================
//
//    @PrePersist
//    protected void onCreate() {
//
//        LocalDateTime now = LocalDateTime.now();
//
//        if (createdAt == null) {
//            createdAt = now;
//        }
//
//        updatedAt = now;
//    }
//
//
//    @PreUpdate
//    protected void onUpdate() {
//        updatedAt = LocalDateTime.now();
//    }
//}