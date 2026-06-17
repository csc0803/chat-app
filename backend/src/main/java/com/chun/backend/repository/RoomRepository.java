package com.chun.backend.repository;

import com.chun.backend.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByIdAndIsDeletedFalse(Long id);

}
