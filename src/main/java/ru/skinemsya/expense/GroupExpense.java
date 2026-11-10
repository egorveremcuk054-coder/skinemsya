package ru.skinemsya.expense;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import ru.skinemsya.group.ExpenseGroup;
import ru.skinemsya.group.GroupMember;
import ru.skinemsya.user.User;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Трата в группе. Суммы хранятся в копейках.
 */
@Entity
@Table(name = "expense")
public class GroupExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id")
    private ExpenseGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payer_id")
    private GroupMember payer;

    private String description;

    private long amount;

    @Enumerated(EnumType.STRING)
    private SplitType splitType;

    private LocalDate spentOn;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by")
    private User createdBy;

    private Instant createdAt;

    /** Доля каждого участника: id участника → сумма в копейках. */
    @ElementCollection
    @CollectionTable(name = "expense_share", joinColumns = @JoinColumn(name = "expense_id"))
    @MapKeyColumn(name = "member_id")
    @Column(name = "amount")
    private Map<Long, Long> shares = new HashMap<>();

    protected GroupExpense() {
    }

    public GroupExpense(ExpenseGroup group, GroupMember payer, String description, long amount,
                        SplitType splitType, LocalDate spentOn, User createdBy, Map<Long, Long> shares) {
        this.group = group;
        this.payer = payer;
        this.description = description;
        this.amount = amount;
        this.splitType = splitType;
        this.spentOn = spentOn;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
        this.shares.putAll(shares);
    }

    public Long getId() {
        return id;
    }

    public GroupMember getPayer() {
        return payer;
    }

    public String getDescription() {
        return description;
    }

    public long getAmount() {
        return amount;
    }

    public SplitType getSplitType() {
        return splitType;
    }

    public LocalDate getSpentOn() {
        return spentOn;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Map<Long, Long> getShares() {
        return Collections.unmodifiableMap(shares);
    }
}
