package com.aether.ms_calculator.shared.docs;

import com.aether.ms_calculator.calculator.dto.input.CalculateInvestmentInputDTO;
import com.aether.ms_calculator.calculator.dto.output.CalculatePaybackOutputDTO;
import com.aether.ms_calculator.calculator.dto.output.CalculateRoiOutputDTO;
import com.aether.ms_calculator.calculator.dto.input.ReductionForecastInputDTO;
import com.aether.ms_calculator.calculator.dto.output.GasCatalogItemOutputDTO;
import com.aether.ms_calculator.calculator.dto.output.ReductionForecastOutputDTO;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

public final class CalculatorControllerDocs {

  private CalculatorControllerDocs() {}

  public interface Roi {
    @Operation(
        summary = "Calculate discounted ROI for a 60-month investment.",
        description =
            "Validates the snake_case investment request and returns present value, VPL, ROI percent and viability.",
        tags = {"Calculator"},
        parameters = {
            @Parameter(
                name = "authentication-key",
                in = ParameterIn.HEADER,
                required = true,
                description = "Shared internal key used by the hub and this service.")
        },
        responses = {
            @ApiResponse(
                description = "Success",
                responseCode = "200",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = CalculateRoiOutputDTO.class))),
            @ApiResponse(description = "Validation error", responseCode = "400", content = @Content),
            @ApiResponse(description = "Unauthorized", responseCode = "401", content = @Content),
            @ApiResponse(description = "Service unavailable", responseCode = "503", content = @Content)
        }
    )
    ResponseEntity<CalculateRoiOutputDTO> calculateRoi(@RequestBody CalculateInvestmentInputDTO body);
  }

  public interface Payback {
    @Operation(
        summary = "Calculate discounted payback month for a 60-month investment.",
        description =
            "Same request as ROI. Returns payback_months (or -1 when the outlay is never recovered) and viability.",
        tags = {"Calculator"},
        parameters = {
            @Parameter(
                name = "authentication-key",
                in = ParameterIn.HEADER,
                required = true,
                description = "Shared internal key used by the hub and this service.")
        },
        responses = {
            @ApiResponse(
                description = "Success",
                responseCode = "200",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = CalculatePaybackOutputDTO.class))),
            @ApiResponse(description = "Validation error", responseCode = "400", content = @Content),
            @ApiResponse(description = "Unauthorized", responseCode = "401", content = @Content),
            @ApiResponse(description = "Service unavailable", responseCode = "503", content = @Content)
        }
    )
    ResponseEntity<CalculatePaybackOutputDTO> calculatePayback(
        @RequestBody CalculateInvestmentInputDTO body);
  }

  public interface Gases {
    @Operation(
        summary = "List every gas in the catalog.",
        description =
            "Returns id, name, formula, is_green (gas.is_biogenic), and gwp for hub pickers. "
                + "Requires JWT with calculator:gas.",
        tags = {"Calculator"},
        parameters = {
            @Parameter(
                name = "Authorization",
                in = ParameterIn.HEADER,
                required = true,
                description = "Bearer JWT for an employee with calculator:gas.")
        },
        responses = {
            @ApiResponse(
                description = "Success",
                responseCode = "200",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(
                        schema = @Schema(implementation = GasCatalogItemOutputDTO.class)))),
            @ApiResponse(description = "Unauthorized", responseCode = "401", content = @Content),
            @ApiResponse(description = "Forbidden", responseCode = "403", content = @Content)
        }
    )
    ResponseEntity<List<GasCatalogItemOutputDTO>> listGases();
  }

  public interface ReductionForecast {
    @Operation(
        summary = "Forecast remaining emissions and fossil tCO2e avoided after switching to a green gas.",
        description =
            "Uses catalog GWP values at request time. Requires JWT with calculator:gas.",
        tags = {"Calculator"},
        parameters = {
            @Parameter(
                name = "Authorization",
                in = ParameterIn.HEADER,
                required = true,
                description = "Bearer JWT for an employee with calculator:gas.")
        },
        responses = {
            @ApiResponse(
                description = "Success",
                responseCode = "200",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ReductionForecastOutputDTO.class))),
            @ApiResponse(description = "Validation error", responseCode = "400", content = @Content),
            @ApiResponse(description = "Unauthorized", responseCode = "401", content = @Content),
            @ApiResponse(description = "Forbidden", responseCode = "403", content = @Content)
        }
    )
    ResponseEntity<ReductionForecastOutputDTO> forecastReduction(
        @RequestBody ReductionForecastInputDTO body);
  }
}
