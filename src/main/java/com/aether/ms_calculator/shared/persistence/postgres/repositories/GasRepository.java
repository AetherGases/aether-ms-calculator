package com.aether.ms_calculator.shared.persistence.postgres.repositories;

import com.aether.ms_calculator.shared.persistence.postgres.entities.GasEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GasRepository extends JpaRepository<GasEntity, Integer> {
}
