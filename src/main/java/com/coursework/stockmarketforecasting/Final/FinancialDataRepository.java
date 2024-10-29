package com.coursework.stockmarketforecasting.Final;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FinancialDataRepository extends JpaRepository<FinancialData, Long> {
    List<FinancialData> findBySymbolAndCategory(String symbol, String category);
}
