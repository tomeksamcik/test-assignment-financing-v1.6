package lu.crx.financing.entities;

import jakarta.persistence.*;
import lombok.*;

/**
 * An entity representing invoice financing, created when {@link Purchaser} is financing the {@link Invoice} on behalf of the {@link Debtor}.
 */
@Entity
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Financing {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    /**
     * Purchaser that is financing the invoice.
     */
    @ManyToOne(optional = false)
    private Purchaser purchaser;

    /**
     * The value is the early payment amount paid to the {@link Creditor} by {@link Purchaser}.
     */
    @Basic(optional = false)
    private long earlyPaymentAmountInCents;

    /**
     * Applied financing rate.
     */
    @Basic(optional = false)
    private float financingRateInBps;

    /**
     * Number of days for which annual financing rate has been applied.
     */
    @Basic(optional = false)
    private int daysToFinance;
}
