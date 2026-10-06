package com.hospital.hms.billing.dto;

import com.hospital.hms.common.enums.PaymentStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillDTO {
    private Long id;
    private String invoiceNumber;

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    private Long appointmentId;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private PaymentStatus status;
    private LocalDateTime createdDate;

    @NotEmpty(message = "At least one bill item is required")
    @Valid
    private List<BillItemDTO> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BillItemDTO {
        private Long id;

        @NotNull(message = "Description is required")
        private String description;

        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be greater than zero")
        private Integer quantity;

        @NotNull(message = "Unit price is required")
        @PositiveOrZero(message = "Unit price cannot be negative")
        private BigDecimal unitPrice;

        private BigDecimal amount;
    }
}
