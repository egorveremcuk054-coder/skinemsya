package ru.skinemsya.group;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExpenseGroupRepository extends JpaRepository<ExpenseGroup, Long> {

    @Query("""
            select g from ExpenseGroup g
            where exists (select 1 from GroupMember m where m.group = g and m.user.id = :userId)
            order by g.id desc
            """)
    List<ExpenseGroup> findAllByMemberUserId(@Param("userId") Long userId);

    /**
     * Удаляет группу одним запросом: траты, участники и переводы
     * удалятся каскадно на стороне БД ({@code on delete cascade}).
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ExpenseGroup g where g.id = :id")
    void deleteWithContents(@Param("id") Long id);
}
