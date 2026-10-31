package com.aether.ms_calculator.shared.persistence.postgres.repositories;

import com.aether.ms_calculator.shared.persistence.postgres.entities.GasEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GasRepository extends JpaRepository<GasEntity, Integer> {
  List<GasEntity> findByNameContainingIgnoreCaseAndBiogenic(String name, Boolean biogenic);
  List<GasEntity> findByNameContainingIgnoreCase(String name);
  List<GasEntity> findAllByBiogenic(Boolean biogenic);
}
