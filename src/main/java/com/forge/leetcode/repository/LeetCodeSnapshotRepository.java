package com.forge.leetcode.repository;

import com.forge.leetcode.entity.LeetCodeSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeetCodeSnapshotRepository extends JpaRepository<LeetCodeSnapshot, UUID> {

    Optional<LeetCodeSnapshot> findByUserId(UUID userId);
}
