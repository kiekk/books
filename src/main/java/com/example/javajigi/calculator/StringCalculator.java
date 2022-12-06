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
            String customDelimiter = m.group(1);
            tokens = m.group(2).split(customDelimiter);
        } else {
            tokens = text.split("[,:]");
        }

        return sum(tokens);
    }

    // 리팩토링 기능별로 메소드로 분리
    // 1. 덧셈 코드 분리
    private int sum(String[] tokens) {
        int sum = 0;
        for (String token : tokens) {
            sum += parseInt(token);
        }
        return sum;
    }

    // 2. 텍스트 -> 숫자 변환 코드 분리
    private int parseInt(String text) {
        int intValue = Integer.parseInt(text);
        if (intValue < 0) {
            throw new RuntimeException("양수만 입력 가능합니다.");
        }
        return intValue;
    }
}
