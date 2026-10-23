package com.sff.authapp.login.services;

import com.sff.authapp.login.dtos.order.HistoricalCustomerSummaryDto;
import com.sff.authapp.login.dtos.order.HistoricalOrderRequestDto;
import com.sff.authapp.login.dtos.order.HistoricalOrderResponseDto;
import com.sff.authapp.login.dtos.order.NextOrderNumberResponseDto;

import java.util.List;

public interface HistoricalOrderService {

    HistoricalOrderResponseDto createHistoricalOrder(
            HistoricalOrderRequestDto request,
            Long authenticatedUserId
    );

    NextOrderNumberResponseDto getNextOrderNumber();

    List<HistoricalCustomerSummaryDto> findCustomersByPhone(String phone);

    List<HistoricalOrderResponseDto> getRecentHistoricalOrders(int limit);
}
