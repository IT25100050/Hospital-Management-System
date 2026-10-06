package com.hospital.hms.pharmacy.service;

import com.hospital.hms.common.exception.BadRequestException;
import com.hospital.hms.common.exception.DuplicateResourceException;
import com.hospital.hms.common.exception.ResourceNotFoundException;
import com.hospital.hms.pharmacy.dto.MedicineDTO;
import com.hospital.hms.pharmacy.event.LowStockEvent;
import com.hospital.hms.pharmacy.model.Medicine;
import com.hospital.hms.pharmacy.repository.MedicineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MedicineServiceImpl implements MedicineService {

    private static final int LOW_STOCK_THRESHOLD = 10;

    private final MedicineRepository medicineRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public MedicineDTO addMedicine(MedicineDTO dto) {
        if (medicineRepository.existsByCode(dto.getCode())) {
            throw new DuplicateResourceException("Medicine with code '" + dto.getCode() + "' already exists.");
        }
        Medicine medicine = mapToEntity(dto);
        Medicine saved = medicineRepository.save(medicine);
        return mapToDTO(saved);
    }

    @Override
    public MedicineDTO getMedicineById(Long id) {
        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine not found with id: " + id));
        return mapToDTO(medicine);
    }

    @Override
    public List<MedicineDTO> getAllMedicines() {
        return medicineRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public MedicineDTO updateMedicine(Long id, MedicineDTO dto) {
        Medicine existing = medicineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine not found with id: " + id));

        if (dto.getCode() != null && !dto.getCode().equals(existing.getCode())
                && medicineRepository.existsByCode(dto.getCode())) {
            throw new DuplicateResourceException("Medicine with code '" + dto.getCode() + "' already exists.");
        }

        existing.setName(dto.getName());
        existing.setCode(dto.getCode());
        existing.setCategory(dto.getCategory());
        existing.setStockQuantity(dto.getStockQuantity());
        existing.setUnitPrice(dto.getUnitPrice());
        existing.setExpiryDate(dto.getExpiryDate());

        return mapToDTO(medicineRepository.save(existing));
    }

    @Override
    public MedicineDTO updateStockQuantity(Long id, Integer quantityChange) {
        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine not found with id: " + id));

        int newQuantity = medicine.getStockQuantity() + quantityChange;
        if (newQuantity < 0) {
            throw new BadRequestException("Stock quantity cannot go below zero. Current stock: " + medicine.getStockQuantity());
        }

        medicine.setStockQuantity(newQuantity);
        Medicine saved = medicineRepository.save(medicine);

        if (newQuantity <= LOW_STOCK_THRESHOLD) {
            eventPublisher.publishEvent(new LowStockEvent(this, saved.getId(), saved.getName(), newQuantity));
        }

        return mapToDTO(saved);
    }

    @Override
    public void deleteMedicine(Long id) {
        if (!medicineRepository.existsById(id)) {
            throw new ResourceNotFoundException("Medicine not found with id: " + id);
        }
        medicineRepository.deleteById(id);
    }

    private Medicine mapToEntity(MedicineDTO dto) {
        return Medicine.builder()
                .id(dto.getId())
                .code(dto.getCode())
                .name(dto.getName())
                .category(dto.getCategory())
                .stockQuantity(dto.getStockQuantity())
                .unitPrice(dto.getUnitPrice())
                .expiryDate(dto.getExpiryDate())
                .build();
    }

    private MedicineDTO mapToDTO(Medicine entity) {
        return MedicineDTO.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .category(entity.getCategory())
                .stockQuantity(entity.getStockQuantity())
                .unitPrice(entity.getUnitPrice())
                .expiryDate(entity.getExpiryDate())
                .build();
    }
}
