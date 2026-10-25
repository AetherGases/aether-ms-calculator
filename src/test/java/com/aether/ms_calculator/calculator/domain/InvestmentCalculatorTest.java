package com.aether.ms_calculator.calculator.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class InvestmentCalculatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final InvestmentValidator VALIDATOR = new InvestmentValidator();
    private static final InvestmentCalculator CALCULATOR = new InvestmentCalculator();

    private InvestmentParsed parse(String json) throws Exception {
        return VALIDATOR.validate(MAPPER.readTree(json));
    }

    @Test
    void workedExampleRoi() throws Exception {
        InvestmentParsed parsed = parse(
                "{\"capex\":150,\"wacc_monthly\":0.25,\"cash_flows\":[125,156.25]}");
        RoiResult result = CALCULATOR.calculateRoi(parsed);

        assertEquals(150, result.capexEcho().intValue());
        assertEquals(0.25, result.waccMonthlyEcho().doubleValue());
        assertEquals(60, result.months());
        assertEquals(200.0, result.vpTotal());
        assertEquals(50.0, result.vpl());
        assertEquals(33.33, result.roiPercent());
        assertTrue(result.viable());
    }

    @Test
    void workedExamplePayback() throws Exception {
        InvestmentParsed parsed = parse(
                "{\"capex\":150,\"wacc_monthly\":0.25,\"cash_flows\":[125,156.25]}");
        PaybackResult result = CALCULATOR.calculatePayback(parsed);

        assertEquals(200.0, result.vpTotal());
        assertEquals(2, result.paybackMonths());
        assertTrue(result.viable());
    }

    @Test
    void unviableRoiWithCapex250() throws Exception {
        InvestmentParsed parsed = parse(
                "{\"capex\":250,\"wacc_monthly\":0.25,\"cash_flows\":[125,156.25]}");
        RoiResult result = CALCULATOR.calculateRoi(parsed);

        assertEquals(-50.0, result.vpl());
        assertEquals(-20.0, result.roiPercent());
        assertFalse(result.viable());
    }

    @Test
    void unviablePaybackWithCapex250() throws Exception {
        InvestmentParsed parsed = parse(
                "{\"capex\":250,\"wacc_monthly\":0.25,\"cash_flows\":[125,156.25]}");
        PaybackResult result = CALCULATOR.calculatePayback(parsed);

        assertEquals(-1, result.paybackMonths());
        assertFalse(result.viable());
    }

    @Test
    void exactRecoveryInMonthOne() throws Exception {
        InvestmentParsed parsed = parse(
                "{\"capex\":100,\"wacc_monthly\":0.25,\"cash_flows\":[125]}");
        RoiResult roi = CALCULATOR.calculateRoi(parsed);
        PaybackResult payback = CALCULATOR.calculatePayback(parsed);

        assertEquals(100.0, roi.vpTotal());
        assertEquals(0.0, roi.roiPercent());
        assertTrue(roi.viable());
        assertEquals(1, payback.paybackMonths());
    }

    @Test
    void halfToEvenRoundingCase() throws Exception {
        InvestmentParsed parsed = parse(
                "{\"capex\":1000,\"wacc_monthly\":0.01,\"cash_flows\":[333.333]}");
        RoiResult result = CALCULATOR.calculateRoi(parsed);

        assertEquals(330.03, result.vpTotal());
        assertEquals(-669.97, result.vpl());
    }

    @Test
    void monthlyCashFlowDiscountedOverSixtyMonths() throws Exception {
        InvestmentParsed parsed = parse(
                "{\"capex\":150000,\"wacc_monthly\":0.01,\"monthly_cash_flow\":5000}");
        RoiResult roi = CALCULATOR.calculateRoi(parsed);
        PaybackResult payback = CALCULATOR.calculatePayback(parsed);

        assertEquals(224775.19, roi.vpTotal());
        assertEquals(49.85, roi.roiPercent());
        assertTrue(roi.viable());
        assertEquals(36, payback.paybackMonths());
    }

    @Test
    void paybackWithZeroWacc() throws Exception {
        InvestmentParsed parsed = parse(
                "{\"capex\":250,\"wacc_monthly\":0,\"cash_flows\":[100,200,300]}");
        PaybackResult result = CALCULATOR.calculatePayback(parsed);

        assertEquals(2, result.paybackMonths());
        assertEquals(600.0, result.vpTotal());
    }

    @Test
    void paybackUsesAccumulatedNotIsolatedMonth() throws Exception {
        InvestmentParsed parsed = parse(
                "{\"capex\":290,\"wacc_monthly\":0,\"cash_flows\":[100,100,100]}");
        PaybackResult result = CALCULATOR.calculatePayback(parsed);

        assertEquals(3, result.paybackMonths());
    }

    @Test
    void paybackThenUnviableProject() throws Exception {
        StringBuilder json = new StringBuilder("{\"capex\":5900,\"wacc_monthly\":0,\"cash_flows\":[");
        for (int i = 0; i < 30; i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append("200");
        }
        json.append(",-6000");
        for (int i = 0; i < 29; i++) {
            json.append(",200");
        }
        json.append("]}");

        InvestmentParsed parsed = parse(json.toString());
        PaybackResult result = CALCULATOR.calculatePayback(parsed);

        assertEquals(30, result.paybackMonths());
        assertEquals(5800.0, result.vpTotal());
        assertFalse(result.viable());
    }
}
