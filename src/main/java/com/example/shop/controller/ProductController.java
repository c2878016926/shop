package com.example.shop.controller;

import com.example.shop.dto.ProductStats;
import com.example.shop.entity.Product;
import com.example.shop.service.AsyncTaskService;
import com.example.shop.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Controller
public class ProductController {

    private final ProductService productService;
    private final AsyncTaskService asyncTaskService;

    public ProductController(ProductService productService, AsyncTaskService asyncTaskService) {
        this.productService = productService;
        this.asyncTaskService = asyncTaskService;
    }

    @GetMapping("/")
    public String index(@RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        Model model) {
        Page<Product> productPage = productService.findPage(page, size);
        model.addAttribute("productPage", productPage);
        return "index";
    }

    @GetMapping("/search")
    public String search(@RequestParam(required = false) String name,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "10") int size,
                         Model model) {
        Page<Product> productPage = productService.searchPage(name, page, size);
        model.addAttribute("productPage", productPage);
        model.addAttribute("keyword", name);
        return "index";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("product", new Product());
        return "form";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        try {
            Product product = productService.findById(id);
            model.addAttribute("product", product);
            return "form";
        } catch (IllegalArgumentException e) {
            return "redirect:/";
        }
    }

    @PostMapping("/save")
    public String save(@Valid Product product, BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("product", product);
            return "form";
        }
        try {
            if (product.getId() != null) {
                // 编辑模式：保留数据库中不可编辑的字段（如分类）
                try {
                    Product existing = productService.findById(product.getId());
                    product.setCategory(existing.getCategory());
                } catch (IllegalArgumentException e) {
                    // 商品不存在，当作新增处理
                    product.setId(null);
                }
                productService.save(product);
            } else {
                // 新增模式：自动生成ID
                productService.save(product);
            }
            redirectAttributes.addFlashAttribute("msg", "保存成功！");
        } catch (IllegalArgumentException e) {
            model.addAttribute("product", product);
            model.addAttribute("errorMsg", e.getMessage());
            return "form";
        }
        return "redirect:/";
    }

    /**
     * 编辑模式下保存修改（包括修改ID）
     */
    @PostMapping("/save-edit")
    public String saveEdit(@RequestParam Long originalId,
                           @RequestParam(required = false) Long newId,
                           @Valid Product product, BindingResult result,
                           Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            product.setId(originalId);
            model.addAttribute("product", product);
            model.addAttribute("newId", newId);
            return "form";
        }
        try {
            // 先保留分类关联
            Product existing = productService.findById(originalId);
            product.setCategory(existing.getCategory());
            product.setId(originalId);
            productService.save(product);
            // 如果指定了新ID且不等于原ID，修改ID
            if (newId != null && !newId.equals(originalId)) {
                productService.updateProductId(originalId, newId);
                redirectAttributes.addFlashAttribute("msg", "保存成功！ID已从 " + originalId + " 改为 " + newId);
            } else {
                redirectAttributes.addFlashAttribute("msg", "保存成功！");
            }
        } catch (IllegalArgumentException e) {
            product.setId(originalId);
            model.addAttribute("product", product);
            model.addAttribute("newId", newId);
            model.addAttribute("errorMsg", e.getMessage());
            return "form";
        }
        return "redirect:/";
    }

    /**
     * 使用自定义ID保存商品
     */
    @PostMapping("/save-with-id")
    public String saveWithCustomId(@RequestParam Long customId,
                                   @Valid Product product, BindingResult result,
                                   Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            product.setId(null); // 清除自动绑定的id，用customId
            model.addAttribute("product", product);
            model.addAttribute("customId", customId);
            return "form";
        }
        try {
            product.setId(customId);
            productService.saveWithCustomId(product);
            redirectAttributes.addFlashAttribute("msg", "保存成功！自定义ID: " + customId);
        } catch (IllegalArgumentException e) {
            model.addAttribute("product", product);
            model.addAttribute("customId", customId);
            model.addAttribute("errorMsg", e.getMessage());
            return "form";
        }
        return "redirect:/";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        productService.deleteById(id);
        redirectAttributes.addFlashAttribute("msg", "删除成功！");
        return "redirect:/";
    }

    @PostMapping("/delete-all")
    public String deleteAll(RedirectAttributes redirectAttributes) {
        productService.deleteAllAndResetId();
        redirectAttributes.addFlashAttribute("msg", "已清空所有商品，ID已重置！");
        return "redirect:/";
    }

    @GetMapping("/stats")
    public String stats(Model model) {
        Map<String, Object> jdbcStats = productService.getStockStatsByJdbc();
        model.addAttribute("jdbcStats", jdbcStats);
        return "stats";
    }

    @GetMapping("/stats/async")
    @ResponseBody
    public CompletableFuture<ProductStats> statsAsync() {
        return asyncTaskService.calculateStatsAsync();
    }
}
