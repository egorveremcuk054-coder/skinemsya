package ru.skinemsya.settlement;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.skinemsya.security.CurrentUser;

import java.util.List;

@RestController
@RequestMapping("/api/groups/{groupId}/settlements")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SettlementResponse create(@AuthenticationPrincipal Jwt jwt,
                                     @PathVariable Long groupId,
                                     @Valid @RequestBody CreateSettlementRequest request) {
        return settlementService.create(CurrentUser.id(jwt), groupId, request);
    }

    @GetMapping
    public List<SettlementResponse> list(@AuthenticationPrincipal Jwt jwt, @PathVariable Long groupId) {
        return settlementService.list(CurrentUser.id(jwt), groupId);
    }

    @DeleteMapping("/{settlementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long groupId, @PathVariable Long settlementId) {
        settlementService.delete(CurrentUser.id(jwt), groupId, settlementId);
    }
}
