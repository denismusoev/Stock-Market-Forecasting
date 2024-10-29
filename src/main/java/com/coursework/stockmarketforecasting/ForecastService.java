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
        // Получаем данные по цене закрытия для указанного символа и категории
        List<FinancialData> financialDataList = financialDataRepository.findBySymbolAndCategory(symbol, category);

        if (financialDataList.isEmpty()) {
            throw new IllegalArgumentException("Данные для прогнозирования не найдены.");
        }

        // Создаем линейную регрессию
        SimpleRegression regression = new SimpleRegression();

        // Добавляем данные в модель регрессии (используем дату как независимую переменную, цену закрытия как зависимую)
        for (int i = 0; i < financialDataList.size(); i++) {
            FinancialData data = financialDataList.get(i);
            regression.addData(i, data.getClosePrice()); // Используем индекс как временную метку
        }

        // Прогнозируем цену закрытия на следующий день
        double nextDayIndex = financialDataList.size();
        return regression.predict(nextDayIndex);
    }
}

