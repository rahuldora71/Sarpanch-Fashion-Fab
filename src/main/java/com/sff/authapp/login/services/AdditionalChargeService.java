package com.sff.authapp.login.services;

import com.sff.authapp.login.dtos.payment.AdditionalChargeRequestDto;
import com.sff.authapp.login.dtos.payment.AdditionalChargeResponse;

import java.util.List;

public interface AdditionalChargeService {

    AdditionalChargeResponse addCharge(
            Long orderId,
            AdditionalChargeRequestDto request
    );

    List<AdditionalChargeResponse> getOrderCharges(
            Long orderId
    );

    AdditionalChargeResponse getChargeById(
            Long chargeId
    );

    AdditionalChargeResponse updateCharge(
            Long chargeId,
            AdditionalChargeRequestDto request
    );

    void deleteCharge(
            Long chargeId
    );
}