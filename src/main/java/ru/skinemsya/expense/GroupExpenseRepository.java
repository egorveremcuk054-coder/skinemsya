package ru.skinemsya.expense;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupExpenseRepository extends JpaRepository<GroupExpense, Long> {

    /** Все траты группы вместе с плательщиком и долями — одним запросом, без N+1. */
    @Query("""
            select distinct e from GroupExpense e
            join fetch e.payer
            left join fetch e.shares
            where e.group.id = :groupId
            order by e.spentOn desc, e.id desc
            """)
    List<GroupExpense> findAllWithShares(@Param("groupId") Long groupId);

    Optional<GroupExpense> findByIdAndGroupId(Long id, Long groupId);
}
