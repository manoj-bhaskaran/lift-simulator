package com.liftsimulator.admin.controller;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaForwardingController {
    @RequestMapping(
            value = {
                "/{path:^(?!api|actuator|error|index\\.html).*$}",
                "/{path:^(?!api|actuator|error|index\\.html).*$}/**"
            },
            headers = "Accept=" + MediaType.TEXT_HTML_VALUE
    )
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}
