package com.sff.authapp.login.services.billing;

import com.sff.authapp.login.entities.BillingConfig;
import com.sff.authapp.login.entities.InvoiceSequence;
import com.sff.authapp.login.repositories.InvoiceSequenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvoiceSequenceService {

    private final InvoiceSequenceRepository sequenceRepository;
    private final BillingConfigService billingConfigService;

    @Transactional(propagation = Propagation.MANDATORY)
    public String generateNextInvoiceNumber() {
        BillingConfig config = billingConfigService.getConfig();
        String prefix = config.getInvoicePrefix() != null ? config.getInvoicePrefix() : "INV";
        String fy = config.getFinancialYear() != null ? config.getFinancialYear() : "2026-27";

        InvoiceSequence sequence = sequenceRepository.findByPrefixAndFinancialYearWithLock(prefix, fy)
                .orElseGet(() -> InvoiceSequence.builder()
                        .prefix(prefix)
                        .financialYear(fy)
                        .currentSequence(0L)
                        .padding(4)
                        .build()
                );

        long nextVal = sequence.getCurrentSequence() + 1;
        sequence.setCurrentSequence(nextVal);
        sequenceRepository.save(sequence);

        return String.format("%s/%s/%0" + sequence.getPadding() + "d", prefix, fy, nextVal);
    }
}
