package ru.skinemsya.settlement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    @Query("""
            select s from Settlement s
            join fetch s.fromMember
            join fetch s.toMember
            where s.group.id = :groupId
            order by s.createdAt desc, s.id desc
            """)
    List<Settlement> findAllByGroup(@Param("groupId") Long groupId);

    Optional<Settlement> findByIdAndGroupId(Long id, Long groupId);
}
