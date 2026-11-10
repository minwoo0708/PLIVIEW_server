package com.musicreview.server.global.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigDecimal;

/** 별점·항목 점수: 0.5 ~ 5.0, 0.5 단위. null은 통과 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = Score.Validator.class)
public @interface Score {

    String message() default "0.5부터 5.0까지 0.5 단위로 입력해야 합니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<Score, BigDecimal> {

        private static final BigDecimal MIN = new BigDecimal("0.5");
        private static final BigDecimal MAX = new BigDecimal("5.0");
        private static final BigDecimal TWO = BigDecimal.valueOf(2);

        @Override
        public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
            if (value == null) {
                return true;
            }
            if (value.compareTo(MIN) < 0 || value.compareTo(MAX) > 0) {
                return false;
            }
            // 2를 곱해서 정수면 0.5 단위
            return value.multiply(TWO).stripTrailingZeros().scale() <= 0;
        }
    }
}
