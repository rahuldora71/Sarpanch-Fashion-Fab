package com.sff.authapp.login.services.impl;

import com.sff.authapp.login.dtos.garment.GarmentRequestDto;
import com.sff.authapp.login.dtos.garment.GarmentResponse;
import com.sff.authapp.login.dtos.garment.GarmentUpdateRequestDto;
import com.sff.authapp.login.entities.Garment;
import com.sff.authapp.login.entities.GarmentStatus;
import com.sff.authapp.login.entities.Order;
import com.sff.authapp.login.entities.TailoringService;
import com.sff.authapp.login.repositories.GarmentRepository;
import com.sff.authapp.login.repositories.OrderRepository;
import com.sff.authapp.login.repositories.TailoringServiceRepository;
import com.sff.authapp.login.services.GarmentService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class GarmentServiceImpl implements GarmentService {

    private final GarmentRepository garmentRepository;

    private final OrderRepository orderRepository;

    private final TailoringServiceRepository tailoringServiceRepository;


    @Override
    public GarmentResponse createGarment(
            GarmentRequestDto request
    ) {

        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Order not found"
                        )
                );

        TailoringService service =
                tailoringServiceRepository.findById(request.serviceId())
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Tailoring service not found"
                                )
                        );

        if (!service.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This tailoring service is inactive"
            );
        }

        Garment garment = Garment.builder()
                .order(order)
                .service(service)

                // Use custom price if provided,
                // otherwise use the current service price.
                .price(
                        request.price() != null
                                ? request.price()
                                : service.getPrice()
                )

                .sampleNumber(request.sampleNumber())

                .customerFabric(
                        request.customerFabric() != null
                                && request.customerFabric()
                )

                .tryDate(request.tryDate())
                .dueDate(request.dueDate())
                .note(request.note())
                .alterationNote(request.alterationNote())

                .status(GarmentStatus.FABRIC_CUT)
                .build();

        Garment saved = garmentRepository.save(garment);

        return mapToResponse(saved);
    }


    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public GarmentResponse getGarmentById(
            Long garmentId
    ) {

        Garment garment = findGarment(garmentId);

        return mapToResponse(garment);
    }


    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<GarmentResponse> getGarmentsByOrder(
            Long orderId
    ) {

        if (!orderRepository.existsById(orderId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Order not found"
            );
        }

        return garmentRepository
                .findByOrderIdOrderByIdAsc(orderId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public Page<GarmentResponse> getGarmentsByOrder(
            Long orderId,
            Pageable pageable
    ) {

        if (!orderRepository.existsById(orderId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Order not found"
            );
        }

        return garmentRepository
                .findByOrderId(orderId, pageable)
                .map(this::mapToResponse);
    }


    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public Page<GarmentResponse> getGarmentsByStatus(
            GarmentStatus status,
            Pageable pageable
    ) {

        return garmentRepository
                .findByStatus(status, pageable)
                .map(this::mapToResponse);
    }


    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public Page<GarmentResponse> getGarmentsByService(
            Long serviceId,
            Pageable pageable
    ) {

        return garmentRepository
                .findByServiceId(serviceId, pageable)
                .map(this::mapToResponse);
    }


    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public Page<GarmentResponse> getCustomerGarments(
            Long customerId,
            Pageable pageable
    ) {

        return garmentRepository
                .findByCustomerId(customerId, pageable)
                .map(this::mapToResponse);
    }


    @Override
    public GarmentResponse updateGarment(
            Long garmentId,
            GarmentUpdateRequestDto request
    ) {

        Garment garment = findGarment(garmentId);

        if (request.serviceId() != null) {

            TailoringService service =
                    tailoringServiceRepository.findById(
                            request.serviceId()
                    ).orElseThrow(() ->
                            new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "Tailoring service not found"
                            )
                    );

            if (!service.isActive()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "This tailoring service is inactive"
                );
            }

            /*
             * Important:
             * We do NOT automatically change the historical garment price
             * when the service is changed.
             *
             * Changing service after order creation should be treated
             * carefully because price is an order-time snapshot.
             */
            garment.setService(service);
        }

        if (request.sampleNumber() != null) {
            garment.setSampleNumber(
                    request.sampleNumber()
            );
        }

        if (request.customerFabric() != null) {
            garment.setCustomerFabric(
                    request.customerFabric()
            );
        }

        if (request.tryDate() != null) {
            garment.setTryDate(
                    request.tryDate()
            );
        }

        if (request.dueDate() != null) {
            garment.setDueDate(
                    request.dueDate()
            );
        }

        if (request.status() != null) {
            garment.setStatus(
                    request.status()
            );
        }

        if (request.note() != null) {
            garment.setNote(
                    request.note()
            );
        }

        if (request.alterationNote() != null) {
            garment.setAlterationNote(
                    request.alterationNote()
            );
        }

        if (request.alterationDeliveryDate() != null) {
            garment.setAlterationDeliveryDate(
                    request.alterationDeliveryDate()
            );
        }

        return mapToResponse(
                garmentRepository.save(garment)
        );
    }


    @Override
    public GarmentResponse updateStatus(
            Long garmentId,
            GarmentStatus status
    ) {

        Garment garment = findGarment(garmentId);

        garment.setStatus(status);

        return mapToResponse(
                garmentRepository.save(garment)
        );
    }


    @Override
    public void deleteGarment(
            Long garmentId
    ) {

        Garment garment = findGarment(garmentId);

        garmentRepository.delete(garment);
    }


    private Garment findGarment(
            Long garmentId
    ) {

        return garmentRepository.findById(garmentId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Garment not found"
                        )
                );
    }


    private GarmentResponse mapToResponse(
            Garment garment
    ) {

        Long measurementId = null;

        if (garment.getGarmentMeasurement() != null) {
            measurementId =
                    garment.getGarmentMeasurement().getId();
        }

        return new GarmentResponse(
                garment.getId(),
                garment.getOrder().getId(),
                garment.getService().getId(),
                garment.getService().getName(),
                garment.getPrice(),
                garment.getSampleNumber(),
                measurementId,
                garment.getStatus(),
                garment.isCustomerFabric(),
                garment.getTryDate(),
                garment.getDueDate(),
                getUserId(garment.getCuttingBy()),
                getUserId(garment.getMadeBy()),
                getUserId(garment.getDeliveredBy()),
                garment.getAlterationDeliveryDate(),
                garment.getAlterationNote(),
                garment.getNote(),
                garment.getCreatedAt(),
                garment.getUpdatedAt()
        );
    }


    private Long getUserId(
            com.sff.authapp.login.entities.User user
    ) {

        return user == null
                ? null
                : user.getId();
    }
}