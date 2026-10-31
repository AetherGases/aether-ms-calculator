package com.aether.ms_calculator.calculator.controllers;

import com.aether.ms_calculator.calculator.dto.output.GasCatalogItemOutputDTO;
import com.aether.ms_calculator.calculator.dto.query_params.FindManyGasesQueryParamsDTO;
import com.aether.ms_calculator.calculator.mappers.CalculatorMapper;
import com.aether.ms_calculator.calculator.services.FinaManyGasesService;
import com.aether.ms_calculator.shared.docs.CalculatorControllerDocs;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Calculator", description = "Discounted ROI, payback, and gas catalog for reduction forecasting.")
@RestController
@RequiredArgsConstructor
public class FindManyGasesController implements CalculatorControllerDocs.FindManyGases {

  private final FinaManyGasesService finaManyGasesService;

  @Override
  @GetMapping("/gases")
  public ResponseEntity<List<GasCatalogItemOutputDTO>> findManyGases(
      @ModelAttribute
      @Valid
      FindManyGasesQueryParamsDTO query
  ){
    return new ResponseEntity<>(
        this.finaManyGasesService.findManyGases(
            CalculatorMapper.convertFindManyGasesQueryToInput(query)
        ),
        HttpStatus.OK
    );
  }
}
