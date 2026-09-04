package com.sff.authapp.login.services;

import com.sff.authapp.login.dtos.garmentaddon.GarmentAddOnRequestDto;
import com.sff.authapp.login.dtos.garmentaddon.GarmentAddOnResponse;

import java.util.List;

public interface GarmentAddOnService {

    GarmentAddOnResponse addAddOn(
            GarmentAddOnRequestDto request
    );

    GarmentAddOnResponse getById(
            Long garmentAddOnId
    );

    List<GarmentAddOnResponse> getByGarment(
            Long garmentId
    );

    GarmentAddOnResponse updateQuantity(
            Long garmentAddOnId,
            Integer quantity
    );

    void delete(
            Long garmentAddOnId
    );
}