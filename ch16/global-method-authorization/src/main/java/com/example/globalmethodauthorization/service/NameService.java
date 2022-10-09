package com.example.globalmethodauthorization.service;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class NameService {

    @PreAuthorize("hasAnyAuthority('write')")
    public String getName() {
        return "Fantastic";
    }

}
