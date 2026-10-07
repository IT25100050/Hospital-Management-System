package com.hospital.hms.billing.repository;

import com.hospital.hms.billing.model.Bill;
import com.hospital.hms.common.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {
    Optional<Bill> findByInvoiceNumber(String invoiceNumber);
    List<Bill> findByPatientId(Long patientId);
    List<Bill> findByStatus(PaymentStatus status);
}
