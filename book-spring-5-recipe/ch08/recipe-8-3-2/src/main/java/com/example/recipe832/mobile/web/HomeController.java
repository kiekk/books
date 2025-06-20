package com.example.recipe832.mobile.web;

import org.springframework.mobile.device.Device;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class HomeController {

    @RequestMapping("/home")
    public String index(Device device) {

        if (device.isMobile()) {
            System.out.println("mobile");
            return "mobile/home";
        } else if (device.isTablet()) {
            System.out.println("tablet");
            return "tablet/home";
        } else {
            System.out.println("else");
            return "home";
        }
    }

}
