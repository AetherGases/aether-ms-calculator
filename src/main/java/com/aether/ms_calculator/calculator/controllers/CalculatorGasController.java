package com.aether.ms_calculator.calculator.controllers;

import com.aether.ms_calculator.calculator.dto.output.GasCatalogItemOutputDTO;
import com.aether.ms_calculator.calculator.services.GasCatalogService;
import com.aether.ms_calculator.shared.docs.CalculatorControllerDocs;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Calculator", description = "Discounted ROI, payback, and gas catalog for reduction forecasting.")
@RestController
@RequiredArgsConstructor
public class CalculatorGasController implements CalculatorControllerDocs.Gases {

  private final GasCatalogService gasCatalogService;

  @Override
  @GetMapping("/gases")
  public ResponseEntity<List<GasCatalogItemOutputDTO>> listGases() {
    return ResponseEntity.ok(gasCatalogService.listGases());
  }
}
