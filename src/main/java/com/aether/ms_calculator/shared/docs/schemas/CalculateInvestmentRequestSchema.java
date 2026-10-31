package com.aether.ms_calculator.shared.docs.schemas;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Swagger-only contract of the /roi and /payback body. The controllers bind the body as JsonNode
 * (see CalculateInvestmentInputDTO) so the Aeko hub gets the exact validation messages of the
 * InvestmentValidator; this record only documents the expected shape.
 */
@Schema(
    name = "CalculateInvestmentRequest",
    description =
        "Investment over a 60-month horizon. Send exactly one of \"cash_flows\" or \"monthly_cash_flow\".")
public record CalculateInvestmentRequestSchema(
    @Schema(
        description = "Initial investment, above zero.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        exclusiveMinimumValue = 0,
        example = "100000")
    Double capex,

    @JsonProperty("wacc_monthly")
    @Schema(
        description =
            "Monthly discount rate as a decimal, from 0 (inclusive) to 1 (exclusive): 0.01 is 1% a month. "
                + "An annual rate must be converted before it is sent.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        minimum = "0",
        exclusiveMaximumValue = 1,
        example = "0.01")
    Double waccMonthly,

    @JsonProperty("cash_flows")
    @ArraySchema(
        arraySchema = @Schema(
            description =
                "Monthly cash flows, oldest first, up to 60. Mutually exclusive with \"monthly_cash_flow\"."),
        schema = @Schema(type = "number", example = "2500"),
        minItems = 1,
        maxItems = 60)
    List<Double> cashFlows,

    @JsonProperty("monthly_cash_flow")
    @Schema(
        description =
            "One cash flow repeated over the 60 months. Mutually exclusive with \"cash_flows\".",
        example = "2500")
    Double monthlyCashFlow
) {
}
