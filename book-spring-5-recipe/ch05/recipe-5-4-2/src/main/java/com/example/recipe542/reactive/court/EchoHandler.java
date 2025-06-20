package com.example.recipe542.reactive.court;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class EchoHandler {
    @MessageMapping("/echo")
    @SendTo("/topic/echo")
    public String echo(String msg) {
        return "RECEIVED : " + msg;
    }
}
