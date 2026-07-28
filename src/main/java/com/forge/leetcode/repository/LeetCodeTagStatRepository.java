package com.forge.leetcode.repository;

import com.forge.leetcode.entity.LeetCodeTagStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LeetCodeTagStatRepository extends JpaRepository<LeetCodeTagStat, UUID> {

    List<LeetCodeTagStat> findByUserId(UUID userId);

    void deleteByUserId(UUID userId);
}
