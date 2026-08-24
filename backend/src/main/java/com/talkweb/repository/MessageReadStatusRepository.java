package com.talkweb.repository;

import com.talkweb.entity.MessageReadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageReadStatusRepository extends JpaRepository<MessageReadStatus, Long> {
    Optional<MessageReadStatus> findByMessageIdAndUserId(Long messageId, Long userId);
    boolean existsByMessageIdAndUserId(Long messageId, Long userId);

    @Query("SELECT rs.message.id FROM MessageReadStatus rs WHERE rs.message.id IN :messageIds AND rs.user.id = :userId")
    List<Long> findReadMessageIds(@Param("messageIds") List<Long> messageIds, @Param("userId") Long userId);
    
    @Query("SELECT COUNT(rs) FROM MessageReadStatus rs WHERE rs.message.id = :messageId")
    long countReadCountByMessageId(@Param("messageId") Long messageId);
}
