package com.example.shop.controller;

import com.example.shop.entity.Product;
import com.example.shop.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = new Product("API测试商品", "API测试描述", new BigDecimal("199.99"), 50);
        testProduct = productRepository.save(testProduct);
    }

    @Test
    void testListProducts() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNotEmpty());
    }

    @Test
    void testGetById() throws Exception {
        mockMvc.perform(get("/api/products/" + testProduct.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("API测试商品"));
    }

    @Test
    void testGetByIdNotFound() throws Exception {
        mockMvc.perform(get("/api/products/99999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void testCreateProduct() throws Exception {
        String json = "{\"name\":\"新商品\",\"price\":59.99,\"stock\":30}";
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.name").value("新商品"));
    }

    @Test
    void testCreateProductValidationFail() throws Exception {
        // 名称为空，应触发校验异常
        String json = "{\"name\":\"\",\"price\":59.99,\"stock\":30}";
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void testUpdateProduct() throws Exception {
        String json = "{\"name\":\"更新商品\",\"price\":299.99,\"stock\":10}";
        mockMvc.perform(put("/api/products/" + testProduct.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("更新商品"));
    }

    @Test
    void testDeleteProduct() throws Exception {
        mockMvc.perform(delete("/api/products/" + testProduct.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    void testPlaceOrder() throws Exception {
        mockMvc.perform(post("/api/products/" + testProduct.getId() + "/order")
                        .param("quantity", "5"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.quantity").value(5));
    }

    @Test
    void testPlaceOrderInsufficientStock() throws Exception {
        mockMvc.perform(post("/api/products/" + testProduct.getId() + "/order")
                        .param("quantity", "999"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void testSalesRanking() throws Exception {
        mockMvc.perform(get("/api/products/sales-ranking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testWithCategory() throws Exception {
        mockMvc.perform(get("/api/products/with-category"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
