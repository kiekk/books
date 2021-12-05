package com.example.recipe312.court.service;

import com.example.recipe312.court.domain.Reservation;

import java.util.List;

public interface ReservationService {

    List<Reservation> query(String courtName);

}
