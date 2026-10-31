package com.aether.ms_calculator.calculator.services;

import com.aether.ms_calculator.calculator.dto.input.FindManyGasesInputDTO;
import com.aether.ms_calculator.calculator.dto.output.GasCatalogItemOutputDTO;
import com.aether.ms_calculator.calculator.mappers.CalculatorMapper;
import com.aether.ms_calculator.shared.persistence.postgres.entities.GasEntity;
import com.aether.ms_calculator.shared.persistence.postgres.repositories.GasRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FinaManyGasesService {

  private final GasRepository repository;

  public List<GasCatalogItemOutputDTO> findManyGases(FindManyGasesInputDTO input) {
    List<GasEntity> gases;

    if (input.name() != null && input.isGreen() != null){
      gases = repository.findByNameContainingIgnoreCaseAndBiogenic(input.name(), input.isGreen());
    } else if (input.name() != null){
      gases = repository.findByNameContainingIgnoreCase(input.name());
    } else if (input.isGreen() != null){
      gases = repository.findAllByBiogenic(input.isGreen());
    } else {
      gases = repository.findAll();
    }

    return gases.stream().map(CalculatorMapper::toGasCatalogItem).toList();
  }
}
