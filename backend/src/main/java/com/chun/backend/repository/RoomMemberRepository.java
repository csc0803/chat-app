package com.chun.backend.repository;

import com.chun.backend.model.RoomMember;
import com.chun.backend.model.RoomMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomMemberRepository extends JpaRepository<RoomMember, RoomMemberId> {

    Optional<RoomMember> findByIdAndIsDeletedFalse(RoomMemberId id);
    long countByRoom_IdAndIsAdminTrueAndIsDeletedFalse(Long roomId);
    long countByRoom_IdAndIsDeletedFalse(Long roomId);
    Optional<RoomMember> findFirstByRoom_IdAndIsAdminFalseAndIsDeletedFalseOrderByJoinedAtAsc(Long roomId);

}
