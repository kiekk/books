package com.thehecklers.sburmongo.planefinder.repository

import com.thehecklers.sburmongo.planefinder.domain.Aircraft
import org.springframework.data.repository.CrudRepository

interface AircraftRepository : CrudRepository<Aircraft, String>