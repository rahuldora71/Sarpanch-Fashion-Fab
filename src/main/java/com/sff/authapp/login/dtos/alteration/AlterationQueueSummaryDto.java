package com.sff.authapp.login.dtos.alteration;

public record AlterationQueueSummaryDto(
        long totalActive,
        long overdue,
        long dueToday,
        long dueTomorrow,
        long upcoming,
        long delivered
) {
}
