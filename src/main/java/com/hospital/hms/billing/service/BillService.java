package com.hospital.hms.billing.service;

import com.hospital.hms.billing.dto.BillDTO;

import java.util.List;

public interface BillService {
    BillDTO createBill(BillDTO dto);
    BillDTO getBillById(Long id);
    BillDTO getBillByInvoiceNumber(String invoiceNumber);
    List<BillDTO> getAllBills();
    List<BillDTO> getBillsByPatientId(Long patientId);
    BillDTO updateBill(Long id, BillDTO dto);
    void deleteBill(Long id);
}
