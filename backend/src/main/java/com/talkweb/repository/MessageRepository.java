package com.talkweb.repository;

import com.talkweb.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m WHERE " +
           "((m.sender.id = :userA AND m.receiver.id = :userB) OR " +
           " (m.sender.id = :userB AND m.receiver.id = :userA)) " +
           "AND (:beforeId IS NULL OR m.id < :beforeId) " +
           "ORDER BY m.id DESC")
    List<Message> findDirectMessages(@Param("userA") Long userA,
                                     @Param("userB") Long userB,
                                     @Param("beforeId") Long beforeId,
                                     Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.group.id = :groupId " +
           "AND (:beforeId IS NULL OR m.id < :beforeId) " +
           "ORDER BY m.id DESC")
    List<Message> findGroupMessages(@Param("groupId") Long groupId,
                                    @Param("beforeId") Long beforeId,
                                    Pageable pageable);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.sender.id = :senderId AND m.receiver.id = :receiverId " +
           "AND NOT EXISTS (SELECT 1 FROM MessageReadStatus rs WHERE rs.message.id = m.id AND rs.user.id = :receiverId)")
    long countUnreadDirectMessages(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.group.id = :groupId AND m.sender.id <> :userId " +
           "AND NOT EXISTS (SELECT 1 FROM MessageReadStatus rs WHERE rs.message.id = m.id AND rs.user.id = :userId)")
    long countUnreadGroupMessages(@Param("groupId") Long groupId, @Param("userId") Long userId);
}
