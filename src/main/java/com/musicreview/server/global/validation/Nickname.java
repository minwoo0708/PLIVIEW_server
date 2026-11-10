package com.musicreview.server.global.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 앞뒤 공백을 뺀 길이가 2~20자. null은 통과(필수 여부는 @NotNull로 따로 지정) */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = Nickname.Validator.class)
public @interface Nickname {

    int MIN = 2;
    int MAX = 20;

    String message() default "닉네임은 앞뒤 공백을 빼고 2~20자로 입력해 주세요.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<Nickname, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null) {
                return true;
            }
            int length = value.strip().length();
            return length >= MIN && length <= MAX;
        }
    }
}
