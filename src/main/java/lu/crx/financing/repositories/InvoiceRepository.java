package lu.crx.financing.repositories;

import lu.crx.financing.dtos.InvoiceTuple;
import lu.crx.financing.entities.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    /*
    Query calculating early payment amounts for all invoices that hasn't been financed
    and fulfill their creditors maximum financing rate (Creditor.maxFinancingRateInBps) and their purchasers minimum financing term in days (Purchaser.minimumFinancingTermInDays).

    Note: Early payment amount is calculated in cents rounded to integral value.
    Note: This query returns early payment amounts for all relevant purchasers for the given invoice
     */
    @Query(value = """
        with invoices as
            (select i.id as iid,
                    p.id as pid,
                    i.value_in_cents,
                    c.max_financing_rate_in_bps,
                    p.minimum_financing_term_in_days,
                    datediff(day, current_date, i.maturity_date) as days_to_finance,
                    cast(pfs.annual_rate_in_bps * datediff(day, current_date, i.maturity_date) as float) / cast(360 as float) as financing_rate
             from invoice i
             join creditor c on i.creditor_id = c.id
             join purchaser_financing_settings pfs on i.creditor_id = pfs.creditor_id
             join purchaser_purchaser_financing_settings ppfs on pfs.id = ppfs.purchaser_financing_settings_id
             join purchaser p on ppfs.purchaser_id = p.id
             where i.financing_id is null
             order by i.id,
                      p.id),
        eligible_invoices as
            (select iid,
                    pid,
                    days_to_finance,
                    financing_rate,
                    value_in_cents - round(value_in_cents * financing_rate * 0.0001) as early_payment_amount
             from invoices
             where days_to_finance >= minimum_financing_term_in_days
                 and financing_rate <= max_financing_rate_in_bps)
        select iid as invoice_id,
               pid as purchaser_id,
               days_to_finance,
               financing_rate,
               early_payment_amount
        from eligible_invoices""",
            nativeQuery = true)
    List<InvoiceTuple> findNotFinancedForAllPurchasers();

    /*
    Query calculating early payment amounts for all invoices that hasn't been financed
    and fulfill their creditors maximum financing rate (Creditor.maxFinancingRateInBps) and their purchasers minimum financing term in days (Purchaser.minimumFinancingTermInDays).

    Note: Early payment amount is calculated in cents rounded to integral value.
    Note: This query returns early payment amount only for the purchaser that offers the smallest rate for the given invoice.
        It may have some negative performance impact.
     */
    @Query(value = """
        with invoices as
            (select i.id as iid,
                    p.id as pid,
                    i.value_in_cents,
                    c.max_financing_rate_in_bps,
                    p.minimum_financing_term_in_days,
                    datediff(day, current_date, i.maturity_date) as days_to_finance,
                    cast(pfs.annual_rate_in_bps * datediff(day, current_date, i.maturity_date) as float) / cast(360 as float) as financing_rate
             from invoice i
             join creditor c on i.creditor_id = c.id
             join purchaser_financing_settings pfs on i.creditor_id = pfs.creditor_id
             join purchaser_purchaser_financing_settings ppfs on pfs.id = ppfs.purchaser_financing_settings_id
             join purchaser p on ppfs.purchaser_id = p.id
             where i.financing_id is null
             order by i.id,
                      p.id),
        eligible_invoices as
            (select iid,
                    pid,
                    days_to_finance,
                    financing_rate,
                    value_in_cents - round(value_in_cents * financing_rate * 0.0001) as early_payment_amount
             from invoices
             where days_to_finance >= minimum_financing_term_in_days
                 and financing_rate <= max_financing_rate_in_bps),
        eligible_grouped_invoices as
            (select iid,
                    max(early_payment_amount) as max_early_payment_amount
             from eligible_invoices
             group by iid)
        select grouped.iid as invoice_id,
               pid as purchaser_id,
               days_to_finance,
               financing_rate,
               early_payment_amount
        from eligible_invoices as not_grouped,
             eligible_grouped_invoices as grouped
        where grouped.iid = not_grouped.iid
            and grouped.max_early_payment_amount = not_grouped.early_payment_amount
        """,
            nativeQuery = true)
    List<InvoiceTuple> findNotFinancedForLowestRatePurchaser();
}
