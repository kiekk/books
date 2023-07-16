package com.thehecklers.sburjpa.planefinder.domain.aircraft;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Aircraft {

    @Id
    @GeneratedValue
    private Long id;

    private String callsign;
    private String squawk;
    private String reg;
    private String flightno;
    private String route;
    private String type;
    private String category;

    private int altitude;
    private int heading;
    private int speed;

    @JsonProperty("vert_rate")
    private int vertRate;
    @JsonProperty("selected_altitude")
    private int selectedAltitude;

    private double lat;
    private double lon;
    private double barometer;

    @JsonProperty("polar_distance")
    private double polarDistance;
    @JsonProperty("polar_bearing")
    private double polarBearing;

    @JsonProperty("is_adsb")
    private boolean isADSB;
    @JsonProperty("is_on_ground")
    private boolean isOnGround;

    @JsonProperty("last_seen_time")
    public LocalDateTime lastSeenTime;
    @JsonProperty("post_update_time")
    public LocalDateTime posUpdateTime;
    @JsonProperty("bds40_seen_time")
    public LocalDateTime bds40SeenTime;
}

