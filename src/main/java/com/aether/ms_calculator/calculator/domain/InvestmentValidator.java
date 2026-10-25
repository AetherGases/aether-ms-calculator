package com.aether.ms_calculator.calculator.domain;

import com.aether.ms_calculator.shared.exceptions.BadRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.stereotype.Component;

@Component
public class InvestmentValidator {

    public static final int HORIZON_MONTHS = 60;
    private static final double MAX_WACC_MONTHLY = 1.0;

    private static final List<String> REQUEST_FIELDS =
            List.of("capex", "wacc_monthly", "cash_flows", "monthly_cash_flow");
    private static final List<String> CASH_FLOW_FIELDS = List.of("cash_flows", "monthly_cash_flow");

    public InvestmentParsed parse(JsonNode request) {
        return validate(request);
    }

    public InvestmentParsed validate(JsonNode request) {
        if (request == null || !request.isObject()) {
            throw invalid("The request must be a JSON object.");
        }

        rejectUnknownFields(request);

        JsonNode capexNode = request.get("capex");
        double capex = parsePositiveCapex(capexNode);

        JsonNode waccNode = request.get("wacc_monthly");
        double waccMonthly = parseWacc(waccNode);

        double[] cashFlows = parseCashFlows(request);

        return new InvestmentParsed(capexNode, waccNode, capex, waccMonthly, cashFlows);
    }

    private void rejectUnknownFields(JsonNode request) {
        Set<String> unknown = new TreeSet<>();
        request.fieldNames().forEachRemaining(field -> {
            if (!REQUEST_FIELDS.contains(field)) {
                unknown.add(field);
            }
        });
        if (!unknown.isEmpty()) {
            throw invalid(
                    "The request takes no " + String.join(", ", unknown)
                            + ". It accepts: "
                            + String.join(", ", REQUEST_FIELDS)
                            + ".");
        }
    }

    private double parsePositiveCapex(JsonNode node) {
        double capex = parseNumber(node, "capex");
        if (capex <= 0) {
            throw invalid(
                    "\"capex\" must be the initial investment, above zero, got " + PythonRepr.of(node) + ".");
        }
        return capex;
    }

    private double parseWacc(JsonNode node) {
        double wacc = parseNumber(node, "wacc_monthly");
        if (!(0 <= wacc && wacc < MAX_WACC_MONTHLY)) {
            throw invalid(
                    "\"wacc_monthly\" must be the monthly discount rate as a decimal between 0 and 1 (0.01 is 1% a month), got "
                            + PythonRepr.of(node)
                            + ". An annual rate must be converted before it is sent.");
        }
        return wacc;
    }

    private double[] parseCashFlows(JsonNode request) {
        List<String> given = new ArrayList<>();
        for (String field : CASH_FLOW_FIELDS) {
            if (request.has(field)) {
                given.add(field);
            }
        }

        if (given.size() != 1) {
            String suffix = given.isEmpty() ? "neither" : String.join(" and ", given);
            throw invalid(
                    "The request must carry exactly one of \"cash_flows\" (the monthly flows, up to "
                            + HORIZON_MONTHS
                            + ") or \"monthly_cash_flow\" (one flow repeated over the "
                            + HORIZON_MONTHS
                            + " months), got "
                            + suffix
                            + ".");
        }

        if ("monthly_cash_flow".equals(given.get(0))) {
            JsonNode flowNode = request.get("monthly_cash_flow");
            double flow = parseNumber(flowNode, "monthly_cash_flow");
            double[] flows = new double[HORIZON_MONTHS];
            for (int i = 0; i < HORIZON_MONTHS; i++) {
                flows[i] = flow;
            }
            return flows;
        }

        JsonNode flowsNode = request.get("cash_flows");
        if (!flowsNode.isArray() || flowsNode.isEmpty()) {
            throw invalid(
                    "\"cash_flows\" must be a list of monthly cash flows, oldest first, got "
                            + PythonRepr.of(flowsNode)
                            + ".");
        }

        ArrayNode flowsArray = (ArrayNode) flowsNode;
        if (flowsArray.size() > HORIZON_MONTHS) {
            throw invalid(
                    "\"cash_flows\" must carry at most "
                            + HORIZON_MONTHS
                            + " monthly flows, one per month of the horizon, got "
                            + flowsArray.size()
                            + ".");
        }

        double[] flows = new double[HORIZON_MONTHS];
        for (int i = 0; i < flowsArray.size(); i++) {
            JsonNode flowNode = flowsArray.get(i);
            flows[i] = parseNumber(flowNode, "cash_flows[" + i + "]");
        }
        return flows;
    }

    private double parseNumber(JsonNode node, String field) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            throw invalid(
                    "\"" + field + "\" must be a number, got None.");
        }
        if (node.isBoolean() || !node.isNumber()) {
            throw invalid(
                    "\"" + field + "\" must be a number, got " + PythonRepr.of(node) + ".");
        }
        double value = node.doubleValue();
        if (!Double.isFinite(value)) {
            throw invalid(
                    "\"" + field + "\" must be a number, got " + PythonRepr.of(node) + ".");
        }
        return value;
    }

    private static BadRequestException invalid(String message) {
        return BadRequestException.withLiteralMessage(message, "validation_error");
    }
}
