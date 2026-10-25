package com.aether.ms_calculator.calculator.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class InvestmentResultJson {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private InvestmentResultJson() {}

    public static ObjectNode fromRoi(RoiResult result) {
        ObjectNode node = MAPPER.createObjectNode();
        node.set("capex", result.capexEcho());
        node.set("wacc_monthly", result.waccMonthlyEcho());
        node.put("months", result.months());
        node.put("vp_total", result.vpTotal());
        node.put("vpl", result.vpl());
        node.put("roi_percent", result.roiPercent());
        node.put("viable", result.viable());
        return node;
    }

    public static ObjectNode fromPayback(PaybackResult result) {
        ObjectNode node = MAPPER.createObjectNode();
        node.set("capex", result.capexEcho());
        node.set("wacc_monthly", result.waccMonthlyEcho());
        node.put("months", result.months());
        node.put("vp_total", result.vpTotal());
        node.put("payback_months", result.paybackMonths());
        node.put("viable", result.viable());
        return node;
    }
}
