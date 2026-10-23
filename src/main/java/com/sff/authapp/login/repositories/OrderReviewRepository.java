package com.sff.authapp.login.repositories;

import com.sff.authapp.login.entities.OrderReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderReviewRepository extends JpaRepository<OrderReview, Long> {

    Optional<OrderReview> findByOrderId(Long orderId);

    boolean existsByOrderId(Long orderId);
}
