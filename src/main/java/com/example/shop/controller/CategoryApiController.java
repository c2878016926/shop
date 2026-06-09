package com.example.shop.controller;

import com.example.shop.dto.ApiResponse;
import com.example.shop.entity.Category;
import com.example.shop.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
public class CategoryApiController {

    private final CategoryService categoryService;

    public CategoryApiController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ApiResponse<List<Category>> list() {
        return ApiResponse.success(categoryService.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<Category> getById(@PathVariable Long id) {
        return ApiResponse.success(categoryService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Category> create(@Valid @RequestBody Category category) {
        return ApiResponse.success(201, "创建成功", categoryService.save(category));
    }

    @PutMapping("/{id}")
    public ApiResponse<Category> update(@PathVariable Long id, @Valid @RequestBody Category category) {
        categoryService.findById(id);
        category.setId(id);
        return ApiResponse.success("更新成功", categoryService.save(category));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> delete(@PathVariable Long id) {
        categoryService.deleteById(id);
        return ApiResponse.success("删除成功", null);
    }

    /**
     * 每个分类的商品数量统计（JdbcTemplate LEFT JOIN + GROUP BY）
     */
    @GetMapping("/product-count")
    public ApiResponse<List<Map<String, Object>>> productCountByCategory() {
        return ApiResponse.success(categoryService.getProductCountByCategory());
    }

    /**
     * 指定分类的价格统计（JdbcTemplate 聚合函数）
     */
    @GetMapping("/{id}/price-stats")
    public ApiResponse<Map<String, Object>> categoryPriceStats(@PathVariable Long id) {
        return ApiResponse.success(categoryService.getCategoryPriceStats(id));
    }
}
