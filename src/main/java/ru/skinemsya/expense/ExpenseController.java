package ru.skinemsya.expense;

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
@RequestMapping("/api/groups/{groupId}/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse create(@AuthenticationPrincipal Jwt jwt,
                                  @PathVariable Long groupId,
                                  @Valid @RequestBody CreateExpenseRequest request) {
        return expenseService.create(CurrentUser.id(jwt), groupId, request);
    }

    @GetMapping
    public List<ExpenseResponse> list(@AuthenticationPrincipal Jwt jwt, @PathVariable Long groupId) {
        return expenseService.list(CurrentUser.id(jwt), groupId);
    }

    @DeleteMapping("/{expenseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long groupId, @PathVariable Long expenseId) {
        expenseService.delete(CurrentUser.id(jwt), groupId, expenseId);
    }
}
