package com.aether.ms_calculator.calculator.services;

import com.aether.ms_calculator.calculator.dto.output.GasCatalogItemOutputDTO;
import com.aether.ms_calculator.calculator.mappers.CalculatorMapper;
import com.aether.ms_calculator.shared.persistence.postgres.repositories.GasRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GasCatalogService {

  private final GasRepository gasRepository;

  public List<GasCatalogItemOutputDTO> listGases() {
    return gasRepository.findAll().stream().map(CalculatorMapper::toGasCatalogItem).toList();
  }
}
