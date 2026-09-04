package com.sff.authapp.login.services.impl;

import com.sff.authapp.login.dtos.garmentaddon.GarmentAddOnRequestDto;
import com.sff.authapp.login.dtos.garmentaddon.GarmentAddOnResponse;
import com.sff.authapp.login.entities.AddOn;
import com.sff.authapp.login.entities.Garment;
import com.sff.authapp.login.entities.GarmentAddOn;
import com.sff.authapp.login.repositories.AddOnRepository;
import com.sff.authapp.login.repositories.GarmentAddOnRepository;
import com.sff.authapp.login.repositories.GarmentRepository;
import com.sff.authapp.login.services.GarmentAddOnService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class GarmentAddOnServiceImpl
        implements GarmentAddOnService {

    private final GarmentAddOnRepository garmentAddOnRepository;

    private final GarmentRepository garmentRepository;

    private final AddOnRepository addOnRepository;


    @Override
    public GarmentAddOnResponse addAddOn(
            GarmentAddOnRequestDto request
    ) {

        Garment garment =
                garmentRepository.findById(
                        request.garmentId()
                ).orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Garment not found"
                        )
                );

        AddOn addOn =
                addOnRepository.findById(
                        request.addOnId()
                ).orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Add-on not found"
                        )
                );

        if (!addOn.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This add-on is inactive"
            );
        }

        BigDecimal unitPrice =
                addOn.getPrice();

        BigDecimal totalPrice =
                unitPrice.multiply(
                        BigDecimal.valueOf(
                                request.quantity()
                        )
                );

        BigDecimal commission =
                addOn.getTailorCommission() == null
                        ? BigDecimal.ZERO
                        : addOn.getTailorCommission()
                        .multiply(
                                BigDecimal.valueOf(
                                        request.quantity()
                                )
                        );

        GarmentAddOn garmentAddOn =
                GarmentAddOn.builder()
                        .garment(garment)
                        .addOn(addOn)

                        // Historical snapshots
                        .addOnName(addOn.getName())
                        .unitPrice(unitPrice)

                        .quantity(request.quantity())
                        .totalPrice(totalPrice)
                        .tailorCommission(commission)

                        .note(request.note())
                        .build();

        return mapToResponse(
                garmentAddOnRepository.save(garmentAddOn)
        );
    }


    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public GarmentAddOnResponse getById(
            Long garmentAddOnId
    ) {

        return mapToResponse(
                garmentAddOnRepository.findById(
                        garmentAddOnId
                ).orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Garment add-on not found"
                        )
                )
        );
    }


    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<GarmentAddOnResponse> getByGarment(
            Long garmentId
    ) {

        if (!garmentRepository.existsById(garmentId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Garment not found"
            );
        }

        return garmentAddOnRepository
                .findByGarmentIdOrderByIdAsc(garmentId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    @Override
    public GarmentAddOnResponse updateQuantity(
            Long garmentAddOnId,
            Integer quantity
    ) {

        if (quantity == null || quantity < 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Quantity must be at least 1"
            );
        }

        GarmentAddOn garmentAddOn =
                garmentAddOnRepository.findById(
                        garmentAddOnId
                ).orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Garment add-on not found"
                        )
                );

        garmentAddOn.setQuantity(quantity);

        BigDecimal total =
                garmentAddOn.getUnitPrice()
                        .multiply(
                                BigDecimal.valueOf(quantity)
                        );

        garmentAddOn.setTotalPrice(total);

        BigDecimal commission =
                garmentAddOn.getAddOn()
                        .getTailorCommission() == null
                        ? BigDecimal.ZERO
                        : garmentAddOn.getAddOn()
                        .getTailorCommission()
                        .multiply(
                                BigDecimal.valueOf(
                                        quantity
                                )
                        );

        garmentAddOn.setTailorCommission(
                commission
        );

        return mapToResponse(
                garmentAddOnRepository.save(
                        garmentAddOn
                )
        );
    }


    @Override
    public void delete(
            Long garmentAddOnId
    ) {

        GarmentAddOn garmentAddOn =
                garmentAddOnRepository.findById(
                        garmentAddOnId
                ).orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Garment add-on not found"
                        )
                );

        garmentAddOnRepository.delete(
                garmentAddOn
        );
    }


    private GarmentAddOnResponse mapToResponse(
            GarmentAddOn entity
    ) {

        return new GarmentAddOnResponse(
                entity.getId(),
                entity.getGarment().getId(),
                entity.getAddOn().getId(),
                entity.getAddOnName(),
                entity.getUnitPrice(),
                entity.getQuantity(),
                entity.getTotalPrice(),
                entity.getTailorCommission(),
                entity.getNote(),
                entity.getCreatedAt()
        );
    }
}