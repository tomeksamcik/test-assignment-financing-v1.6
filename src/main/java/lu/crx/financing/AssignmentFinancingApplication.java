package lu.crx.financing;

import lu.crx.financing.services.FinancingService;
import lu.crx.financing.services.SeedingService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import static lu.crx.financing.services.FinancingService.QueryMode.QUERY_FOR_LOWEST_RATE;

@SpringBootApplication
public class AssignmentFinancingApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssignmentFinancingApplication.class, args);
    }

    @Bean
    @Profile("!test")
    public CommandLineRunner run(
            SeedingService seedingService,
            FinancingService financingService) {

        return args -> {
            // seeding master data - creditors, debtors and purchasers
            seedingService.seedMasterData();

            // seeding the invoices
            seedingService.seedInvoices();

            // running the financing
            financingService.finance(QUERY_FOR_LOWEST_RATE);
        };
    }

}
