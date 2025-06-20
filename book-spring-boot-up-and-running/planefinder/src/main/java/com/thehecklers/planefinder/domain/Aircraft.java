package com.thehecklers.planefinder.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Aircraft {
    @Id
    private Long id;
    private String callsign, squawk, reg, flightno, route, type, category;

    private int altitude, heading, speed;
    @JsonProperty("vert_rate")
    private int vertRate;
    @JsonProperty("selected_altitude")
    private int selectedAltitude;

    private double lat, lon, barometer;
    @JsonProperty("polar_distance")
    private double polarDistance;
    @JsonProperty("polar_bearing")
    private double polarBearing;

    @JsonProperty("is_adsb")
    private boolean isADSB;
    @JsonProperty("is_on_ground")
    private boolean isOnGround;

    @JsonProperty("last_seen_time")
    private LocalDateTime lastSeenTime;
    @JsonProperty("pos_update_time")
    private LocalDateTime posUpdateTime;
    @JsonProperty("bds40_seen_time")
    private LocalDateTime bds40SeenTime;

    public Aircraft(String callsign, String reg, String flightno, String type,
                    int altitude, int heading, int speed,
                    double lat, double lon) {

        this(null, callsign, "sqwk", reg, flightno, "route", type, "ct",
                altitude, heading, speed, 0, 0,
                lat, lon, 0D, 0D, 0D,
                false, true,
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
    }
}