package com.thehecklers.sburjpa.planefinder.loader;

import com.thehecklers.sburjpa.planefinder.domain.aircraft.Aircraft;
import com.thehecklers.sburjpa.planefinder.repository.aircraft.AircraftRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataLoader {

    private final AircraftRepository repository;

    @PostConstruct
    private void loadData() {
        repository.save(new Aircraft(82L,
                "AAL608", "1451", "N754UW", "AA608", "IND-PHX", "A319", "A3",
                36000, 255, 423, 0, 36000,
                39.150284, -90.684795, 1012.8, 26.575562, 295.501994,
                true, false,
                LocalDateTime.parse("2020-11-27T21:29:35"),
                LocalDateTime.parse("2020-11-27T21:29:34"),
                LocalDateTime.parse("2020-11-27T21:29:27")));
    }
}
