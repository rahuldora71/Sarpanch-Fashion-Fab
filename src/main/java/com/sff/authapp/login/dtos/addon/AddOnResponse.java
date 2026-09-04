package com.sff.authapp.login.dtos.addon;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AddOnResponse(

        Long id,

        String name,

        BigDecimal price,

        BigDecimal tailorCommission,

        String imageUrl,

        String imagePublicId,

        boolean beforeCutting,

        boolean active,

        LocalDateTime createdAt,

        LocalDateTime updatedAt
) {
}