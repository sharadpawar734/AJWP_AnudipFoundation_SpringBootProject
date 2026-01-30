package com.Anudip.FinalProject.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ModelAndView handleException(Exception ex) {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("Orders"); // Redirect to Orders page
        modelAndView.addObject("error", "An error occurred while loading orders. Please try again.");
        modelAndView.addObject("orders", java.util.Collections.emptyList());
        
        System.err.println("Error loading orders: " + ex.getMessage());
        ex.printStackTrace();
        
        return modelAndView;
    }
}

