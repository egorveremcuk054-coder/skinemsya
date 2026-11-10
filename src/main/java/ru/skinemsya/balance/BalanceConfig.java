package ru.skinemsya.balance;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.skinemsya.core.AdaptiveDebtSimplifier;
import ru.skinemsya.core.DebtSimplifier;

@Configuration
public class BalanceConfig {

    /** Алгоритм взаимозачёта: точный для небольших групп, жадный — для больших. */
    @Bean
    DebtSimplifier debtSimplifier() {
        return new AdaptiveDebtSimplifier();
    }
}
