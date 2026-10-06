package com.hospital.hms.pharmacy.service;

import com.hospital.hms.common.exception.BadRequestException;
import com.hospital.hms.common.exception.ResourceNotFoundException;
import com.hospital.hms.pharmacy.dto.PrescriptionDTO;
import com.hospital.hms.pharmacy.event.LowStockEvent;
import com.hospital.hms.pharmacy.exception.InsufficientStockException;
import com.hospital.hms.pharmacy.model.Medicine;
import com.hospital.hms.pharmacy.model.Prescription;
import com.hospital.hms.pharmacy.model.PrescriptionItem;
import com.hospital.hms.pharmacy.repository.MedicineRepository;
import com.hospital.hms.pharmacy.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrescriptionServiceImpl implements PrescriptionService {

    private static final int LOW_STOCK_THRESHOLD = 10;

    private final PrescriptionRepository prescriptionRepository;
    private final MedicineRepository medicineRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public PrescriptionDTO createPrescription(PrescriptionDTO dto) {
        Prescription prescription = Prescription.builder()
                .patientId(dto.getPatientId())
                .doctorId(dto.getDoctorId())
                .notes(dto.getNotes())
                .prescribedDate(LocalDateTime.now())
                .build();

        prescription.setItems(buildItems(dto, prescription));

        Prescription saved = prescriptionRepository.save(prescription);
        return mapToDTO(saved);
    }

    @Override
    public PrescriptionDTO getPrescriptionById(Long id) {
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found with id: " + id));
        return mapToDTO(prescription);
    }

    @Override
    public List<PrescriptionDTO> getAllPrescriptions() {
        return prescriptionRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PrescriptionDTO> getPrescriptionsByPatient(Long patientId) {
        return prescriptionRepository.findByPatientId(patientId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PrescriptionDTO> getPrescriptionsByDoctor(Long doctorId) {
        return prescriptionRepository.findByDoctorId(doctorId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PrescriptionDTO updatePrescription(Long id, PrescriptionDTO dto) {
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found with id: " + id));

        if (dto.getNotes() != null) {
            prescription.setNotes(dto.getNotes());
        }
        if (dto.getPatientId() != null) {
            prescription.setPatientId(dto.getPatientId());
        }
        if (dto.getDoctorId() != null) {
            prescription.setDoctorId(dto.getDoctorId());
        }
        if (dto.getItems() != null) {
            prescription.getItems().clear();
            prescription.getItems().addAll(buildItems(dto, prescription));
        }

        Prescription updated = prescriptionRepository.save(prescription);
        return mapToDTO(updated);
    }

    @Override
    public void deletePrescription(Long id) {
        if (!prescriptionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Prescription not found with id: " + id);
        }
        prescriptionRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void fulfillPrescription(Long prescriptionId) {
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found with id: " + prescriptionId));

        if (prescription.getItems() == null || prescription.getItems().isEmpty()) {
            throw new BadRequestException("Prescription has no items to fulfill.");
        }

        for (PrescriptionItem item : prescription.getItems()) {
            Medicine medicine = item.getMedicine();
            if (medicine.getStockQuantity() < item.getQuantity()) {
                throw new InsufficientStockException("Insufficient stock for medicine: " + medicine.getName());
            }
            int remaining = medicine.getStockQuantity() - item.getQuantity();
            medicine.setStockQuantity(remaining);
            medicineRepository.save(medicine);

            if (remaining <= LOW_STOCK_THRESHOLD) {
                eventPublisher.publishEvent(new LowStockEvent(this, medicine.getId(), medicine.getName(), remaining));
            }
        }
    }

    private List<PrescriptionItem> buildItems(PrescriptionDTO dto, Prescription prescription) {
        if (dto.getItems() == null) {
            return new ArrayList<>();
        }
        return dto.getItems().stream().map(itemDto -> {
            Medicine medicine = medicineRepository.findById(itemDto.getMedicineId())
                    .orElseThrow(() -> new ResourceNotFoundException("Medicine not found: " + itemDto.getMedicineId()));

            return PrescriptionItem.builder()
                    .prescription(prescription)
                    .medicine(medicine)
                    .quantity(itemDto.getQuantity())
                    .dosage(itemDto.getDosage())
                    .build();
        }).collect(Collectors.toList());
    }

    private PrescriptionDTO mapToDTO(Prescription entity) {
        List<PrescriptionDTO.PrescriptionItemDTO> itemDTOs = entity.getItems().stream()
                .map(item -> PrescriptionDTO.PrescriptionItemDTO.builder()
                        .id(item.getId())
                        .medicineId(item.getMedicine().getId())
                        .medicineName(item.getMedicine().getName())
                        .quantity(item.getQuantity())
                        .dosage(item.getDosage())
                        .build())
                .collect(Collectors.toList());

        return PrescriptionDTO.builder()
                .id(entity.getId())
                .patientId(entity.getPatientId())
                .doctorId(entity.getDoctorId())
                .notes(entity.getNotes())
                .prescribedDate(entity.getPrescribedDate())
                .items(itemDTOs)
                .build();
    }
}
