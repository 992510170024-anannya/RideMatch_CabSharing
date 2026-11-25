package com.ridematch.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletResponse;

/** Converts expected application failures into safe, centralized error pages. */
@ControllerAdvice(annotations = Controller.class)
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApplicationException.class)
    public ModelAndView handleApplicationException(ApplicationException exception,
            HttpServletResponse response, Model model) {
        response.setStatus(exception.getStatus().value());
        model.addAttribute("status", exception.getStatus().value());
        model.addAttribute("errorMessage", exception.getMessage());
        return new ModelAndView("error/error", model.asMap());
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleUnexpectedException(Exception exception,
            HttpServletResponse response, Model model) {
        LOGGER.error("Unexpected request failure", exception);
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        model.addAttribute("status", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        model.addAttribute("errorMessage", "Something went wrong. Please try again.");
        return new ModelAndView("error/error", model.asMap());
    }
}
