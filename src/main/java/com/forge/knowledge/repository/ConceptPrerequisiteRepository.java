package com.forge.knowledge.repository;

import com.forge.knowledge.entity.ConceptPrerequisite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConceptPrerequisiteRepository extends JpaRepository<ConceptPrerequisite, UUID> {

    List<ConceptPrerequisite> findByConceptSlug(String conceptSlug);

    List<ConceptPrerequisite> findByPrerequisiteSlug(String prerequisiteSlug);

    boolean existsByConceptSlugAndPrerequisiteSlug(String conceptSlug, String prerequisiteSlug);

    long count();
}
