package com.finflow.dto;

import com.finflow.entity.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateBankAccountRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull AccountType accountType,
        @NotBlank @Pattern(regexp = "[A-Za-z]{3}", message = "Currency must be a three-letter ISO code") String currency,
        @NotNull @DecimalMin(value = "0.00") @Digits(integer = 15, fraction = 4) BigDecimal initialBalance) {
}
