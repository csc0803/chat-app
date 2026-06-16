package com.chun.backend.repository;

import com.chun.backend.model.RoomMember;
import com.chun.backend.model.RoomMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomMemberRepository extends JpaRepository<RoomMember, RoomMemberId> {

}
