package ru.skinemsya.settlement;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import ru.skinemsya.group.ExpenseGroup;
import ru.skinemsya.group.GroupMember;
import ru.skinemsya.user.User;

import java.time.Instant;

/**
 * Перевод, которым участник вернул долг другому участнику. Сумма в копейках.
 */
@Entity
@Table(name = "settlement")
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id")
    private ExpenseGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_member_id")
    private GroupMember fromMember;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_member_id")
    private GroupMember toMember;

    private long amount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by")
    private User createdBy;

    private Instant createdAt;

    protected Settlement() {
    }

    public Settlement(ExpenseGroup group, GroupMember fromMember, GroupMember toMember, long amount, User createdBy) {
        this.group = group;
        this.fromMember = fromMember;
        this.toMember = toMember;
        this.amount = amount;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public GroupMember getFromMember() {
        return fromMember;
    }

    public GroupMember getToMember() {
        return toMember;
    }

    public long getAmount() {
        return amount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
