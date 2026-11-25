package com.ridematch.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ridematch.domain.entity.Community;
import com.ridematch.domain.enums.CommunityType;

/** Persistence operations for college and company communities. */
public interface CommunityRepository extends JpaRepository<Community, Long> {

    List<Community> findAllByOrderByName();

    boolean existsByNameIgnoreCaseAndType(String name, CommunityType type);

    List<Community> findByTypeOrderByName(CommunityType type);
}
