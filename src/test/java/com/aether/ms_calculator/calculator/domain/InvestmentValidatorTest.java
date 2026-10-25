package com.aether.ms_calculator.calculator.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class InvestmentValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final InvestmentValidator VALIDATOR = new InvestmentValidator();

    private JsonNode parse(String json) throws Exception {
        return MAPPER.readTree(json);
    }

    @Test
    void rejectsUnknownFieldsBeforeCapex() throws Exception {
        var error = assertThrows(
                InvestmentValidationException.class,
                () -> VALIDATOR.validate(parse("{\"discount_rate\":0.1,\"tax\":0.3}")));
        assertEquals(
                "The request takes no discount_rate, tax. It accepts: capex, wacc_monthly, cash_flows, monthly_cash_flow.",
                error.getMessage());
    }

    @Test
    void missingCapexIsNone() throws Exception {
        var error = assertThrows(
                InvestmentValidationException.class,
                () -> VALIDATOR.validate(parse("{\"wacc_monthly\":0.25,\"cash_flows\":[125]}")));
        assertEquals("\"capex\" must be a number, got None.", error.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "true", "\"150\"", "[150]"})
    void rejectsNonNumberCapex(String capexJson) throws Exception {
        var error = assertThrows(
                InvestmentValidationException.class,
                () -> VALIDATOR.validate(
                        parse("{\"capex\":" + capexJson + ",\"wacc_monthly\":0.25,\"cash_flows\":[125]}")));
        assertEquals("\"capex\" must be a number, got " + expectedRepr(capexJson) + ".", error.getMessage());
    }

    @Test
    void rejectsNonFiniteCapex() throws Exception {
        var error = assertThrows(
                InvestmentValidationException.class,
                () -> VALIDATOR.validate(
                        parse("{\"capex\":1e309,\"wacc_monthly\":0.25,\"cash_flows\":[125]}")));
        assertEquals("\"capex\" must be a number, got inf.", error.getMessage());
    }

    @Test
    void rejectsNonPositiveCapex() throws Exception {
        var error = assertThrows(
                InvestmentValidationException.class,
                () -> VALIDATOR.validate(
                        parse("{\"capex\":0,\"wacc_monthly\":0.25,\"cash_flows\":[125]}")));
        assertEquals("\"capex\" must be the initial investment, above zero, got 0.", error.getMessage());
    }

    @Test
    void rejectsInvalidWaccRange() throws Exception {
        var error = assertThrows(
                InvestmentValidationException.class,
                () -> VALIDATOR.validate(
                        parse("{\"capex\":150,\"wacc_monthly\":1,\"cash_flows\":[125]}")));
        assertEquals(
                "\"wacc_monthly\" must be the monthly discount rate as a decimal between 0 and 1 (0.01 is 1% a month), got 1. An annual rate must be converted before it is sent.",
                error.getMessage());
    }

    @Test
    void acceptsWaccZero() throws Exception {
        InvestmentParsed parsed = VALIDATOR.validate(
                parse("{\"capex\":150,\"wacc_monthly\":0,\"cash_flows\":[125]}"));
        assertEquals(0.0, parsed.waccMonthly());
    }

    @Test
    void rejectsNeitherFlowField() throws Exception {
        var error = assertThrows(
                InvestmentValidationException.class,
                () -> VALIDATOR.validate(parse("{\"capex\":150,\"wacc_monthly\":0.25}")));
        assertEquals(
                "The request must carry exactly one of \"cash_flows\" (the monthly flows, up to 60) or \"monthly_cash_flow\" (one flow repeated over the 60 months), got neither.",
                error.getMessage());
    }

    @Test
    void rejectsBothFlowFields() throws Exception {
        var error = assertThrows(
                InvestmentValidationException.class,
                () -> VALIDATOR.validate(
                        parse("{\"capex\":150,\"wacc_monthly\":0.25,\"cash_flows\":[125],\"monthly_cash_flow\":5000}")));
        assertEquals(
                "The request must carry exactly one of \"cash_flows\" (the monthly flows, up to 60) or \"monthly_cash_flow\" (one flow repeated over the 60 months), got cash_flows and monthly_cash_flow.",
                error.getMessage());
    }

    @Test
    void rejectsEmptyCashFlowsList() throws Exception {
        var error = assertThrows(
                InvestmentValidationException.class,
                () -> VALIDATOR.validate(
                        parse("{\"capex\":150,\"wacc_monthly\":0.25,\"cash_flows\":[]}")));
        assertEquals(
                "\"cash_flows\" must be a list of monthly cash flows, oldest first, got [].",
                error.getMessage());
    }

    @Test
    void rejectsMoreThanSixtyFlows() throws Exception {
        StringBuilder flows = new StringBuilder("[");
        for (int i = 0; i < 61; i++) {
            if (i > 0) {
                flows.append(',');
            }
            flows.append('1');
        }
        flows.append(']');
        var error = assertThrows(
                InvestmentValidationException.class,
                () -> VALIDATOR.validate(
                        parse("{\"capex\":150,\"wacc_monthly\":0.25,\"cash_flows\":" + flows + "}")));
        assertEquals(
                "\"cash_flows\" must carry at most 60 monthly flows, one per month of the horizon, got 61.",
                error.getMessage());
    }

    @Test
    void rejectsBadCashFlowItem() throws Exception {
        var error = assertThrows(
                InvestmentValidationException.class,
                () -> VALIDATOR.validate(
                        parse("{\"capex\":150,\"wacc_monthly\":0.25,\"cash_flows\":[125,\"x\"]}")));
        assertEquals("\"cash_flows[1]\" must be a number, got 'x'.", error.getMessage());
    }

    @Test
    void rejectsBadMonthlyCashFlow() throws Exception {
        var error = assertThrows(
                InvestmentValidationException.class,
                () -> VALIDATOR.validate(
                        parse("{\"capex\":150,\"wacc_monthly\":0.25,\"monthly_cash_flow\":true}")));
        assertEquals("\"monthly_cash_flow\" must be a number, got True.", error.getMessage());
    }

    @Test
    void padsShortCashFlowsAndEchoesJsonValues() throws Exception {
        InvestmentParsed parsed = VALIDATOR.validate(
                parse("{\"capex\":150,\"wacc_monthly\":0.25,\"cash_flows\":[125,156.25]}"));
        assertEquals(60, parsed.cashFlows().length);
        assertEquals(125.0, parsed.cashFlows()[0]);
        assertEquals(156.25, parsed.cashFlows()[1]);
        assertEquals(0.0, parsed.cashFlows()[2]);
        assertEquals(150, parsed.capexEcho().intValue());
        assertEquals(0.25, parsed.waccMonthlyEcho().doubleValue());
    }

    @Test
    void repeatsMonthlyCashFlowSixtyTimes() throws Exception {
        InvestmentParsed parsed = VALIDATOR.validate(
                parse("{\"capex\":150,\"wacc_monthly\":0.25,\"monthly_cash_flow\":5000}"));
        assertEquals(60, parsed.cashFlows().length);
        assertEquals(5000.0, parsed.cashFlows()[0]);
        assertEquals(5000.0, parsed.cashFlows()[59]);
    }

    private static String expectedRepr(String capexJson) {
        if ("null".equals(capexJson)) {
            return "None";
        }
        if ("true".equals(capexJson)) {
            return "True";
        }
        if ("\"150\"".equals(capexJson)) {
            return "'150'";
        }
        return "[150]";
    }
}
