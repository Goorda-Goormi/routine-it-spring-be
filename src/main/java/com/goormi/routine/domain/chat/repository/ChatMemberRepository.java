package com.goormi.routine.domain.chat.repository;

import com.goormi.routine.domain.chat.entity.ChatMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMemberRepository extends JpaRepository<ChatMember, Long> {
    
    Optional<ChatMember> findByRoomIdAndUserId(Long roomId, Long userId);
    
    Optional<ChatMember> findByRoomIdAndUserIdAndIsActiveTrue(Long roomId, Long userId);
    
    List<ChatMember> findByRoomIdAndIsActiveTrue(Long roomId);
    
    List<ChatMember> findByUserIdAndIsActiveTrue(Long userId);
    
    @Query("SELECT COUNT(cm) FROM ChatMember cm WHERE cm.roomId = :roomId AND cm.isActive = true")
    int countActiveMembers(@Param("roomId") Long roomId);
    
    boolean existsByRoomIdAndUserIdAndIsActiveTrue(Long roomId, Long userId);
    
    @Modifying
    @Query("UPDATE ChatMember cm SET cm.lastReadMessageId = :messageId WHERE cm.roomId = :roomId AND cm.userId = :userId")
    void updateLastReadMessage(@Param("roomId") Long roomId, @Param("userId") Long userId, @Param("messageId") Long messageId);

    @Query("SELECT cm.roomId as roomId, COUNT(cm) as count FROM ChatMember cm " +
           "WHERE cm.roomId IN :roomIds AND cm.isActive = true " +
           "GROUP BY cm.roomId")
    List<Object[]> countActiveMembersByRoomIds(@Param("roomIds") List<Long> roomIds);
}