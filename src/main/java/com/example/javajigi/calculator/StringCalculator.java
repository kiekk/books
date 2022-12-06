package com.example.javajigi.calculator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringCalculator {

    int add(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        Matcher m = Pattern.compile("//(.)\n(.*)").matcher(text);

        String[] tokens;

        if (m.find()) {
            String customDelimeter = m.group(1);
            tokens = m.group(2).split(customDelimeter);
        } else {
            tokens = text.split("[,:]");
        }

        int sum = 0;
        for (String token : tokens) {
            int tokenToInt = Integer.parseInt(token);
            if (tokenToInt < 0) {
                throw new RuntimeException("정수만 입력 가능합니다.");
            }
            sum += tokenToInt;
        }
        return sum;
    }
}
