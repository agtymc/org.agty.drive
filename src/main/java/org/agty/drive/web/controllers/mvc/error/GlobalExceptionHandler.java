package org.agty.drive.web.controllers.mvc.error;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.tomcat.util.http.InvalidParameterException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@ControllerAdvice(annotations = Controller.class)
public class GlobalExceptionHandler {

    @ExceptionHandler({
            MaxUploadSizeExceededException.class,
            MultipartException.class,
            InvalidParameterException.class
    })
    public Object handleUploadError(Exception exception, HttpServletRequest request, HttpServletResponse response, Model model) {
        if (isAjaxRequest(request)) {
            response.setStatus(413);
            return ResponseEntity.status(413).body(Map.of(
                    "error", "Файл слишком большой. Уменьшите размер файла или увеличьте лимит загрузки."
            ));
        }
        if (shouldSkipHtmlError(request, response, 413)) {
            return new ModelAndView();
        }
        fillModel(model, 413,
                "Файл слишком большой",
                "Размер загружаемого файла превышает допустимый лимит. Уменьшите файл или увеличьте лимит в [upload].",
                request);
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public Object handleAnyError(Exception exception, HttpServletRequest request, HttpServletResponse response, Model model) {
        if (isAjaxRequest(request)) {
            response.setStatus(500);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Во время обработки запроса произошла ошибка. Подробности смотрите в логе приложения."
            ));
        }
        if (shouldSkipHtmlError(request, response, 500)) {
            return new ModelAndView();
        }
        fillModel(model, 500,
                "Внутренняя ошибка",
                "Во время обработки запроса произошла ошибка. Попробуйте повторить действие позже.",
                request);
        return "error";
    }

    private void fillModel(Model model, int statusCode, String title, String message, HttpServletRequest request) {
        model.addAttribute("pageTitle", "Ошибка");
        model.addAttribute("statusCode", statusCode);
        model.addAttribute("errorTitle", title);
        model.addAttribute("errorMessage", message);
        Object requestPath = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        model.addAttribute("requestPath", requestPath != null ? requestPath : request.getRequestURI());
    }

    private boolean shouldSkipHtmlError(HttpServletRequest request, HttpServletResponse response, int statusCode) {
        if (response.isCommitted()) {
            return true;
        }

        String uri = request == null ? null : request.getRequestURI();
        if (uri == null || uri.isBlank()) {
            return false;
        }

        boolean mediaRequest = uri.contains("/content")
                || uri.contains("/thumbnail")
                || uri.contains("/download")
                || uri.contains("/preview-");
        if (mediaRequest) {
            response.setStatus(statusCode);
            return true;
        }
        return false;
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        return request != null && "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"));
    }
}
