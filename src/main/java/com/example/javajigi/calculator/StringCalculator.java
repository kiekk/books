package com.example.javajigi.calculator;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringCalculator {

    // 리팩토링 기능별로 메소드로 분리
    // 1. 덧셈 코드 분리
    // 2. 텍스트 -> 숫자 변환 코드 분리
    // 3. null, empty 체크 로직 분리
    // 4. split 코드 분리
    // 덧셈 메소드 안에 parseInt 로직 분리
    int add(String text) {
        if (nullOrEmpty(text)) {
            return 0;
        }

        return sum(toInts(split(text)));
    }

    private String[] split(String text) {
        Matcher m = Pattern.compile("//(.)\n(.*)").matcher(text);
        if (m.find()) {
            String customDelimiter = m.group(1);
            return m.group(2).split(customDelimiter);
        }
        return text.split("[,:]");
    }

    private boolean nullOrEmpty(String text) {
        return text == null || text.isEmpty();
    }

    private int sum(int[] numbers) {
        int sum = 0;
        for (int number : numbers) {
            sum += number;
        }
        return sum;
    }

    private int parseInt(String text) {
        int intValue = Integer.parseInt(text);
        if (intValue < 0) {
            throw new RuntimeException("양수만 입력 가능합니다.");
        }
        return intValue;
    }

    private int[] toInts(String[] tokens) {
        int tokenLength = tokens.length;
        int[] numbers = new int[tokenLength];
        for (int i = 0; i < tokenLength; i++) {
            numbers[i] = parseInt(tokens[i]);
        }
        return numbers;
    }
}
