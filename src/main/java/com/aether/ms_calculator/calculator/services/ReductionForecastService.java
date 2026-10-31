package com.aether.ms_calculator.calculator.services;

import com.aether.ms_calculator.calculator.domain.CatalogGas;
import com.aether.ms_calculator.calculator.domain.ReductionForecastCalculator;
import com.aether.ms_calculator.calculator.domain.ReductionForecastParsed;
import com.aether.ms_calculator.calculator.domain.ReductionForecastResult;
import com.aether.ms_calculator.calculator.domain.ReductionForecastValidator;
import com.aether.ms_calculator.calculator.dto.input.ReductionForecastInputDTO;
import com.aether.ms_calculator.calculator.dto.output.ReductionForecastOutputDTO;
import com.aether.ms_calculator.calculator.mappers.CalculatorMapper;
import com.aether.ms_calculator.shared.exceptions.BadRequestException;
import com.aether.ms_calculator.shared.persistence.postgres.entities.GasEntity;
import com.aether.ms_calculator.shared.persistence.postgres.repositories.GasRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReductionForecastService {

  private final GasRepository gasRepository;
  private final ReductionForecastValidator reductionForecastValidator;
  private final ReductionForecastCalculator reductionForecastCalculator;

  public ReductionForecastOutputDTO forecast(ReductionForecastInputDTO input) {
    ReductionForecastParsed parsed =
        reductionForecastValidator.validate(CalculatorMapper.toForecastRequest(input));

    GasEntity currentGas =
        gasRepository
            .findById(parsed.currentGasId())
            .orElseThrow(
                () ->
                    BadRequestException.withLiteralMessage(
                        "Unknown gas id " + parsed.currentGasId() + ".", "validation_error"));
    GasEntity greenGas =
        gasRepository
            .findById(parsed.greenGasId())
            .orElseThrow(
                () ->
                    BadRequestException.withLiteralMessage(
                        "Unknown gas id " + parsed.greenGasId() + ".", "validation_error"));

    if (Boolean.TRUE.equals(currentGas.getBiogenic())) {
      throw BadRequestException.withLiteralMessage(
          "current_emission.gas must be a non-biogenic gas.", "validation_error");
    }
    if (!Boolean.TRUE.equals(greenGas.getBiogenic())) {
      throw BadRequestException.withLiteralMessage(
          "green_gas must be a biogenic gas.", "validation_error");
    }

    ReductionForecastResult result =
        reductionForecastCalculator.calculate(
            parsed,
            new CatalogGas(currentGas.getId(), currentGas.getGwp()),
            new CatalogGas(greenGas.getId(), greenGas.getGwp()));
    return CalculatorMapper.toForecastOutput(result);
  }
}
