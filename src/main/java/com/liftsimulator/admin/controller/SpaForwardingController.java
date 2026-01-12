package com.liftsimulator.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaForwardingController {
    @RequestMapping(
            value = {
                "/{path:^(?!api|actuator|error).*$}",
                "/{path:^(?!api|actuator|error).*$}/**/{path:[^\\.]*}"
            }
    )
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}
