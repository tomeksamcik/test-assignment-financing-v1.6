package lu.crx.financing.services;

import jakarta.transaction.Transactional;
import lu.crx.financing.entities.Invoice;
import lu.crx.financing.repositories.InvoiceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class FinancingServiceTest {

    @Autowired
    private SeedingService seedingService;

    @Autowired
    private FinancingService financingService;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @BeforeEach
    public void init() {
        seedingService.seedMasterData();
        seedingService.seedInvoices();
    }

    @AfterEach
    public void empty() {
        seedingService.emptyDatabase();
    }

    @Test
    @Transactional
    void shouldFinanceInvoiceWithTheLowestFinancingRate() {
        financingService.finance();

        var invoices = invoiceRepository.findAll();

        assertThat(invoices.stream().filter(i -> i.getFinancing() != null).map(Invoice::getId).toList())
                .doesNotContain(4L, 5L, 8L, 9L, 15L);
        assertThat(invoices.stream().filter(i -> i.getId() == 1).findFirst().get().getFinancing().getPurchaser().getId()).isEqualTo(3);
        assertThat(invoices.stream().filter(i -> i.getId() == 2).findFirst().get().getFinancing().getPurchaser().getId()).isEqualTo(3);
        assertThat(invoices.stream().filter(i -> i.getId() == 3).findFirst().get().getFinancing().getPurchaser().getId()).isEqualTo(3);
        assertThat(invoices.stream().filter(i -> i.getId() == 6).findFirst().get().getFinancing().getPurchaser().getId()).isEqualTo(3);
        assertThat(invoices.stream().filter(i -> i.getId() == 7).findFirst().get().getFinancing().getPurchaser().getId()).isEqualTo(3);
        assertThat(invoices.stream().filter(i -> i.getId() == 10).findFirst().get().getFinancing().getPurchaser().getId()).isEqualTo(3);
        assertThat(invoices.stream().filter(i -> i.getId() == 11).findFirst().get().getFinancing().getPurchaser().getId()).isEqualTo(1);
        assertThat(invoices.stream().filter(i -> i.getId() == 12).findFirst().get().getFinancing().getPurchaser().getId()).isEqualTo(2);
        assertThat(invoices.stream().filter(i -> i.getId() == 13).findFirst().get().getFinancing().getPurchaser().getId()).isEqualTo(2);
        assertThat(invoices.stream().filter(i -> i.getId() == 14).findFirst().get().getFinancing().getPurchaser().getId()).isEqualTo(2);
    }
}
