package com.ridematch.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Safe destination for Spring Security's access-denied page. */
@Controller
public class ErrorPageController {

    @GetMapping("/error/403")
    public String forbidden() {
        return "error/403";
    }
}
