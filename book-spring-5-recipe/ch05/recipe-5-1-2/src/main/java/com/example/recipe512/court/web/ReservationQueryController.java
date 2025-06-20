package com.example.recipe512.court.web;

import com.example.recipe512.court.Delayer;
import com.example.recipe512.court.domain.Reservation;
import com.example.recipe512.court.service.ReservationService;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;


@Controller
@RequestMapping("/reservationQuery")
public class ReservationQueryController {

    private final ReservationService reservationService;
    private final TaskExecutor taskExecutor;

    public ReservationQueryController(ReservationService reservationService, AsyncTaskExecutor taskExecutor) {
        this.reservationService = reservationService;
        this.taskExecutor = taskExecutor;
    }

    @GetMapping
    public void setupForm() {}


    // Callable 대신 DeferredResult 사용 가능
    // TaskExecutor 인스턴스를 만들어 Runnable(비동기 처리 작업)을 전송한 후
    // 작업 내부에서 setResult() 메서드를 이용해 DeferredResult 결과값을 설정
    // 예외가 발생할 경우 DeferredResult.setErrorResult() 메서드의 인수로 보내 처리
    @PostMapping
    public DeferredResult<String> submitForm(@RequestParam("courtName") String courtName, Model model) {
        final DeferredResult<String> result = new DeferredResult<>();

        taskExecutor.execute(() -> {
            List<Reservation> reservations = java.util.Collections.emptyList();

            if (courtName != null) {
                Delayer.randomDelay();
                reservations = reservationService.query(courtName);
            }

            model.addAttribute("reservations", reservations);

            result.setResult("reservationQuery");
        });

        return result;

    }
}