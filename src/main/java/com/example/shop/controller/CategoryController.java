package com.example.shop.controller;

import com.example.shop.entity.Category;
import com.example.shop.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * 分类管理 Web 控制器
 * 提供分类列表、新增、编辑、删除等页面操作
 */
@Controller
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/categories")
    public String categoryList(Model model) {
        List<Category> categories = categoryService.findAll();
        model.addAttribute("categories", categories);
        model.addAttribute("category", new Category());
        return "categories";
    }

    @PostMapping("/categories/save")
    public String save(@Valid Category category, BindingResult result,
                       Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.findAll());
            return "categories";
        }
        categoryService.save(category);
        redirectAttributes.addFlashAttribute("msg", "分类保存成功！");
        return "redirect:/categories";
    }

    @PostMapping("/categories/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.deleteById(id);
            redirectAttributes.addFlashAttribute("msg", "分类删除成功！");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "删除失败：该分类下还有商品，无法删除");
        }
        return "redirect:/categories";
    }
}
