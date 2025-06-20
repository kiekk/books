package com.example.recipe232.sequence;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
// @Primary : 타입이 같은 Bean 이 여러 개라도 우선권을 부여하여 의존성 주입
@Primary
public class DatePrefixGenerator implements PrefixGenerator {

    @Override
    public String getPrefix() {
        DateFormat formatter = new SimpleDateFormat("yyyyMMdd");
        return formatter.format(new Date());
    }
}
