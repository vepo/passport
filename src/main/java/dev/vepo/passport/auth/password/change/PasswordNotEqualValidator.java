package dev.vepo.passport.auth.password.change;

import java.util.Objects;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordNotEqualValidator implements ConstraintValidator<PasswordNotEqual, ChangePasswordRequest> {

    @Override
    public boolean isValid(ChangePasswordRequest value, ConstraintValidatorContext context) {
        return Objects.nonNull(value) && !Objects.equals(value.currentPassword(), value.newPassword());
    }
}