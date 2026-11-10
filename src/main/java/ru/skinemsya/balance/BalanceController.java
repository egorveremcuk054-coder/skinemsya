package ru.skinemsya.balance;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.skinemsya.security.CurrentUser;

import java.util.List;

@RestController
@RequestMapping("/api/groups/{groupId}")
public class BalanceController {

    private final BalanceService balanceService;

    public BalanceController(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    /** Баланс каждого участника. */
    @GetMapping("/balances")
    public List<BalanceResponse> balances(@AuthenticationPrincipal Jwt jwt, @PathVariable Long groupId) {
        return balanceService.balances(CurrentUser.id(jwt), groupId);
    }

    /** Минимальный набор переводов, после которого все в расчёте. */
    @GetMapping("/settle-up")
    public List<TransferResponse> settleUp(@AuthenticationPrincipal Jwt jwt, @PathVariable Long groupId) {
        return balanceService.settleUp(CurrentUser.id(jwt), groupId);
    }
}
