package com.example.recipe363.court.service;


import com.example.recipe363.court.domain.PeriodicReservation;
import com.example.recipe363.court.domain.Reservation;
import com.example.recipe363.court.domain.SportType;

import java.time.LocalDate;
import java.util.List;

public interface ReservationService {

    List<Reservation> query(String courtName);

    void make(Reservation reservation)
            throws ReservationNotAvailableException;

    List<SportType> getAllSportTypes();

    SportType getSportType(int sportTypeId);

    List<Reservation> findByDate(LocalDate summaryDate);

    void makePeriodic(PeriodicReservation periodicReservation)
            throws ReservationNotAvailableException;
}
