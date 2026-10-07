package com.hospital.hms.pharmacy.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicineDTO {
    private Long id;
    @NotBlank(message = "Medicine code is required")
    private String code;

    @NotBlank(message = "Medicine name is required")
    private String name;

    @NotBlank(message = "Category is required")
    private String category;

    @NotNull(message = "Stock quantity is required")
    @PositiveOrZero(message = "Stock quantity cannot be negative")
    private Integer stockQuantity;

    @NotNull(message = "Unit price is required")
    @Positive(message = "Unit price must be greater than zero")
    private BigDecimal unitPrice;

    @NotNull(message = "Expiry date is required")
    @Future(message = "Expiry date must be in the future")
    private LocalDate expiryDate;
}
