package lu.crx.financing.services;

import lu.crx.financing.dtos.InvoiceTuple;
import lu.crx.financing.entities.Invoice;
import lu.crx.financing.entities.Purchaser;
import lu.crx.financing.repositories.InvoiceRepository;
import lu.crx.financing.repositories.PurchaserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FinancingServiceMockTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private PurchaserRepository purchaserRepository;

    private FinancingService financingService;

    @BeforeEach
    public void init() {
        financingService = new FinancingService(invoiceRepository, purchaserRepository);
    }

    @Test
    void shouldNotSaveWhenInvoicesToFinanceNotFound() {
        when(invoiceRepository.findNotFinancedForLowestRatePurchaser()).thenReturn(List.of());

        financingService.finance();

        verify(invoiceRepository, never()).findById(anyLong());
        verify(purchaserRepository, never()).findById(anyLong());
        verify(invoiceRepository, times(1)).saveAll(eq(List.of()));
    }

    @Test
    void shouldSaveWhenInvoiceForTheGivenIdIsFound() {
        var invoiceId = 1L;
        var purchaserId = 2L;
        var daysToFinance = 10;
        var financingRate = 2f;
        var earlyPaymentAmount = 19998L;
        var tuple = mock(InvoiceTuple.class);
        var invoice = spy(new Invoice());
        var purchaser = spy(new Purchaser());

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(purchaserRepository.findById(purchaserId)).thenReturn(Optional.of(purchaser));
        when(invoiceRepository.findNotFinancedForLowestRatePurchaser()).thenReturn(List.of(tuple));
        when(tuple.getInvoiceId()).thenReturn(invoiceId);
        when(tuple.getPurchaserId()).thenReturn(purchaserId);
        when(tuple.getDaysToFinance()).thenReturn(daysToFinance);
        when(tuple.getFinancingRate()).thenReturn(financingRate);
        when(tuple.getEarlyPaymentAmount()).thenReturn(earlyPaymentAmount);
        when(invoice.getId()).thenReturn(invoiceId);
        when(purchaser.getId()).thenReturn(purchaserId);

        financingService.finance();

        var captor = ArgumentCaptor.forClass(List.class);

        verify(invoiceRepository, times(1)).findById(eq(1L));
        verify(purchaserRepository, times(1)).findById(eq(2L));
        verify(invoiceRepository, times(1)).saveAll(captor.capture());

        var capturedInvoice = (Invoice)captor.getValue().get(0);

        assertThat(captor.getValue()).hasSize(1);
        assertThat(capturedInvoice.getId()).isEqualTo(invoiceId);
        assertThat(capturedInvoice.getFinancing().getPurchaser().getId()).isEqualTo(purchaserId);
        assertThat(capturedInvoice.getFinancing().getDaysToFinance()).isEqualTo(daysToFinance);
        assertThat(capturedInvoice.getFinancing().getFinancingRateInBps()).isEqualTo(financingRate);
        assertThat(capturedInvoice.getFinancing().getEarlyPaymentAmountInCents()).isEqualTo(earlyPaymentAmount);
    }

    @Test
    void shouldSaveWhenPurchaserForTheGivenIdIsNotFoundButInvoiceForTheGivenIdIsFound() {
        var invoiceId = 1L;
        var purchaserId = 2L;
        var daysToFinance = 10;
        var financingRate = 2f;
        var earlyPaymentAmount = 19998L;
        var tuple = mock(InvoiceTuple.class);
        var invoice = spy(new Invoice());

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(purchaserRepository.findById(purchaserId)).thenReturn(Optional.empty());
        when(invoiceRepository.findNotFinancedForLowestRatePurchaser()).thenReturn(List.of(tuple));
        when(tuple.getInvoiceId()).thenReturn(invoiceId);
        when(tuple.getPurchaserId()).thenReturn(purchaserId);
        when(tuple.getDaysToFinance()).thenReturn(daysToFinance);
        when(tuple.getFinancingRate()).thenReturn(financingRate);
        when(tuple.getEarlyPaymentAmount()).thenReturn(earlyPaymentAmount);
        when(invoice.getId()).thenReturn(invoiceId);

        financingService.finance();

        var captor = ArgumentCaptor.forClass(List.class);

        verify(invoiceRepository, times(1)).findById(eq(1L));
        verify(purchaserRepository, times(1)).findById(eq(2L));
        verify(invoiceRepository, times(1)).saveAll(captor.capture());

        var capturedInvoice = (Invoice)captor.getValue().get(0);

        assertThat(captor.getValue()).hasSize(1);
        assertThat(capturedInvoice.getId()).isEqualTo(invoiceId);
        assertThat(capturedInvoice.getFinancing().getPurchaser()).isNull();
        assertThat(capturedInvoice.getFinancing().getDaysToFinance()).isEqualTo(daysToFinance);
        assertThat(capturedInvoice.getFinancing().getFinancingRateInBps()).isEqualTo(financingRate);
        assertThat(capturedInvoice.getFinancing().getEarlyPaymentAmountInCents()).isEqualTo(earlyPaymentAmount);
    }

    @Test
    void shouldNotSaveWhenInvoiceForTheGivenIdIsNotFound() {
        var invoiceId = 1L;
        var purchaserId = 2L;
        var daysToFinance = 10;
        var financingRate = 2f;
        var earlyPaymentAmount = 19998L;
        var tuple = mock(InvoiceTuple.class);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.empty());
        when(purchaserRepository.findById(purchaserId)).thenReturn(Optional.empty());
        when(invoiceRepository.findNotFinancedForLowestRatePurchaser()).thenReturn(List.of(tuple));
        when(tuple.getInvoiceId()).thenReturn(invoiceId);
        when(tuple.getPurchaserId()).thenReturn(purchaserId);
        when(tuple.getDaysToFinance()).thenReturn(daysToFinance);
        when(tuple.getFinancingRate()).thenReturn(financingRate);
        when(tuple.getEarlyPaymentAmount()).thenReturn(earlyPaymentAmount);

        financingService.finance();

        verify(invoiceRepository, times(1)).findById(eq(1L));
        verify(purchaserRepository, times(1)).findById(eq(2L));
        verify(invoiceRepository, times(1)).saveAll(List.of());
    }
}
