package com.liftsimulator.admin.controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaForwardingController {
    private final ResourceLoader resourceLoader;

    public SpaForwardingController(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @RequestMapping(
            value = {
                "/{path:^(?!api|actuator|error|index\\.html).*$}",
                "/{path:^(?!api|actuator|error|index\\.html).*$}/**"
            },
            headers = "Accept=" + MediaType.TEXT_HTML_VALUE
    )
    public String forwardToIndex() {
        Resource indexHtml = resourceLoader.getResource("classpath:/static/index.html");
        if (!indexHtml.exists()) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "index.html not found. Build the frontend or use the dev server."
            );
        }
        return "forward:/index.html";
    }
}
