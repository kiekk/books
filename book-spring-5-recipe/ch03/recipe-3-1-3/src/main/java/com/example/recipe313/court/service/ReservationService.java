package com.example.recipe313.court.service;

import com.example.recipe313.court.domain.Reservation;

import java.util.List;

public interface ReservationService {

    List<Reservation> query(String courtName);

}
