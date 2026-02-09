package dev.vepo.passport.auth.password.change;

import static jakarta.validation.constraintvalidation.ValidationTarget.ANNOTATED_ELEMENT;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraintvalidation.SupportedValidationTarget;

@Documented
@Constraint(validatedBy = PasswordNotEqualValidator.class)
@Target(TYPE_USE)
@Retention(RUNTIME)
@SupportedValidationTarget(ANNOTATED_ELEMENT)
@ReportAsSingleViolation
public @interface PasswordNotEqual {
    String message() default "{dev.vepo.passport.auth.password.change.PasswordNotEqual.message}";

    Class<? extends Payload>[] payload() default {};

    Class<?>[] groups() default {};
}
