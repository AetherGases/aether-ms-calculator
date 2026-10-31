package com.aether.ms_calculator.shared.docs;

import com.aether.ms_calculator.calculator.dto.input.CalculateInvestmentInputDTO;
import com.aether.ms_calculator.calculator.dto.output.CalculatePaybackOutputDTO;
import com.aether.ms_calculator.calculator.dto.output.CalculateRoiOutputDTO;
import com.aether.ms_calculator.calculator.dto.output.CostReductionForecastOutputDTO;
import com.aether.ms_calculator.calculator.dto.output.GasCatalogItemOutputDTO;
import com.aether.ms_calculator.calculator.dto.output.ReductionForecastOutputDTO;
import com.aether.ms_calculator.calculator.dto.query_params.FindManyGasesQueryParamsDTO;
import com.aether.ms_calculator.calculator.dto.request.CostReductionForecastRequestDTO;
import com.aether.ms_calculator.calculator.dto.request.ReductionForecastRequestDTO;
import com.aether.ms_calculator.shared.docs.schemas.CalculateInvestmentRequestSchema;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import io.swagger.v3.oas.annotations.Operation;
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
            "Validates the snake_case investment request and returns present value, VPL, ROI percent and viability. "
                + "Internal route for the AI hub: it is authenticated by the authentication-key header, not by the Bearer token.",
        tags = {"Calculator"},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = CalculateInvestmentRequestSchema.class))),
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
            "Same request as ROI. Returns payback_months (or -1 when the outlay is never recovered) and viability. "
                + "Internal route for the AI hub: it is authenticated by the authentication-key header, not by the Bearer token.",
        tags = {"Calculator"},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = CalculateInvestmentRequestSchema.class))),
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

  public interface FindManyGases {
    @Operation(
        summary = "List the gases in the catalog, optionally filtered by name.",
        description =
            "Returns id, name, formula, is_green (gas.is_biogenic), and gwp for hub pickers. "
                + "The optional `name` query param filters by a case-insensitive partial match; "
                + "when it is omitted or blank, the full catalog is returned. "
                + "Requires JWT with calculator:gas.",
        tags = {"Calculator"},
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
    ResponseEntity<List<GasCatalogItemOutputDTO>> findManyGases(
        @ParameterObject FindManyGasesQueryParamsDTO query);
  }

  public interface ReductionForecast {
    @Operation(
        summary = "Forecast remaining emissions and fossil tCO2e avoided after switching to a green gas.",
        description =
            "Uses catalog GWP values at request time. Requires JWT with calculator:gas.",
        tags = {"Calculator"},
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
        @RequestBody ReductionForecastRequestDTO body);
  }

  public interface CostReductionForecast {
    @Operation(
        summary = "Forecast monthly cost savings from a reduction percentage.",
        description =
            "Returns monthly savings, new monthly cost, total savings, and a month-by-month "
                + "projection. Requires JWT with calculator:roi.",
        tags = {"Calculator"},
        responses = {
            @ApiResponse(
                description = "Success",
                responseCode = "200",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = CostReductionForecastOutputDTO.class))),
            @ApiResponse(description = "Validation error", responseCode = "400", content = @Content),
            @ApiResponse(description = "Unauthorized", responseCode = "401", content = @Content),
            @ApiResponse(description = "Forbidden", responseCode = "403", content = @Content),
            @ApiResponse(description = "Unsupported media type", responseCode = "415", content = @Content)
        }
    )
    ResponseEntity<CostReductionForecastOutputDTO> forecastCostReduction(
        @RequestBody CostReductionForecastRequestDTO body);
  }
}
