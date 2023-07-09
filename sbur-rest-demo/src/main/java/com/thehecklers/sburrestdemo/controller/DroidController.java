package com.thehecklers.sburrestdemo.controller;

import com.thehecklers.sburrestdemo.config.properties.DroidProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("droid")
public class DroidController {

    private final DroidProperties droidProperties;

    public DroidController(DroidProperties droidProperties) {
        this.droidProperties = droidProperties;
    }

    @GetMapping
    public DroidProperties getDroid() {
        return droidProperties;
    }
}
