package com.musicreview.server.global.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.Pattern;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Spotify 리소스 id: 영문·숫자 1~64자. null은 통과 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@ReportAsSingleViolation
@Pattern(regexp = SpotifyId.REGEX)
public @interface SpotifyId {

    String REGEX = "^[A-Za-z0-9]{1,64}$";

    String message() default "Spotify id는 영문과 숫자 1~64자입니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
