package com.example.shop.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
public class ExternalApiService {

    private final RestTemplate restTemplate;

    public ExternalApiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 调用外部公开API获取模拟汇率（USD/CNY）
     * 实际生产环境应替换为真实汇率接口
     */
    @SuppressWarnings("unchecked")
    public BigDecimal getExchangeRate() {
        try {
            String url = "https://api.exchangerate-api.com/v4/latest/USD";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("rates")) {
                Map<String, Object> rates = (Map<String, Object>) response.get("rates");
                Object cny = rates.get("CNY");
                if (cny != null) {
                    return new BigDecimal(cny.toString());
                }
            }
        } catch (Exception e) {
            System.err.println("调用外部汇率接口失败，使用默认汇率: " + e.getMessage());
        }
        return new BigDecimal("7.2");
    }

    /**
     * 将人民币价格转换为美元
     */
    public BigDecimal convertToUsd(BigDecimal cnyPrice) {
        BigDecimal rate = getExchangeRate();
        if (rate.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return cnyPrice.divide(rate, 2, RoundingMode.HALF_UP);
    }
}
