package ru.skinemsya.group;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import ru.skinemsya.user.User;

/**
 * Участник группы. Может быть зарегистрированным пользователем
 * или просто именем — для друзей, которых нет в приложении.
 */
@Entity
@Table(name = "group_member")
public class GroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id")
    private ExpenseGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String name;

    protected GroupMember() {
    }

    GroupMember(ExpenseGroup group, String name, User user) {
        this.group = group;
        this.name = name;
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getUserId() {
        return user == null ? null : user.getId();
    }
}
