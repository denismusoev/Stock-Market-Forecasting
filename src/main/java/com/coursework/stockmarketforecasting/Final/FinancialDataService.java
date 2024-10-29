package com.coursework.stockmarketforecasting.Final;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;

@Service
public class FinancialDataService {

    private static final Logger logger = LoggerFactory.getLogger(FinancialDataService.class);
    private static final String API_KEY = "X02UMNMMJ8GT9HVW";
    private static final String BASE_URL = "https://www.alphavantage.co/query";

    private final FinancialDataRepository financialDataRepository;

    @Autowired
    public FinancialDataService(FinancialDataRepository financialDataRepository) {
        this.financialDataRepository = financialDataRepository;
    }

    public void fetchAndSaveData(String symbol, String category, String function) {
        try {
            HttpResponse<JsonNode> response = Unirest.get(BASE_URL)
                    .queryString("function", function)
                    .queryString("symbol", symbol)
                    .queryString("apikey", API_KEY)
                    .asJson();

            // Логируем полный ответ API
            logger.info("API Response for {} - {}: {}", symbol, category, response.getBody().toString());

            JSONObject responseBody = response.getBody().getObject();

            // Проверяем, содержит ли ответ нужный ключ, и логируем результат
            if (!responseBody.has("Time Series (Daily)")) {
                logger.error("Expected 'Time Series (Daily)' key not found in response for symbol: {}", symbol);
                throw new IllegalArgumentException("Time Series (Daily) not found in the API response.");
            }

            JSONObject timeSeries = responseBody.getJSONObject("Time Series (Daily)");

            // Обработка данных...

        } catch (Exception e) {
            // Логируем исключение
            logger.error("Error fetching data for symbol: {} with category: {}. Exception: {}", symbol, category, e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
