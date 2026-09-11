package com.aether.ms_calculator.shared.persistence.postgres.repositories;

import com.aether.ms_calculator.shared.persistence.postgres.entities.PermissionGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionGroupRepository extends JpaRepository<PermissionGroupEntity, Integer> {
}
