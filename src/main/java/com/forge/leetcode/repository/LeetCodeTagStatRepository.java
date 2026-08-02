package com.forge.leetcode.repository;

import com.forge.leetcode.entity.LeetCodeTagStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LeetCodeTagStatRepository extends JpaRepository<LeetCodeTagStat, UUID> {

    List<LeetCodeTagStat> findByUserId(UUID userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from LeetCodeTagStat t where t.user.id = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}
