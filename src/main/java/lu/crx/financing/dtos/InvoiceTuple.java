package lu.crx.financing.dtos;

public interface InvoiceTuple {

    Long getInvoiceId();

    Long getPurchaserId();

    Integer getDaysToFinance();

    Float getFinancingRate();

    Long getEarlyPaymentAmount();
}
