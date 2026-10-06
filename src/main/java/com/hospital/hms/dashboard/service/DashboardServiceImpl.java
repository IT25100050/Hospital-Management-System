package com.hospital.hms.dashboard.service;

import com.hospital.hms.appointment.repository.AppointmentRepository;
import com.hospital.hms.billing.model.Bill;
import com.hospital.hms.billing.repository.BillRepository;
import com.hospital.hms.common.enums.PaymentStatus;
import com.hospital.hms.dashboard.dto.DashboardSummaryDTO;
import com.hospital.hms.doctorrecords.repository.DoctorRepository;
import com.hospital.hms.patient.repository.PatientRepository;
import com.hospital.hms.pharmacy.repository.MedicineRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final int LOW_STOCK_THRESHOLD = 10;

    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final BillRepository billRepository;
    private final MedicineRepository medicineRepository;

    public DashboardServiceImpl(PatientRepository patientRepository, DoctorRepository doctorRepository,
                                 AppointmentRepository appointmentRepository, BillRepository billRepository,
                                 MedicineRepository medicineRepository) {
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.billRepository = billRepository;
        this.medicineRepository = medicineRepository;
    }

    @Override
    public DashboardSummaryDTO getSummary() {
        long totalPatients = patientRepository.count();
        long totalDoctors = doctorRepository.count();

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfTomorrow = startOfToday.plusDays(1);
        long todayAppointments = appointmentRepository.countByAppointmentTimeBetween(startOfToday, startOfTomorrow);
        long totalAppointments = appointmentRepository.count();

        List<Bill> allBills = billRepository.findAll();
        long totalBills = allBills.size();

        BigDecimal totalRevenue = allBills.stream()
                .map(Bill::getPaidAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal outstandingAmount = allBills.stream()
                .filter(b -> b.getStatus() != PaymentStatus.PAID && b.getStatus() != PaymentStatus.REFUNDED)
                .map(b -> b.getTotalAmount().subtract(b.getPaidAmount() != null ? b.getPaidAmount() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long lowStockMedicineCount = medicineRepository.findAll().stream()
                .filter(m -> m.getStockQuantity() != null && m.getStockQuantity() <= LOW_STOCK_THRESHOLD)
                .count();

        return new DashboardSummaryDTO(
                totalPatients, totalDoctors, todayAppointments, totalAppointments,
                totalBills, lowStockMedicineCount, totalRevenue, outstandingAmount
        );
    }
}
