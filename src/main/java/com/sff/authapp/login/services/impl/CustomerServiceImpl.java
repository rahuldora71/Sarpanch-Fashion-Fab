package com.sff.authapp.login.services.impl;

import com.sff.authapp.login.dtos.customer.CustomerCreateRequest;
import com.sff.authapp.login.dtos.customer.CustomerResponse;
import com.sff.authapp.login.dtos.customer.CustomerSummaryResponse;
import com.sff.authapp.login.dtos.customer.CustomerUpdateRequest;
import com.sff.authapp.login.dtos.customer.CustomerProfileResponse;
import com.sff.authapp.login.dtos.customer.CustomerProfileSummary;
import com.sff.authapp.login.dtos.customer.CustomerProfileOrder;
import com.sff.authapp.login.dtos.customer.CustomerProfileGarment;
import com.sff.authapp.login.dtos.customer.CustomerProfileMeasurement;
import com.sff.authapp.login.dtos.customer.CustomerProfilePayment;
import com.sff.authapp.login.dtos.customer.CustomerProfileAlteration;
import com.sff.authapp.login.entities.Provider;
import com.sff.authapp.login.entities.Role;
import com.sff.authapp.login.entities.User;
import com.sff.authapp.login.entities.Order;
import com.sff.authapp.login.entities.Garment;
import com.sff.authapp.login.entities.GarmentAddOn;
import com.sff.authapp.login.entities.GarmentMeasurement;
import com.sff.authapp.login.entities.GarmentStatus;
import com.sff.authapp.login.entities.Payment;
import com.sff.authapp.login.entities.AdditionalCharge;
import com.sff.authapp.login.exceptions.ResourceNotFoundException;
import com.sff.authapp.login.helpers.CustomerCodeHelper;
import com.sff.authapp.login.repositories.RoleRepository;
import com.sff.authapp.login.repositories.OrderRepository;
import com.sff.authapp.login.repositories.GarmentRepository;
import com.sff.authapp.login.repositories.GarmentMeasurementRepository;
import com.sff.authapp.login.entities.UserImage;
import com.sff.authapp.login.repositories.PaymentRepository;
import com.sff.authapp.login.repositories.GarmentAddOnRepository;
import com.sff.authapp.login.repositories.AdditionalChargeRepository;
import com.sff.authapp.login.repositories.UserRepository;
import com.sff.authapp.login.repositories.UserImageRepository;
import com.sff.authapp.login.image.service.ImageService;
import com.sff.authapp.login.dtos.customer.UserImageDto;
import com.sff.authapp.login.dtos.customer.UserImageCreateRequest;
import com.sff.authapp.login.dtos.customer.CustomerProfilePictureRequest;
import com.sff.authapp.login.services.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private static final String CUSTOMER_ROLE = "ROLE_CUSTOMER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    private final OrderRepository orderRepository;
    private final GarmentRepository garmentRepository;
    private final GarmentMeasurementRepository garmentMeasurementRepository;
    private final PaymentRepository paymentRepository;
    private final GarmentAddOnRepository garmentAddOnRepository;
    private final AdditionalChargeRepository additionalChargeRepository;
    private final UserImageRepository userImageRepository;
    private final ImageService imageService;


    // ============================================================
    // CREATE CUSTOMER
    // ============================================================

    @Override
    @Transactional
    public CustomerResponse createCustomer(
            CustomerCreateRequest request
    ) {

        String email = normalizeEmail(request.email());

        /*
         * Email is unique when supplied.
         */
        if (email != null
                && userRepository.existsByEmail(email)) {

            throw new IllegalArgumentException(
                    "User with this email already exists"
            );
        }

        Role customerRole = getCustomerRole();

        /*
         * Phone is intentionally NOT checked for uniqueness.
         *
         * Father and son can share the same phone number.
         */
        User customer =
                User.builder()
                        .name(request.name().trim())
                        .phone(request.phone())
                        .alternatePhone(
                                request.alternatePhone()
                        )
                        .email(email)
                        .address(request.address())
                        .city(request.city())
                        .notes(request.notes())
                        .image(request.image() != null && !request.image().isBlank() ? request.image().trim() : null)
                        .imagePublicId(request.imagePublicId() != null && !request.imagePublicId().isBlank() ? request.imagePublicId().trim() : null)
                        .password(null)
                        .enable(true)
                        .provider(Provider.LOCAL)
                        .roles(new HashSet<>())
                        .build();

        customer.getRoles()
                .add(customerRole);

        /*
         * Customer code will be generated
         * when the first order is created.
         */
        customer.setCustomerCode(null);

        User savedCustomer =
                userRepository.save(customer);

        if (request.images() != null && !request.images().isEmpty()) {
            List<UserImage> userImages = new ArrayList<>();
            for (UserImageCreateRequest imgReq : request.images()) {
                if (imgReq != null && imgReq.imageUrl() != null && !imgReq.imageUrl().isBlank()) {
                    UserImage userImage = UserImage.builder()
                            .user(savedCustomer)
                            .imageUrl(imgReq.imageUrl().trim())
                            .publicId(imgReq.publicId() != null && !imgReq.publicId().isBlank() ? imgReq.publicId().trim() : null)
                            .purpose(imgReq.purpose() != null && !imgReq.purpose().isBlank() ? imgReq.purpose().trim() : "OTHER")
                            .build();
                    userImages.add(userImage);
                }
            }
            if (!userImages.isEmpty()) {
                userImageRepository.saveAll(userImages);
            }
        }

        return mapToResponse(savedCustomer);
    }


    // ============================================================
    // GET ALL CUSTOMERS - PAGINATED
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerSummaryResponse> getAllCustomers(
            Pageable pageable
    ) {

        Role customerRole = getCustomerRole();

        Page<User> customers =
                userRepository.findAllCustomers(
                        customerRole.getName(),
                        pageable
                );

        /*
         * Calculate the complete summary for every customer.
         *
         * CustomerSummaryResponse contains:
         * - totalOrders
         * - totalSpent
         * - currentBalance
         * - lastOrderDate
         */
        return customers.map(this::mapToSummaryResponse);
    }


    // ============================================================
    // SEARCH CUSTOMERS - PAGINATED
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerSummaryResponse> searchCustomers(
            String keyword,
            Pageable pageable
    ) {

        if (keyword == null
                || keyword.isBlank()) {

            return getAllCustomers(pageable);
        }

        Page<User> customers =
                userRepository.searchCustomers(
                        keyword.trim(),
                        CUSTOMER_ROLE,
                        pageable
                );

        return customers.map(this::mapToSummaryResponse);
    }


    // ============================================================
    // SET NEW CUSTOMER CODE
    // ============================================================

    @Override
    @Transactional
    public CustomerResponse setNewCustomerCode(
            Long customerId
    ) {

        User customer = getCustomer(customerId);

        /*
         * Do not regenerate the code if the customer
         * already has one.
         */
        if (customer.getCustomerCode() != null
                && !customer.getCustomerCode().isBlank()) {

            return mapToResponse(customer);
        }

        String customerCode =
                CustomerCodeHelper.generateFromUserId(
                        customer.getId()
                );

        customer.setCustomerCode(customerCode);

        User savedCustomer =
                userRepository.save(customer);

        return mapToResponse(savedCustomer);
    }


    // ============================================================
    // GET CUSTOMER
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(
            Long customerId
    ) {

        User customer = getCustomer(customerId);

        return mapToResponse(customer);
    }


    // ============================================================
    // CUSTOMER PROFILE
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public CustomerProfileResponse getCustomerProfile(
            Long customerId
    ) {

        User customer = getCustomer(customerId);

        List<Order> orders =
                orderRepository
                        .findByCustomerIdOrderByOrderDateDescIdDesc(
                                customerId
                        );

        List<Garment> garments =
                garmentRepository
                        .findAllByCustomerIdForProfile(
                                customerId
                        );

        List<GarmentMeasurement> measurements =
                garmentMeasurementRepository
                        .findAllByUserIdForProfile(
                                customerId
                        );

        List<Payment> payments =
                paymentRepository
                        .findAllByCustomerIdForProfile(
                                customerId
                        );

        List<GarmentAddOn> garmentAddOns =
                garmentAddOnRepository
                        .findAllByCustomerIdForProfile(
                                customerId
                        );

        List<AdditionalCharge> charges =
                additionalChargeRepository
                        .findAllByCustomerIdForProfile(
                                customerId
                        );


        // ========================================================
        // GROUP GARMENTS BY ORDER
        // ========================================================

        Map<Long, List<Garment>> garmentsByOrder =
                new HashMap<>();

        for (Garment garment : garments) {

            if (garment.getOrder() != null) {

                garmentsByOrder
                        .computeIfAbsent(
                                garment.getOrder().getId(),
                                ignored -> new ArrayList<>()
                        )
                        .add(garment);
            }
        }


        // ========================================================
        // GROUP ADD-ONS BY GARMENT
        // ========================================================

        Map<Long, List<GarmentAddOn>> addOnsByGarment =
                new HashMap<>();

        for (GarmentAddOn addOn : garmentAddOns) {

            if (addOn.getGarment() != null) {

                addOnsByGarment
                        .computeIfAbsent(
                                addOn.getGarment().getId(),
                                ignored -> new ArrayList<>()
                        )
                        .add(addOn);
            }
        }


        // ========================================================
        // GROUP CHARGES BY ORDER
        // ========================================================

        Map<Long, List<AdditionalCharge>> chargesByOrder =
                new HashMap<>();

        for (AdditionalCharge charge : charges) {

            if (charge.getOrder() != null) {

                chargesByOrder
                        .computeIfAbsent(
                                charge.getOrder().getId(),
                                ignored -> new ArrayList<>()
                        )
                        .add(charge);
            }
        }


        // ========================================================
        // GROUP PAYMENTS BY ORDER
        // ========================================================

        Map<Long, List<Payment>> paymentsByOrder =
                new HashMap<>();

        for (Payment payment : payments) {

            if (payment.getOrder() != null) {

                paymentsByOrder
                        .computeIfAbsent(
                                payment.getOrder().getId(),
                                ignored -> new ArrayList<>()
                        )
                        .add(payment);
            }
        }


        // ========================================================
        // FIRST GARMENT FOR EACH MEASUREMENT
        // ========================================================

        Map<Long, Garment> firstGarmentByMeasurement =
                new HashMap<>();

        for (Garment garment : garments) {

            if (garment.getGarmentMeasurement() != null) {

                firstGarmentByMeasurement.putIfAbsent(
                        garment.getGarmentMeasurement().getId(),
                        garment
                );
            }
        }


        // ========================================================
        // PROFILE TOTALS
        // ========================================================

        BigDecimal totalSpent = BigDecimal.ZERO;
        BigDecimal currentBalance = BigDecimal.ZERO;

        LocalDate lastOrderDate = null;

        List<CustomerProfileOrder> orderResponses =
                new ArrayList<>();


        // ========================================================
        // BUILD ORDER RESPONSES
        // ========================================================

        for (Order order : orders) {

            if (lastOrderDate == null
                    || (
                    order.getOrderDate() != null
                            && order.getOrderDate()
                            .isAfter(lastOrderDate)
            )) {

                lastOrderDate = order.getOrderDate();
            }


            List<Garment> orderGarments =
                    garmentsByOrder.getOrDefault(
                            order.getId(),
                            List.of()
                    );


            BigDecimal garmentTotal =
                    orderGarments.stream()
                            .map(Garment::getPrice)
                            .filter(Objects::nonNull)
                            .reduce(
                                    BigDecimal.ZERO,
                                    BigDecimal::add
                            );


            BigDecimal addOnTotal =
                    orderGarments.stream()
                            .flatMap(
                                    g -> addOnsByGarment
                                            .getOrDefault(
                                                    g.getId(),
                                                    List.of()
                                            )
                                            .stream()
                            )
                            .map(GarmentAddOn::getTotalPrice)
                            .filter(Objects::nonNull)
                            .reduce(
                                    BigDecimal.ZERO,
                                    BigDecimal::add
                            );


            BigDecimal chargeTotal =
                    chargesByOrder
                            .getOrDefault(
                                    order.getId(),
                                    List.of()
                            )
                            .stream()
                            .map(AdditionalCharge::getAmount)
                            .filter(Objects::nonNull)
                            .reduce(
                                    BigDecimal.ZERO,
                                    BigDecimal::add
                            );


            BigDecimal discount =
                    order.getDiscount() != null
                            ? order.getDiscount()
                            : BigDecimal.ZERO;


            BigDecimal total =
                    garmentTotal
                            .add(addOnTotal)
                            .add(chargeTotal)
                            .subtract(discount);


            if (total.compareTo(BigDecimal.ZERO) < 0) {
                total = BigDecimal.ZERO;
            }


            BigDecimal paid =
                    paymentsByOrder
                            .getOrDefault(
                                    order.getId(),
                                    List.of()
                            )
                            .stream()
                            .map(Payment::getAmount)
                            .filter(Objects::nonNull)
                            .reduce(
                                    BigDecimal.ZERO,
                                    BigDecimal::add
                            );


            BigDecimal balance =
                    total.subtract(paid);

            if (balance.compareTo(BigDecimal.ZERO) < 0) {
                balance = BigDecimal.ZERO;
            }


            totalSpent =
                    totalSpent.add(total);

            currentBalance =
                    currentBalance.add(balance);


            LocalDate deliveryDate =
                    orderGarments.stream()
                            .map(Garment::getDueDate)
                            .filter(Objects::nonNull)
                            .max(LocalDate::compareTo)
                            .orElse(null);


            String status =
                    resolveOrderStatus(orderGarments);


            List<CustomerProfileGarment> garmentResponses =
                    orderGarments.stream()
                            .map(garment ->
                                    new CustomerProfileGarment(
                                            garment.getId(),

                                            garment.getService() != null
                                                    ? garment.getService().getId()
                                                    : null,

                                            garment.getService() != null
                                                    ? garment.getService().getName()
                                                    : null,

                                            garment.getStatus() != null
                                                    ? garment.getStatus().name()
                                                    : null
                                    )
                            )
                            .toList();


            String displayOrderNumber = order.getOrderNumber() != null
                    ? String.valueOf(order.getOrderNumber())
                    : formatOrderNumber(order.getId());

            orderResponses.add(
                    new CustomerProfileOrder(
                            order.getId(),
                            displayOrderNumber,
                            order.getOrderDate(),
                            deliveryDate,
                            status,
                            total,
                            balance,
                            garmentResponses,
                            order.getBillImageUrl()
                    )
            );
        }


        // ========================================================
        // MEASUREMENTS
        // ========================================================

        List<CustomerProfileMeasurement> measurementResponses =
                measurements.stream()
                        .map(measurement -> {

                            Garment garment =
                                    firstGarmentByMeasurement
                                            .get(measurement.getId());

                            return new CustomerProfileMeasurement(
                                    measurement.getId(),

                                    garment != null
                                            ? garment.getId()
                                            : null,

                                    garment != null
                                            && garment.getOrder() != null
                                            ? garment.getOrder().getId()
                                            : null,

                                    measurement.getService() != null
                                            ? measurement.getService().getId()
                                            : null,

                                    measurement.getService() != null
                                            ? measurement.getService().getName()
                                            : null,

                                    measurement.getMeasurementFields(),

                                    measurement.getCreatedAt()
                            );
                        })
                        .toList();


        // ========================================================
        // PAYMENTS
        // ========================================================

        List<CustomerProfilePayment> paymentResponses =
                payments.stream()
                        .map(payment ->
                                new CustomerProfilePayment(
                                        payment.getId(),

                                        payment.getOrder() != null
                                                ? payment.getOrder().getId()
                                                : null,

                                        payment.getAmount(),

                                        payment.getPaymentMethod() != null
                                                ? payment.getPaymentMethod().name()
                                                : null,

                                        payment.getPaymentType() != null
                                                ? payment.getPaymentType().name()
                                                : null,

                                        payment.getPaidAt()
                                )
                        )
                        .toList();


        // ========================================================
        // ALTERATIONS
        // ========================================================

        List<CustomerProfileAlteration> alterationResponses =
                garments.stream()
                        .filter(g ->
                                g.getStatus()
                                        == GarmentStatus.ALTERATION
                                        || g.getAlterationNote() != null
                                        || g.getAlterationDeliveryDate() != null
                        )
                        .map(g ->
                                new CustomerProfileAlteration(
                                        g.getId(),
                                        null,
                                        g.getAlterationNote(),
                                        g.getStatus() != null
                                                ? g.getStatus().name()
                                                : null,
                                        g.getOrder() != null
                                                ? g.getOrder().getId()
                                                : null,
                                        g.getCreatedAt() != null
                                                ? g.getCreatedAt().toLocalDate()
                                                : null
                                )
                        )
                        .toList();


        // ========================================================
        // PROFILE SUMMARY
        // ========================================================

        CustomerProfileSummary summary =
                new CustomerProfileSummary(
                        orders.size(),
                        totalSpent,
                        currentBalance,
                        lastOrderDate
                );


        // ========================================================
        // CUSTOMER POSTURE / REFERENCE IMAGES
        // ========================================================

        List<UserImage> customerImages =
                userImageRepository.findByUserIdOrderByCreatedAtDesc(customerId);

        List<UserImageDto> imageDtos = customerImages.stream()
                .map(this::mapToUserImageDto)
                .toList();


        // ========================================================
        // FINAL PROFILE RESPONSE
        // ========================================================

        return new CustomerProfileResponse(
                mapToResponse(customer),
                summary,
                orderResponses,
                measurementResponses,
                paymentResponses,
                alterationResponses,
                imageDtos
        );
    }


    // ============================================================
    // FORMAT ORDER NUMBER
    // ============================================================

    private String formatOrderNumber(Long orderId) {

        return orderId == null
                ? null
                : String.format(
                "SFF-%06d",
                orderId
        );
    }


    // ============================================================
    // RESOLVE ORDER STATUS
    // ============================================================

    private String resolveOrderStatus(
            List<Garment> garments
    ) {

        if (garments == null
                || garments.isEmpty()) {

            return "PENDING";
        }

        GarmentStatus firstStatus =
                garments.getFirst().getStatus();

        boolean same =
                garments.stream()
                        .allMatch(
                                g -> Objects.equals(
                                        g.getStatus(),
                                        firstStatus
                                )
                        );

        if (same && firstStatus != null) {
            return firstStatus.name();
        }

        boolean allDelivered = garments.stream()
                .allMatch(g -> g.getStatus() == GarmentStatus.DELIVERED);
        if (allDelivered) {
            return GarmentStatus.DELIVERED.name();
        }

        boolean anyDelivered = garments.stream()
                .anyMatch(g -> g.getStatus() == GarmentStatus.DELIVERED);
        boolean anyReady = garments.stream()
                .anyMatch(g -> g.getStatus() == GarmentStatus.READY);

        if (anyDelivered) {
            return "PARTIALLY_DELIVERED";
        }
        if (anyReady) {
            return "PARTIALLY_READY";
        }

        return "IN_PROGRESS";
    }


    // ============================================================
    // UPDATE CUSTOMER
    // ============================================================

    @Override
    @Transactional
    public CustomerResponse updateCustomer(
            Long customerId,
            CustomerUpdateRequest request
    ) {

        User customer =
                getCustomer(customerId);


        if (request.name() != null
                && !request.name().isBlank()) {

            customer.setName(
                    request.name().trim()
            );
        }


        /*
         * Phone does NOT need to be unique.
         */
        if (request.phone() != null
                && !request.phone().isBlank()) {

            customer.setPhone(
                    request.phone()
            );
        }


        if (request.alternatePhone() != null) {

            customer.setAlternatePhone(
                    request.alternatePhone()
            );
        }


        if (request.email() != null) {

            String email =
                    normalizeEmail(
                            request.email()
                    );


            if (email != null
                    && !email.equals(
                    customer.getEmail()
            )) {

                if (userRepository.existsByEmail(email)) {

                    throw new IllegalArgumentException(
                            "Email already belongs to another user"
                    );
                }
            }

            customer.setEmail(email);
        }


        if (request.address() != null) {

            customer.setAddress(
                    request.address()
            );
        }


        if (request.city() != null) {

            customer.setCity(
                    request.city()
            );
        }


        if (request.notes() != null) {

            customer.setNotes(
                    request.notes()
            );
        }

        if (request.image() != null) {
            customer.setImage(request.image().isBlank() ? null : request.image().trim());
        }

        if (request.imagePublicId() != null) {
            customer.setImagePublicId(request.imagePublicId().isBlank() ? null : request.imagePublicId().trim());
        }

        User updatedCustomer =
                userRepository.save(customer);

        return mapToResponse(updatedCustomer);
    }


    // ============================================================
    // DELETE CUSTOMER
    // ============================================================

    @Override
    @Transactional
    public void deleteCustomer(
            Long customerId
    ) {

        User customer =
                getCustomer(customerId);

        userRepository.delete(customer);
    }


    // ============================================================
    // GET CUSTOMER
    // ============================================================

    private User getCustomer(
            Long customerId
    ) {

        User user =
                userRepository
                        .findById(customerId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Customer not found with id: "
                                                + customerId
                                )
                        );


        boolean isCustomer =
                user.getRoles()
                        .stream()
                        .anyMatch(role ->
                                CUSTOMER_ROLE.equals(
                                        role.getName()
                                )
                        );


        if (!isCustomer) {

            throw new ResourceNotFoundException(
                    "Customer not found with id: "
                            + customerId
            );
        }


        return user;
    }


    // ============================================================
    // GET CUSTOMER ROLE
    // ============================================================

    private Role getCustomerRole() {

        return roleRepository
                .findByName(CUSTOMER_ROLE)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "ROLE_CUSTOMER not found"
                        )
                );
    }


    // ============================================================
    // CUSTOMER RESPONSE MAPPING
    // ============================================================

    private CustomerResponse mapToResponse(
            User user
    ) {

        return new CustomerResponse(
                user.getId(),
                user.getCustomerCode(),
                user.getName(),
                user.getPhone(),
                user.getAlternatePhone(),
                user.getEmail(),
                user.getImage(),
                user.getAddress(),
                user.getCity(),
                user.getNotes(),
                user.isEnable(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }


    // ============================================================
    // CUSTOMER SUMMARY RESPONSE MAPPING
    // ============================================================

    private CustomerSummaryResponse mapToSummaryResponse(
            User user
    ) {

        /*
         * Load all orders belonging to this customer.
         */
        List<Order> orders =
                orderRepository
                        .findByCustomerIdOrderByOrderDateDescIdDesc(
                                user.getId()
                        );


        /*
         * No orders means all financial values are zero.
         */
        if (orders.isEmpty()) {

            return new CustomerSummaryResponse(
                    user.getId(),
                    user.getCustomerCode(),
                    user.getName(),
                    user.getPhone(),
                    user.getEmail(),
                    user.getImage(),
                    0L,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    null
            );
        }


        /*
         * Load the customer's financial data.
         */
        List<Garment> garments =
                garmentRepository
                        .findAllByCustomerIdForProfile(
                                user.getId()
                        );

        List<Payment> payments =
                paymentRepository
                        .findAllByCustomerIdForProfile(
                                user.getId()
                        );

        List<GarmentAddOn> garmentAddOns =
                garmentAddOnRepository
                        .findAllByCustomerIdForProfile(
                                user.getId()
                        );

        List<AdditionalCharge> charges =
                additionalChargeRepository
                        .findAllByCustomerIdForProfile(
                                user.getId()
                        );


        // ========================================================
        // GROUP GARMENTS
        // ========================================================

        Map<Long, List<Garment>> garmentsByOrder =
                new HashMap<>();

        for (Garment garment : garments) {

            if (garment.getOrder() != null) {

                garmentsByOrder
                        .computeIfAbsent(
                                garment.getOrder().getId(),
                                ignored -> new ArrayList<>()
                        )
                        .add(garment);
            }
        }


        // ========================================================
        // GROUP ADD-ONS
        // ========================================================

        Map<Long, List<GarmentAddOn>> addOnsByGarment =
                new HashMap<>();

        for (GarmentAddOn addOn : garmentAddOns) {

            if (addOn.getGarment() != null) {

                addOnsByGarment
                        .computeIfAbsent(
                                addOn.getGarment().getId(),
                                ignored -> new ArrayList<>()
                        )
                        .add(addOn);
            }
        }


        // ========================================================
        // GROUP CHARGES
        // ========================================================

        Map<Long, List<AdditionalCharge>> chargesByOrder =
                new HashMap<>();

        for (AdditionalCharge charge : charges) {

            if (charge.getOrder() != null) {

                chargesByOrder
                        .computeIfAbsent(
                                charge.getOrder().getId(),
                                ignored -> new ArrayList<>()
                        )
                        .add(charge);
            }
        }


        // ========================================================
        // GROUP PAYMENTS
        // ========================================================

        Map<Long, List<Payment>> paymentsByOrder =
                new HashMap<>();

        for (Payment payment : payments) {

            if (payment.getOrder() != null) {

                paymentsByOrder
                        .computeIfAbsent(
                                payment.getOrder().getId(),
                                ignored -> new ArrayList<>()
                        )
                        .add(payment);
            }
        }


        // ========================================================
        // CALCULATE SUMMARY
        // ========================================================

        BigDecimal totalSpent =
                BigDecimal.ZERO;

        BigDecimal currentBalance =
                BigDecimal.ZERO;

        LocalDate lastOrderDate = null;


        for (Order order : orders) {

            // ----------------------------------------------------
            // Last Order Date
            // ----------------------------------------------------

            if (lastOrderDate == null
                    || (
                    order.getOrderDate() != null
                            && order.getOrderDate()
                            .isAfter(lastOrderDate)
            )) {

                lastOrderDate =
                        order.getOrderDate();
            }


            // ----------------------------------------------------
            // Garment Total
            // ----------------------------------------------------

            List<Garment> orderGarments =
                    garmentsByOrder.getOrDefault(
                            order.getId(),
                            List.of()
                    );


            BigDecimal garmentTotal =
                    orderGarments.stream()
                            .map(Garment::getPrice)
                            .filter(Objects::nonNull)
                            .reduce(
                                    BigDecimal.ZERO,
                                    BigDecimal::add
                            );


            // ----------------------------------------------------
            // Add-on Total
            // ----------------------------------------------------

            BigDecimal addOnTotal =
                    orderGarments.stream()
                            .flatMap(
                                    garment ->
                                            addOnsByGarment
                                                    .getOrDefault(
                                                            garment.getId(),
                                                            List.of()
                                                    )
                                                    .stream()
                            )
                            .map(
                                    GarmentAddOn::getTotalPrice
                            )
                            .filter(Objects::nonNull)
                            .reduce(
                                    BigDecimal.ZERO,
                                    BigDecimal::add
                            );


            // ----------------------------------------------------
            // Additional Charges
            // ----------------------------------------------------

            BigDecimal chargeTotal =
                    chargesByOrder
                            .getOrDefault(
                                    order.getId(),
                                    List.of()
                            )
                            .stream()
                            .map(
                                    AdditionalCharge::getAmount
                            )
                            .filter(Objects::nonNull)
                            .reduce(
                                    BigDecimal.ZERO,
                                    BigDecimal::add
                            );


            // ----------------------------------------------------
            // Discount
            // ----------------------------------------------------

            BigDecimal discount =
                    order.getDiscount() != null
                            ? order.getDiscount()
                            : BigDecimal.ZERO;


            // ----------------------------------------------------
            // Grand Total
            // ----------------------------------------------------

            BigDecimal total =
                    garmentTotal
                            .add(addOnTotal)
                            .add(chargeTotal)
                            .subtract(discount);


            if (total.compareTo(BigDecimal.ZERO) < 0) {

                total =
                        BigDecimal.ZERO;
            }


            // ----------------------------------------------------
            // Paid Amount
            // ----------------------------------------------------

            BigDecimal paid =
                    paymentsByOrder
                            .getOrDefault(
                                    order.getId(),
                                    List.of()
                            )
                            .stream()
                            .map(Payment::getAmount)
                            .filter(Objects::nonNull)
                            .reduce(
                                    BigDecimal.ZERO,
                                    BigDecimal::add
                            );


            // ----------------------------------------------------
            // Order Balance
            // ----------------------------------------------------

            BigDecimal balance =
                    total.subtract(paid);


            if (balance.compareTo(BigDecimal.ZERO) < 0) {

                balance =
                        BigDecimal.ZERO;
            }


            // ----------------------------------------------------
            // Customer Totals
            // ----------------------------------------------------

            totalSpent =
                    totalSpent.add(total);

            currentBalance =
                    currentBalance.add(balance);
        }


        // ========================================================
        // FINAL CUSTOMER SUMMARY
        // ========================================================

        return new CustomerSummaryResponse(
                user.getId(),
                user.getCustomerCode(),
                user.getName(),
                user.getPhone(),
                user.getEmail(),
                user.getImage(),

                orders.size(),

                totalSpent,

                currentBalance,

                lastOrderDate
        );
    }


    // ============================================================
    // EMAIL NORMALIZATION
    // ============================================================

    private String normalizeEmail(
            String email
    ) {

        if (email == null
                || email.isBlank()) {

            return null;
        }

        return email
                .trim()
                .toLowerCase();
    }

    // ============================================================
    // CUSTOMER IMAGES & PROFILE PICTURE IMPLEMENTATION
    // ============================================================

    @Override
    @Transactional
    public CustomerResponse updateProfilePicture(Long customerId, CustomerProfilePictureRequest request) {
        User customer = getCustomer(customerId);
        String oldPublicId = customer.getImagePublicId();

        customer.setImage(request.imageUrl().trim());
        customer.setImagePublicId(request.publicId() != null && !request.publicId().isBlank() ? request.publicId().trim() : null);
        User saved = userRepository.save(customer);

        if (oldPublicId != null && !oldPublicId.isBlank() && !oldPublicId.equals(customer.getImagePublicId())) {
            try {
                imageService.deleteImage(oldPublicId);
            } catch (Exception ignored) {
                // Ignore Cloudinary cleanup failure to not abort customer update
            }
        }

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public CustomerResponse removeProfilePicture(Long customerId) {
        User customer = getCustomer(customerId);
        String oldPublicId = customer.getImagePublicId();

        customer.setImage(null);
        customer.setImagePublicId(null);
        User saved = userRepository.save(customer);

        if (oldPublicId != null && !oldPublicId.isBlank()) {
            try {
                imageService.deleteImage(oldPublicId);
            } catch (Exception ignored) {
                // Ignore Cloudinary cleanup failure
            }
        }

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserImageDto> getCustomerImages(Long customerId) {
        getCustomer(customerId);
        return userImageRepository.findByUserIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(this::mapToUserImageDto)
                .toList();
    }

    @Override
    @Transactional
    public UserImageDto addCustomerImage(Long customerId, UserImageCreateRequest request) {
        User customer = getCustomer(customerId);
        UserImage userImage = UserImage.builder()
                .user(customer)
                .imageUrl(request.imageUrl().trim())
                .publicId(request.publicId() != null && !request.publicId().isBlank() ? request.publicId().trim() : null)
                .purpose(request.purpose() != null && !request.purpose().isBlank() ? request.purpose().trim() : "OTHER")
                .build();

        UserImage saved = userImageRepository.save(userImage);
        return mapToUserImageDto(saved);
    }

    @Override
    @Transactional
    public void deleteCustomerImage(Long customerId, Long imageId) {
        UserImage userImage = userImageRepository.findByIdAndUserId(imageId, customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer image not found"));

        String publicId = userImage.getPublicId();
        userImageRepository.delete(userImage);

        if (publicId != null && !publicId.isBlank()) {
            try {
                imageService.deleteImage(publicId);
            } catch (Exception ignored) {
                // Ignore Cloudinary cleanup failure
            }
        }
    }

    private UserImageDto mapToUserImageDto(UserImage img) {
        return new UserImageDto(
                img.getId(),
                img.getImageUrl(),
                img.getPublicId(),
                img.getPurpose(),
                img.getCreatedAt()
        );
    }
}