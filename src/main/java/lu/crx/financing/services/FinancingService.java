package lu.crx.financing.services;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lu.crx.financing.dtos.InvoiceTuple;
import lu.crx.financing.entities.Financing;
import lu.crx.financing.repositories.InvoiceRepository;
import lu.crx.financing.repositories.PurchaserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class FinancingService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PurchaserRepository purchaserRepository;

    @Transactional
    public void finance() {
        log.info("Financing started");

        /*
        Commented out approach yields same results, but requires data post-processing
        The second approach will fail tests in FinancingServiceMockTest
         */
        var invoicesToFinance = getInvoicesToFinanceForLowestRatePurchaser();
        //var invoicesToFinance = getInvoicesToFinanceForAllPurchasers();

        log.info("{} invoices to finance found", invoicesToFinance.size());
        invoicesToFinance.forEach(i ->
                log.info("invoiceId: {}. purchaserId : {}, daysToFinance: {}, financingRate: {}, earlyPaymentAmount: {}",
                        i.getInvoiceId(), i.getPurchaserId(), i.getDaysToFinance(), i.getFinancingRate(), i.getEarlyPaymentAmount()));

        var invoicesToSave = invoicesToFinance.stream()
                .map(invoiceTuple -> {
                    var purchaser = purchaserRepository.findById(invoiceTuple.getPurchaserId());
                    var invoice = invoiceRepository.findById(invoiceTuple.getInvoiceId());
                    var financingBuilder = Financing.builder()
                            .daysToFinance(invoiceTuple.getDaysToFinance())
                            .financingRateInBps(invoiceTuple.getFinancingRate())
                            .earlyPaymentAmountInCents(invoiceTuple.getEarlyPaymentAmount());
                    purchaser.ifPresent(financingBuilder::purchaser);
                    invoice.ifPresent(present -> present.setFinancing(financingBuilder.build()));

                    return invoice;
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        invoiceRepository.saveAll(invoicesToSave);

        log.info("Financing completed");
    }

    /*
    Alternative version of extracting purchasers with the lowest rate. To be tested for performance.
     */
    private List<InvoiceTuple> getInvoicesToFinanceForAllPurchasers() {
        return invoiceRepository.findNotFinancedForAllPurchasers().stream()
                .collect(Collectors.groupingBy(InvoiceTuple::getInvoiceId)).values().stream()
                .map(invoiceTuple -> {
                    var maxEarlyPaymentAmount = invoiceTuple.stream()
                            .mapToLong(InvoiceTuple::getEarlyPaymentAmount)
                            .max();
                    return invoiceTuple.stream()
                            .filter(t -> t.getEarlyPaymentAmount().equals(maxEarlyPaymentAmount.orElse(0)))
                            .findFirst();
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    private List<InvoiceTuple> getInvoicesToFinanceForLowestRatePurchaser() {
        return invoiceRepository.findNotFinancedForLowestRatePurchaser();
    }
}
