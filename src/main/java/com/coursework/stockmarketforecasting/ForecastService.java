package com.coursework.stockmarketforecasting;

import com.coursework.stockmarketforecasting.Final.FinancialData;
import com.coursework.stockmarketforecasting.Final.FinancialDataRepository;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import org.springframework.stereotype.Service;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ForecastService {

    private final FinancialDataRepository financialDataRepository;
    private static final Logger logger = LoggerFactory.getLogger(ForecastService.class);

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

    public double predictWithLSTM(String symbol, String category) {
        List<FinancialData> financialDataList = financialDataRepository.findBySymbolAndCategory(symbol, category);

        // Создаем JSON массив данных
        JSONArray dataArray = new JSONArray();
        for (FinancialData data : financialDataList) {
            JSONObject dataPoint = new JSONObject();
            dataPoint.put("open", data.getOpenPrice());
            dataPoint.put("high", data.getHighPrice());
            dataPoint.put("low", data.getLowPrice());
            dataPoint.put("close", data.getClosePrice());
            dataPoint.put("volume", data.getVolume());
            dataArray.put(dataPoint);
        }

        // Формируем окончательный JSON объект
        JSONObject jsonData = new JSONObject();
        jsonData.put("data", dataArray);

        logger.info("JSON Data to be sent: {}", jsonData.toString());

        // Отправляем JSON объект через Unirest
        HttpResponse<JsonNode> response = Unirest.post("http://localhost:8000/forecast")
                .header("Content-Type", "application/json")
                .body(jsonData.toString())
                .asJson();

        if (response.getStatus() != 200) {
            throw new RuntimeException("Ошибка при вызове LSTM API: " + response.getBody());
        }

        return response.getBody().getObject().getDouble("forecast");
    }
}


