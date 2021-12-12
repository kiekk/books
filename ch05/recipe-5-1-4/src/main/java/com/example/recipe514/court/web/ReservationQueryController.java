package com.example.recipe514.court.web;

import com.example.recipe514.court.Delayer;
import com.example.recipe514.court.domain.Reservation;
import com.example.recipe514.court.service.ReservationService;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;


@Controller
@RequestMapping("/reservationQuery")
public class ReservationQueryController {

    private final ReservationService reservationService;
    private final AsyncListenableTaskExecutor taskExecutor;

    public ReservationQueryController(ReservationService reservationService, AsyncListenableTaskExecutor taskExecutor) {
        this.reservationService = reservationService;
        this.taskExecutor = taskExecutor;
    }

    @GetMapping
    public void setupForm() {}

    @PostMapping
    public ListenableFuture<String> submitForm(@RequestParam("courtName") String courtName, Model model) {

        // 작업 성공시 DeferredResult.setResult() 호출
        // 작업 실패시 DeferredResult.setErrorResult() 호출
        return taskExecutor.submitListenable(() -> {
            List<Reservation> reservations = java.util.Collections.emptyList();

            if (courtName != null) {
                Delayer.randomDelay();
                reservations = reservationService.query(courtName);
            }

            model.addAttribute("reservations", reservations);

            return "reservationQuery";
        });
    }
}