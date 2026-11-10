package ru.skinemsya.group;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.skinemsya.common.ApiException;
import ru.skinemsya.user.AuthService;
import ru.skinemsya.user.User;
import ru.skinemsya.user.UserRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class GroupService {

    private final ExpenseGroupRepository groups;
    private final GroupMemberRepository members;
    private final UserRepository users;

    public GroupService(ExpenseGroupRepository groups, GroupMemberRepository members, UserRepository users) {
        this.groups = groups;
        this.members = members;
        this.users = users;
    }

    public GroupResponse create(Long userId, CreateGroupRequest request) {
        User user = users.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Пользователь не найден"));
        ExpenseGroup group = new ExpenseGroup(request.name().trim(), user);
        group.addMember(user.getDisplayName(), user);
        groups.save(group);
        return GroupResponse.from(group);
    }

    @Transactional(readOnly = true)
    public List<GroupSummaryResponse> list(Long userId) {
        return groups.findAllByMemberUserId(userId).stream()
                .map(GroupSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public GroupResponse get(Long userId, Long groupId) {
        return GroupResponse.from(requireAccess(userId, groupId));
    }

    public void delete(Long userId, Long groupId) {
        ExpenseGroup group = requireAccess(userId, groupId);
        if (!group.isOwnedBy(userId)) {
            throw ApiException.forbidden("Удалить группу может только её создатель");
        }
        groups.deleteWithContents(groupId);
    }

    public MemberResponse addMember(Long userId, Long groupId, AddMemberRequest request) {
        ExpenseGroup group = requireAccess(userId, groupId);

        String name = request.name() == null ? "" : request.name().trim();
        User user = null;
        if (request.email() != null && !request.email().isBlank()) {
            user = users.findByEmail(AuthService.normalizeEmail(request.email()))
                    .orElseThrow(() -> ApiException.notFound(
                            "Пользователь с email %s не зарегистрирован".formatted(request.email())));
            if (members.existsByGroup_IdAndUser_Id(groupId, user.getId())) {
                throw ApiException.conflict("Этот пользователь уже состоит в группе");
            }
            if (name.isEmpty()) {
                name = user.getDisplayName();
            }
        }
        if (name.isEmpty()) {
            throw ApiException.badRequest("Укажите имя участника или email зарегистрированного пользователя");
        }
        if (members.existsByGroupIdAndNameIgnoreCase(groupId, name)) {
            throw ApiException.conflict("Участник с именем «%s» уже есть в группе".formatted(name));
        }

        GroupMember member = members.save(group.addMember(name, user));
        return MemberResponse.from(member);
    }

    /**
     * Возвращает группу, если пользователь в ней состоит.
     * Для чужих групп отвечаем 404, а не 403, чтобы не раскрывать, что такая группа существует.
     */
    @Transactional(readOnly = true)
    public ExpenseGroup requireAccess(Long userId, Long groupId) {
        if (!members.existsByGroup_IdAndUser_Id(groupId, userId)) {
            throw ApiException.notFound("Группа не найдена");
        }
        return groups.findById(groupId)
                .orElseThrow(() -> ApiException.notFound("Группа не найдена"));
    }

    /** Участники группы по id в порядке добавления. */
    @Transactional(readOnly = true)
    public Map<Long, GroupMember> membersById(Long groupId) {
        Map<Long, GroupMember> result = new LinkedHashMap<>();
        members.findByGroupIdOrderById(groupId).forEach(member -> result.put(member.getId(), member));
        return result;
    }
}
