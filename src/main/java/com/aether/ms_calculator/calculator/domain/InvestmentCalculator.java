package com.aether.ms_calculator.calculator.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

@Component
public class InvestmentCalculator {

    private static final int PAYBACK_NOT_RECOVERED = -1;

    public RoiResult calculateRoi(InvestmentParsed parsed) {
        double vpTotalRaw = presentValueTotal(parsed.cashFlows(), parsed.waccMonthly());
        double vplRaw = vpTotalRaw - parsed.capex();
        boolean viable = parsed.capex() <= vpTotalRaw;
        double roiPercentRaw = (vplRaw / parsed.capex()) * 100.0;

        return new RoiResult(
                parsed.capexEcho(),
                parsed.waccMonthlyEcho(),
                InvestmentValidator.HORIZON_MONTHS,
                roundMoney(vpTotalRaw),
                roundMoney(vplRaw),
                roundMoney(roiPercentRaw),
                viable);
    }

    public PaybackResult calculatePayback(InvestmentParsed parsed) {
        double[] presentValues = presentValues(parsed.cashFlows(), parsed.waccMonthly());
        double vpTotalRaw = sum(presentValues);
        boolean viable = parsed.capex() <= vpTotalRaw;

        return new PaybackResult(
                parsed.capexEcho(),
                parsed.waccMonthlyEcho(),
                InvestmentValidator.HORIZON_MONTHS,
                roundMoney(vpTotalRaw),
                paybackMonth(presentValues, parsed.capex()),
                viable);
    }

    private static double[] presentValues(double[] cashFlows, double waccMonthly) {
        double[] presentValues = new double[cashFlows.length];
        for (int month = 1; month <= cashFlows.length; month++) {
            presentValues[month - 1] = cashFlows[month - 1] / Math.pow(1.0 + waccMonthly, month);
        }
        return presentValues;
    }

    private static double presentValueTotal(double[] cashFlows, double waccMonthly) {
        return sum(presentValues(cashFlows, waccMonthly));
    }

    private static double sum(double[] values) {
        double total = 0.0;
        for (double value : values) {
            total += value;
        }
        return total;
    }

    private static int paybackMonth(double[] presentValues, double capex) {
        double accumulated = 0.0;
        for (int month = 1; month <= presentValues.length; month++) {
            accumulated += presentValues[month - 1];
            if (accumulated >= capex) {
                return month;
            }
        }
        return PAYBACK_NOT_RECOVERED;
    }

    private static double roundMoney(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_EVEN).doubleValue();
    }
}
