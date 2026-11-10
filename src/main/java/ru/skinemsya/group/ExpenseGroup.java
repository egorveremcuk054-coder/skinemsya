package ru.skinemsya.group;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import ru.skinemsya.user.User;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Группа общих расходов: поездка, квартира, праздник.
 */
@Entity
@Table(name = "expense_group")
public class ExpenseGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private User owner;

    private Instant createdAt;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<GroupMember> members = new ArrayList<>();

    protected ExpenseGroup() {
    }

    public ExpenseGroup(String name, User owner) {
        this.name = name;
        this.owner = owner;
        this.createdAt = Instant.now();
    }

    public GroupMember addMember(String memberName, User user) {
        GroupMember member = new GroupMember(this, memberName, user);
        members.add(member);
        return member;
    }

    public boolean isOwnedBy(Long userId) {
        return owner.getId().equals(userId);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getOwnerId() {
        return owner.getId();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<GroupMember> getMembers() {
        return Collections.unmodifiableList(members);
    }
}
