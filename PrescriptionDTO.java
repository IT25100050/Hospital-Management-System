package com.hospital.hms.pharmacy.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionDTO {
    private Long id;
    @NotNull(message = "Patient ID is required")
    private Long patientId;

    @NotNull(message = "Doctor ID is required")
    private Long doctorId;
    private String notes;
    private LocalDateTime prescribedDate;
    @NotEmpty(message = "At least one medicine item is required")
    @Valid
    private List<PrescriptionItemDTO> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PrescriptionItemDTO {
        private Long id;
        @NotNull(message = "Medicine ID is required")
        private Long medicineId;
        private String medicineName;
        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be greater than zero")
        private Integer quantity;
        @NotBlank(message = "Dosage is required")
        private String dosage;
    }
}
