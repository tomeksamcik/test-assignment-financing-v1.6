package lu.crx.financing.dtos;

/**
 * Interface defining values returned by InvoiceRepository queries
 */
public interface InvoiceTuple {

    Long getInvoiceId();

    Long getPurchaserId();

    Integer getDaysToFinance();

    Float getFinancingRate();

    Long getEarlyPaymentAmount();
}
