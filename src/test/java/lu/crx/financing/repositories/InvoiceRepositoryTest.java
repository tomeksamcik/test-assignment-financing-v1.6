package lu.crx.financing.repositories;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lu.crx.financing.dtos.InvoiceTuple;
import lu.crx.financing.services.SeedingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class InvoiceRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private SeedingService seedingService;

    @BeforeEach
    public void init() {
        seedingService.seedMasterData();
        seedingService.seedInvoices();
    }

    @AfterEach
    public void empty() {
        seedingService.emptyDatabase();
    }

    private void markAllInvoicesFinanced() {
        var sql = """
                insert into financing (id, days_to_finance, early_payment_amount_in_cents, financing_rate_in_bps, purchaser_id) values (1, 1, 1, 1, 1);
                update invoice set financing_id = 1;
                """;
        entityManager.createNativeQuery(sql).executeUpdate();
    }

    @Test
    void shouldExcludeInvoicesWhenMaximumFinancingRateIsExceededWhenFindNotFinancedForAllPurchasers() {
        var invoices = invoiceRepository.findNotFinancedForAllPurchasers().stream()
                .collect(Collectors.groupingBy(InvoiceTuple::getInvoiceId));

        assertThat(invoices).hasSize(10);
        assertThat(invoices.get(1L).stream().map(InvoiceTuple::getPurchaserId).toList()).doesNotContain(1L, 2L);
        assertThat(invoices.get(3L).stream().map(InvoiceTuple::getPurchaserId).toList()).doesNotContain(1L);
        assertThat(invoices.get(7L).stream().map(InvoiceTuple::getPurchaserId).toList()).doesNotContain(2L);
        assertThat(invoices.get(13L).stream().map(InvoiceTuple::getPurchaserId).toList()).doesNotContain(3L);
        assertThat(invoices.get(14L).stream().map(InvoiceTuple::getPurchaserId).toList()).doesNotContain(3L);
        assertThat(invoices).doesNotContainKeys(4L, 8L, 9L, 15L);
    }

    @Test
    void shouldExcludeInvoicesWhenMaximumFinancingRateIsExceededWhenFindNotFinancedForLowestRatePurchaser() {
        var invoices = invoiceRepository.findNotFinancedForLowestRatePurchaser();

        assertThat(invoices).hasSize(10);
        assertThat(invoices.stream().filter(it -> it.getInvoiceId().equals(1L)).map(InvoiceTuple::getPurchaserId).findFirst().orElseThrow()).isNotEqualTo(1L).isNotEqualTo(2L);
        assertThat(invoices.stream().filter(it -> it.getInvoiceId().equals(3L)).map(InvoiceTuple::getPurchaserId).findFirst().orElseThrow()).isNotEqualTo(1L);
        assertThat(invoices.stream().filter(it -> it.getInvoiceId().equals(7L)).map(InvoiceTuple::getPurchaserId).findFirst().orElseThrow()).isNotEqualTo(2L);
        assertThat(invoices.stream().filter(it -> it.getInvoiceId().equals(13L)).map(InvoiceTuple::getPurchaserId).findFirst().orElseThrow()).isNotEqualTo(3L);
        assertThat(invoices.stream().filter(it -> it.getInvoiceId().equals(14L)).map(InvoiceTuple::getPurchaserId).findFirst().orElseThrow()).isNotEqualTo(3L);
        assertThat(invoices.stream().map(InvoiceTuple::getPurchaserId)).doesNotContain(4L, 8L, 9L, 15L);
    }

    @Test
    void shouldExcludeInvoicesWhenMinimumFinancingTermIsNotFulfilledWhenFindNotFinancedForAllPurchasers() {
        var invoices = invoiceRepository.findNotFinancedForAllPurchasers().stream()
                .collect(Collectors.groupingBy(InvoiceTuple::getInvoiceId));

        assertThat(invoices).hasSize(10);
        assertThat(invoices.get(6L).stream().map(InvoiceTuple::getPurchaserId).toList()).doesNotContain(2L);
        assertThat(invoices.get(10L).stream().map(InvoiceTuple::getPurchaserId).toList()).doesNotContain(2L);
        assertThat(invoices.get(11L).stream().map(InvoiceTuple::getPurchaserId).toList()).doesNotContain(2L);
        assertThat(invoices).doesNotContainKeys(5L);
    }

    @Test
    void shouldExcludeInvoicesWhenMinimumFinancingTermIsNotFulfilledWhenFindNotFinancedForLowestRatePurchaser() {
        var invoices = invoiceRepository.findNotFinancedForLowestRatePurchaser();

        assertThat(invoices).hasSize(10);
        assertThat(invoices.stream().filter(it -> it.getInvoiceId().equals(6L)).map(InvoiceTuple::getPurchaserId).findFirst().orElseThrow()).isNotEqualTo(2L);
        assertThat(invoices.stream().filter(it -> it.getInvoiceId().equals(10L)).map(InvoiceTuple::getPurchaserId).findFirst().orElseThrow()).isNotEqualTo(2L);
        assertThat(invoices.stream().filter(it -> it.getInvoiceId().equals(11L)).map(InvoiceTuple::getPurchaserId).findFirst().orElseThrow()).isNotEqualTo(2L);
        assertThat(invoices.stream().map(InvoiceTuple::getPurchaserId).toList()).doesNotContain(5L);
    }

    @Test
    @Transactional
    void shouldExcludeFinancedInvoicesWhenFindNotFinancedForAllPurchasers() {
        markAllInvoicesFinanced();

        var invoices = invoiceRepository.findNotFinancedForAllPurchasers().stream()
                .collect(Collectors.groupingBy(InvoiceTuple::getInvoiceId));

        assertThat(invoices.keySet()).isEmpty();
    }

    @Test
    @Transactional
    void shouldExcludeFinancedInvoicesWhenFindNotFinancedForLowestRatePurchaser() {
        markAllInvoicesFinanced();

        var invoices = invoiceRepository.findNotFinancedForLowestRatePurchaser();

        assertThat(invoices).isEmpty();
    }
}
