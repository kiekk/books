package com.bookstore.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

// autoApply = true는 모든 Boolean 속성에 적용됨, @Entity에 @Convert를 적용하지 않아도 동작
// 따라서 특정 속성에만 적용하고 싶다면 autoApply = false로 설정해야 함 (default: false)
// ** @Enumerated에는 AttributeConverter를 적용할 수 없음
//@Converter(autoApply = true)
@Converter
public class BooleanConverter implements AttributeConverter<Boolean, String> {
    @Override
    public String convertToDatabaseColumn(Boolean attr) {
        return attr == null ? "No" : "Yes";
    }

    @Override
    public Boolean convertToEntityAttribute(String dbData) {
        return !"No".equals(dbData);
    }
}
