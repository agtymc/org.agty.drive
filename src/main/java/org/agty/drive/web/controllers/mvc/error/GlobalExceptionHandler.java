package org.agty.drive.web.controllers.mvc.error;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.tomcat.util.http.InvalidParameterException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

@ControllerAdvice(annotations = Controller.class)
public class GlobalExceptionHandler {

    @ExceptionHandler({
            MaxUploadSizeExceededException.class,
            MultipartException.class,
            InvalidParameterException.class
    })
    public String handleUploadError(Exception exception, HttpServletRequest request, Model model) {
        fillModel(model, 413,
                "Файл слишком большой",
                "Размер загружаемого файла превышает допустимый лимит. Уменьшите файл или увеличьте лимит в [upload].",
                request);
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public String handleAnyError(Exception exception, HttpServletRequest request, Model model) {
        fillModel(model, 500,
                "Внутренняя ошибка",
                "Во время обработки запроса произошла ошибка. Попробуйте повторить действие позже.",
                request);
        return "error";
    }

    private void fillModel(Model model, int statusCode, String title, String message, HttpServletRequest request) {
        model.addAttribute("title", "AGTY/DRIVE Error");
        model.addAttribute("statusCode", statusCode);
        model.addAttribute("errorTitle", title);
        model.addAttribute("errorMessage", message);
        model.addAttribute("requestPath", request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI));
    }
}
