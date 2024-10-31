package com.coursework.stockmarketforecasting.Final;

import com.coursework.stockmarketforecasting.ForecastService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/financial-data")
public class FinancialDataController {

    private final FinancialDataService financialDataService;
    private final ForecastService forecastService;

    @Autowired
    public FinancialDataController(FinancialDataService financialDataService, ForecastService forecastService) {
        this.financialDataService = financialDataService;
        this.forecastService = forecastService;
    }

    @PostMapping("/fetch/{symbol}/{category}")
    public String fetchFinancialData(@PathVariable String symbol, @PathVariable String category) {
        financialDataService.fetchAndSaveData(symbol, category);
        return "Данные успешно загружены и сохранены.";
    }

    @GetMapping("/predict/{symbol}/{category}")
    public double predictNextDayClosePrice(@PathVariable String symbol, @PathVariable String category) {
//        return forecastService.predictNextDayClosePrice(symbol, category);
        return forecastService.predictWithARIMA(symbol, category);
    }
}


