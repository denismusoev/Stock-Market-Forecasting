package com.coursework.stockmarketforecasting.Final;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class FinancialDataService {

    private static final Logger logger = LoggerFactory.getLogger(FinancialDataService.class);
    private static final String API_KEY = "X02UMNMMJ8GT9HVW";
    private static final String BASE_URL = "https://www.alphavantage.co/query";
    private static final int BATCH_SIZE = 50;

    private final FinancialDataRepository financialDataRepository;

    @Autowired
    public FinancialDataService(FinancialDataRepository financialDataRepository) {
        this.financialDataRepository = financialDataRepository;
    }

    @Transactional
    public void fetchAndSaveData(String symbol, String category) {
        String function;
        String timeSeriesKey;
        HttpResponse<JsonNode> response;

        switch (category.toLowerCase()) {
            case "stocks":
                function = "TIME_SERIES_DAILY";
                timeSeriesKey = "Time Series (Daily)";
                response = Unirest.get(BASE_URL)
                        .queryString("function", function)
                        .queryString("symbol", symbol)
//                        .queryString("outputsize", "full")
                        .queryString("apikey", API_KEY)
                        .asJson();
                processTimeSeriesData(response, symbol, category, timeSeriesKey);
                break;

            case "cryptocurrency":
                function = "DIGITAL_CURRENCY_DAILY";
                timeSeriesKey = "Time Series (Digital Currency Daily)";
                response = Unirest.get(BASE_URL)
                        .queryString("function", function)
                        .queryString("symbol", symbol)
                        .queryString("market", "USD")
                        .queryString("apikey", API_KEY)
                        .asJson();
                processCryptoData(response, symbol, category, timeSeriesKey);
                break;

            case "forex":
                function = "FX_DAILY";
                timeSeriesKey = "Time Series FX (Daily)";
                response = Unirest.get(BASE_URL)
                        .queryString("function", function)
                        .queryString("from_symbol", symbol.substring(0, 3))
                        .queryString("to_symbol", symbol.substring(3))
                        .queryString("apikey", API_KEY)
                        .asJson();
                processTimeSeriesData(response, symbol, category, timeSeriesKey);
                break;

            case "oil":
                function = "WTI";
                response = Unirest.get(BASE_URL)
                        .queryString("function", function)
                        .queryString("interval", "monthly")
                        .queryString("apikey", API_KEY)
                        .asJson();
                processCommodityData(response, symbol, category);
                break;

            case "gdp":
                function = "REAL_GDP";
                response = Unirest.get(BASE_URL)
                        .queryString("function", function)
                        .queryString("interval", "annual")
                        .queryString("apikey", API_KEY)
                        .asJson();
                processEconomicData(response, symbol, category);
                break;

            default:
                logger.error("Category {} is not supported", category);
                throw new IllegalArgumentException("Category is not supported");
        }
    }

    private void processTimeSeriesData(HttpResponse<JsonNode> response, String symbol, String category, String timeSeriesKey) {
        JSONObject responseBody = response.getBody().getObject();
        if (!responseBody.has(timeSeriesKey)) {
            throw new IllegalArgumentException("Time series data not found");
        }
        JSONObject timeSeries = responseBody.getJSONObject(timeSeriesKey);
        List<FinancialData> dataBatch = new ArrayList<>();

        for (String dateStr : timeSeries.keySet()) {
            JSONObject dailyData = timeSeries.getJSONObject(dateStr);
            double open = dailyData.getDouble("1. open");
            double high = dailyData.getDouble("2. high");
            double low = dailyData.getDouble("3. low");
            double close = dailyData.getDouble("4. close");
            long volume = dailyData.optLong("5. volume", 0);

            FinancialData data = new FinancialData(symbol, category, LocalDate.parse(dateStr), open, high, low, close, volume);
            dataBatch.add(data);

            if (dataBatch.size() >= BATCH_SIZE) {
                financialDataRepository.saveAll(dataBatch);
                financialDataRepository.flush(); // сбросить в базу, чтобы освободить память
                dataBatch.clear(); // очистить список
            }
        }

        if (!dataBatch.isEmpty()) {
            financialDataRepository.saveAll(dataBatch);
            financialDataRepository.flush();
        }
    }

    private void processCryptoData(HttpResponse<JsonNode> response, String symbol, String category, String timeSeriesKey) {
        JSONObject responseBody = response.getBody().getObject();
        if (!responseBody.has(timeSeriesKey)) {
            throw new IllegalArgumentException("Crypto time series data not found");
        }
        JSONObject timeSeries = responseBody.getJSONObject(timeSeriesKey);
        List<FinancialData> dataBatch = new ArrayList<>();

        for (String dateStr : timeSeries.keySet()) {
            JSONObject dailyData = timeSeries.getJSONObject(dateStr);
            double open = dailyData.getDouble("1. open");
            double high = dailyData.getDouble("2. high");
            double low = dailyData.getDouble("3. low");
            double close = dailyData.getDouble("4. close");
            long volume = dailyData.optLong("5. volume", 0);

            FinancialData data = new FinancialData(symbol, category, LocalDate.parse(dateStr), open, high, low, close, volume);
            dataBatch.add(data);

            if (dataBatch.size() >= BATCH_SIZE) {
                financialDataRepository.saveAll(dataBatch);
                financialDataRepository.flush();
                dataBatch.clear();
            }
        }

        if (!dataBatch.isEmpty()) {
            financialDataRepository.saveAll(dataBatch);
            financialDataRepository.flush();
        }
    }

    private void processCommodityData(HttpResponse<JsonNode> response, String symbol, String category) {
        JSONObject responseBody = response.getBody().getObject();
        List<FinancialData> dataBatch = new ArrayList<>();

        for (Object item : responseBody.getJSONArray("data")) {
            JSONObject dataPoint = (JSONObject) item;
            LocalDate date = LocalDate.parse(dataPoint.getString("date"));
            double value = dataPoint.getDouble("value");

            FinancialData data = new FinancialData(symbol, category, date, value, value, value, value, 0);
            dataBatch.add(data);

            if (dataBatch.size() >= BATCH_SIZE) {
                financialDataRepository.saveAll(dataBatch);
                financialDataRepository.flush();
                dataBatch.clear();
            }
        }

        if (!dataBatch.isEmpty()) {
            financialDataRepository.saveAll(dataBatch);
            financialDataRepository.flush();
        }
    }

    private void processEconomicData(HttpResponse<JsonNode> response, String symbol, String category) {
        JSONObject responseBody = response.getBody().getObject();
        List<FinancialData> dataBatch = new ArrayList<>();

        for (Object item : responseBody.getJSONArray("data")) {
            JSONObject dataPoint = (JSONObject) item;
            LocalDate date = LocalDate.parse(dataPoint.getString("date"));
            double value = dataPoint.getDouble("value");

            FinancialData data = new FinancialData(symbol, category, date, value, value, value, value, 0);
            dataBatch.add(data);

            if (dataBatch.size() >= BATCH_SIZE) {
                financialDataRepository.saveAll(dataBatch);
                financialDataRepository.flush();
                dataBatch.clear();
            }
        }

        if (!dataBatch.isEmpty()) {
            financialDataRepository.saveAll(dataBatch);
            financialDataRepository.flush();
        }
    }
}
