package com.commit.commit.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.commit.commit.entity.MembershipStatus;
import com.commit.commit.entity.UserSpace;

public interface UserSpaceRepository extends JpaRepository<UserSpace, Long> {
    boolean existsByUserIdAndSpaceIdAndStatus(Long userId, Long spaceId, MembershipStatus status);
    Optional<UserSpace> findByUserIdAndSpaceId(Long userId, Long spaceId);
    List<UserSpace> findByUserIdAndStatus(Long userId, MembershipStatus status);
    Optional<UserSpace> findByIdAndUserId(Long id, Long userId);
    
    List<UserSpace> findBySpaceIdAndStatus(Long spaceId, MembershipStatus status);
    int countBySpaceIdAndStatus(Long spaceId, MembershipStatus status);
    
    @Query("SELECT us FROM UserSpace us WHERE us.space.id = :spaceId AND us.status = :status " +
           "AND (LOWER(us.user.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(us.user.email) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<UserSpace> searchMembersByQuery(@Param("spaceId") Long spaceId, 
                                         @Param("status") MembershipStatus status,
                                         @Param("query") String query);
}
