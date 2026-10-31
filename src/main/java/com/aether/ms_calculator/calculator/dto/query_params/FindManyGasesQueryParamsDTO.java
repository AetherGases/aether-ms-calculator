package com.aether.ms_calculator.calculator.dto.query_params;

import com.aether.ms_calculator.shared.AetherConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record FindManyGasesQueryParamsDTO(
    @Size(
        max = AetherConstants.MAX_GAS_NAME_LENGTH,
        message = "{validation.gas-name.size}"
    )
    @Schema(
        description = "O filtro de nome dos gases (opcional, parcial e sem diferenciar maiúsculas)",
        example = "metano"
    )
    String name,

    @Schema(
        description = "Filtro se o gás é verde ou não",
        example = "true"
    )
    Boolean isGreen
) {
}
