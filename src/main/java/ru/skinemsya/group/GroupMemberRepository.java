package ru.skinemsya.group;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    List<GroupMember> findByGroupIdOrderById(Long groupId);

    Optional<GroupMember> findByIdAndGroupId(Long id, Long groupId);

    boolean existsByGroup_IdAndUser_Id(Long groupId, Long userId);

    boolean existsByGroupIdAndNameIgnoreCase(Long groupId, String name);
}
