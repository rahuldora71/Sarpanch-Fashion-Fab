package com.sff.authapp.login.services.impl;

import com.sff.authapp.login.dtos.payment.AdditionalChargeRequestDto;
import com.sff.authapp.login.dtos.payment.AdditionalChargeResponse;
import com.sff.authapp.login.entities.AdditionalCharge;
import com.sff.authapp.login.entities.Order;
import com.sff.authapp.login.exceptions.ResourceNotFoundException;
import com.sff.authapp.login.repositories.AdditionalChargeRepository;
import com.sff.authapp.login.repositories.OrderRepository;
import com.sff.authapp.login.services.AdditionalChargeService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AdditionalChargeServiceImpl
        implements AdditionalChargeService {

    private final AdditionalChargeRepository
            additionalChargeRepository;

    private final OrderRepository orderRepository;


    // ============================================================
    // ADD CHARGE
    // ============================================================

    @Override
    public AdditionalChargeResponse addCharge(
            Long orderId,
            AdditionalChargeRequestDto request
    ) {

        Order order =
                orderRepository.findById(orderId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Order not found with id: "
                                                + orderId
                                )
                        );


        AdditionalCharge charge =
                AdditionalCharge.builder()
                        .order(order)
                        .description(
                                request.description()
                        )
                        .amount(
                                request.amount()
                        )
                        .build();


        AdditionalCharge saved =
                additionalChargeRepository.save(charge);


        return toResponse(saved);
    }


    // ============================================================
    // GET ORDER CHARGES
    // ============================================================

    @Override
    @Transactional
    public List<AdditionalChargeResponse> getOrderCharges(
            Long orderId
    ) {

        validateOrder(orderId);

        return additionalChargeRepository
                .findByOrderIdOrderByCreatedAtAscIdAsc(orderId)
                .stream()
                .map(this::toResponse)
                .toList();
    }


    // ============================================================
    // GET CHARGE
    // ============================================================

    @Override
    @Transactional
    public AdditionalChargeResponse getChargeById(
            Long chargeId
    ) {

        AdditionalCharge charge =
                additionalChargeRepository.findById(chargeId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Additional charge not found with id: "
                                                + chargeId
                                )
                        );

        return toResponse(charge);
    }


    // ============================================================
    // UPDATE CHARGE
    // ============================================================

    @Override
    public AdditionalChargeResponse updateCharge(
            Long chargeId,
            AdditionalChargeRequestDto request
    ) {

        AdditionalCharge charge =
                additionalChargeRepository.findById(chargeId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Additional charge not found with id: "
                                                + chargeId
                                )
                        );


        charge.setDescription(
                request.description()
        );

        charge.setAmount(
                request.amount()
        );


        return toResponse(charge);
    }


    // ============================================================
    // DELETE CHARGE
    // ============================================================

    @Override
    public void deleteCharge(
            Long chargeId
    ) {

        AdditionalCharge charge =
                additionalChargeRepository.findById(chargeId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Additional charge not found with id: "
                                                + chargeId
                                )
                        );

        additionalChargeRepository.delete(charge);
    }


    // ============================================================
    // VALIDATE ORDER
    // ============================================================

    private void validateOrder(
            Long orderId
    ) {

        if (!orderRepository.existsById(orderId)) {

            throw new ResourceNotFoundException(
                    "Order not found with id: "
                            + orderId
            );
        }
    }


    // ============================================================
    // RESPONSE MAPPER
    // ============================================================

    private AdditionalChargeResponse toResponse(
            AdditionalCharge charge
    ) {

        return new AdditionalChargeResponse(

                charge.getId(),

                charge.getOrder() != null
                        ? charge.getOrder().getId()
                        : null,

                charge.getDescription(),

                charge.getAmount(),

                charge.getCreatedAt()
        );
    }
}