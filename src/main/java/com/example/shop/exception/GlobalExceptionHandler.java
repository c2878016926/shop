package com.example.shop.exception;

import com.example.shop.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 区分 API 请求（返回 JSON）和 Web 请求（返回错误页面）
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 判断是否为 API 请求（路径以 /api/ 开头）
     */
    private boolean isApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/api/");
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public String handle404(NoHandlerFoundException e, Model model) {
        model.addAttribute("error", "页面不存在");
        return "error";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Object handleIllegalArgument(IllegalArgumentException e, HttpServletRequest request, Model model) {
        if (isApiRequest(request)) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
        model.addAttribute("error", e.getMessage());
        return "error";
    }

    /**
     * 处理 @Valid 校验异常，返回字段级错误信息
     * 体现协议设计的合理性
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleValidation(MethodArgumentNotValidException e, HttpServletRequest request, Model model) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        if (isApiRequest(request)) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, message));
        }
        model.addAttribute("error", message);
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public Object handleException(Exception e, HttpServletRequest request, Model model) {
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "系统繁忙，请稍后重试"));
        }
        model.addAttribute("error", "系统繁忙，请稍后重试");
        model.addAttribute("detail", e.getMessage());
        return "error";
    }
}
