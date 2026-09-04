//package com.sff.authapp.login.entities;
//
//import jakarta.persistence.*;
//import lombok.*;
//
//import java.time.LocalDateTime;
//
//@Entity
//@Table(
//        name = "order_status_history",
//        indexes = {
//                @Index(
//                        name = "idx_order_history_order",
//                        columnList = "order_id"
//                ),
//                @Index(
//                        name = "idx_order_history_date",
//                        columnList = "changed_at"
//                )
//        }
//)
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class OrderStatusHistory {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//
//    @ManyToOne(fetch = FetchType.LAZY, optional = false)
//    @JoinColumn(
//            name = "order_id",
//            nullable = false
//    )
//    private Order order;
//
//
//    @Enumerated(EnumType.STRING)
//    @Column(
//            name = "old_status",
//            length = 30
//    )
//    private Order.Status oldStatus;
//
//
//    @Enumerated(EnumType.STRING)
//    @Column(
//            name = "new_status",
//            nullable = false,
//            length = 30
//    )
//    private Order.Status newStatus;
//
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "changed_by")
//    private User changedBy;
//
//
//    @Column(
//            name = "changed_at",
//            nullable = false
//    )
//    private LocalDateTime changedAt;
//
//
//    @Column(columnDefinition = "TEXT")
//    private String notes;
//
//
//    @PrePersist
//    protected void onCreate() {
//
//        if (changedAt == null) {
//            changedAt = LocalDateTime.now();
//        }
//    }
//}