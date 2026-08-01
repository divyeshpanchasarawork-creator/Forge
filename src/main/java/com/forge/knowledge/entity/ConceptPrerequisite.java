package com.forge.knowledge.entity;

import com.forge.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "concept_prerequisites")
public class ConceptPrerequisite extends BaseEntity {

    @Column(name = "concept_slug", nullable = false, length = 100)
    private String conceptSlug;

    @Column(name = "prerequisite_slug", nullable = false, length = 100)
    private String prerequisiteSlug;
}
