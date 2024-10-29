package com.coursework.stockmarketforecasting;

import com.coursework.stockmarketforecasting.Final.FinancialData;
import com.coursework.stockmarketforecasting.Final.FinancialDataRepository;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ForecastService {

    private final FinancialDataRepository financialDataRepository;

    @Autowired
    public ForecastService(FinancialDataRepository financialDataRepository) {
        this.financialDataRepository = financialDataRepository;
    }

    public double predictNextDayClosePrice(String symbol, String category) {
        List<FinancialData> financialDataList = financialDataRepository.findBySymbolAndCategory(symbol, category);

        if (financialDataList.isEmpty()) {
            throw new IllegalArgumentException("Данные для прогнозирования не найдены.");
        }

        SimpleRegression regression = new SimpleRegression();

        for (int i = 0; i < financialDataList.size(); i++) {
            FinancialData data = financialDataList.get(i);
            regression.addData(i, data.getClosePrice());
        }

        double nextDayIndex = financialDataList.size();
        return regression.predict(nextDayIndex);
    }
}


