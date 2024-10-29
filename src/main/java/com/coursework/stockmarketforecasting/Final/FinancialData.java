package com.coursework.stockmarketforecasting.Final;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
public class FinancialData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;
    private String category;
    private LocalDate date;
    private double openPrice;
    private double closePrice;
    private double highPrice;
    private double lowPrice;
    private long volume;
}
