package ru.skinemsya.group;

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
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateGroupRequest request) {
        return groupService.create(CurrentUser.id(jwt), request);
    }

    @GetMapping
    public List<GroupSummaryResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return groupService.list(CurrentUser.id(jwt));
    }

    @GetMapping("/{groupId}")
    public GroupResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long groupId) {
        return groupService.get(CurrentUser.id(jwt), groupId);
    }

    @DeleteMapping("/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long groupId) {
        groupService.delete(CurrentUser.id(jwt), groupId);
    }

    @PostMapping("/{groupId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse addMember(@AuthenticationPrincipal Jwt jwt,
                                    @PathVariable Long groupId,
                                    @Valid @RequestBody AddMemberRequest request) {
        return groupService.addMember(CurrentUser.id(jwt), groupId, request);
    }
}
