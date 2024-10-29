package com.coursework.stockmarketforecasting.Final;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Iterator;

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

    public void fetchAndSaveData(String symbol, String category) {
        String function;
        HttpResponse<JsonNode> response;

        switch (category.toLowerCase()) {
            case "акции":
                function = "TIME_SERIES_DAILY";
                response = Unirest.get(BASE_URL)
                        .queryString("function", function)
                        .queryString("symbol", symbol)
                        .queryString("apikey", API_KEY)
                        .asJson();
                processTimeSeriesData(response, symbol, category, "Time Series (Daily)");
                break;

            case "валюты":
                function = "FX_DAILY";
                response = Unirest.get(BASE_URL)
                        .queryString("function", function)
                        .queryString("from_symbol", symbol.substring(0, 3))
                        .queryString("to_symbol", symbol.substring(3))
                        .queryString("apikey", API_KEY)
                        .asJson();
                processTimeSeriesData(response, symbol, category, "Time Series FX (Daily)");
                break;

            case "драгоценные металлы":
            case "криптовалюты":
                function = "DIGITAL_CURRENCY_DAILY";
                response = Unirest.get(BASE_URL)
                        .queryString("function", function)
                        .queryString("symbol", symbol)
                        .queryString("market", "USD")
                        .queryString("apikey", API_KEY)
                        .asJson();
                processTimeSeriesData(response, symbol, category, "Time Series (Digital Currency Daily)");
                break;

            default:
                logger.error("Категория {} не поддерживается", category);
                throw new IllegalArgumentException("Категория не поддерживается");
        }
    }

    private void processTimeSeriesData(HttpResponse<JsonNode> response, String symbol, String category, String timeSeriesKey) {
        try {
            JSONObject responseBody = response.getBody().getObject();
            logger.info("API Response for {} - {}: {}", symbol, category, responseBody.toString());

            if (!responseBody.has(timeSeriesKey)) {
                logger.error("Expected '{}' key not found in response for symbol: {}", timeSeriesKey, symbol);
                throw new IllegalArgumentException(timeSeriesKey + " not found in the API response.");
            }

            JSONObject timeSeries = responseBody.getJSONObject(timeSeriesKey);
            Iterator<String> dates = timeSeries.keys();

            while (dates.hasNext()) {
                String dateStr = dates.next();
                LocalDate date = LocalDate.parse(dateStr);
                JSONObject dailyData = timeSeries.getJSONObject(dateStr);

                // Объявляем переменные для хранения значений
                double openPrice = 0;
                double highPrice = 0;
                double lowPrice = 0;
                double closePrice = 0;
                long volume = 0;

                // Обработка данных в зависимости от категории
                if (category.equalsIgnoreCase("акции") || category.equalsIgnoreCase("валюты")) {
                    // Для акций и валют
                    openPrice = dailyData.getDouble("1. open");
                    highPrice = dailyData.getDouble("2. high");
                    lowPrice = dailyData.getDouble("3. low");
                    closePrice = dailyData.getDouble("4. close");

                    // Объем доступен только для акций, в Forex данных его нет
                    if (category.equalsIgnoreCase("акции") && dailyData.has("5. volume")) {
                        volume = dailyData.getLong("5. volume");
                    }
                } else if (category.equalsIgnoreCase("криптовалюты") || category.equalsIgnoreCase("драгоценные металлы")) {
                    // Для криптовалют и драгоценных металлов
                    openPrice = dailyData.getDouble("1. open");
                    highPrice = dailyData.getDouble("2. high");
                    lowPrice = dailyData.getDouble("3. low");
                    closePrice = dailyData.getDouble("4. close");

                    // Объем присутствует для криптовалют, но может отсутствовать для некоторых драгоценных металлов
                    if (dailyData.has("5. volume")) {
                        volume = dailyData.getLong("5. volume");
                    }
                }

                // Создаем объект и сохраняем данные в базе
                FinancialData data = new FinancialData();
                data.setSymbol(symbol);
                data.setCategory(category);
                data.setDate(date);
                data.setOpenPrice(openPrice);
                data.setHighPrice(highPrice);
                data.setLowPrice(lowPrice);
                data.setClosePrice(closePrice);
                data.setVolume(volume);

                financialDataRepository.save(data);
            }
        } catch (Exception e) {
            logger.error("Error processing data for symbol: {} with category: {}. Exception: {}", symbol, category, e.getMessage());
            throw new RuntimeException(e);
        }
    }

}

