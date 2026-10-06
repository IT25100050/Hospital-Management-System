package com.hospital.hms.pharmacy.service;

import com.hospital.hms.pharmacy.dto.MedicineDTO;
import java.util.List;

public interface MedicineService {
    MedicineDTO addMedicine(MedicineDTO dto);
    MedicineDTO getMedicineById(Long id);
    List<MedicineDTO> getAllMedicines();
    MedicineDTO updateMedicine(Long id, MedicineDTO dto);
    MedicineDTO updateStockQuantity(Long id, Integer quantityChange);
    void deleteMedicine(Long id);
}
