package com.example.modernjavainaction.ch13;

import java.util.List;

public class Utils {

    public static void paint(List<Resizable> l) {
        l.forEach(r -> {
            r.setAbsoluteSize(42, 42);
        });
    }

}
