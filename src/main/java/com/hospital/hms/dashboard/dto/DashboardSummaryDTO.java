package com.hospital.hms.dashboard.dto;

import java.math.BigDecimal;

public class DashboardSummaryDTO {
    private long totalPatients;
    private long totalDoctors;
    private long todayAppointments;
    private long totalAppointments;
    private long totalBills;
    private long lowStockMedicineCount;
    private BigDecimal totalRevenue;      // sum of all payments actually collected
    private BigDecimal outstandingAmount; // sum of (totalAmount - paidAmount) across unpaid/partial bills

    public DashboardSummaryDTO() {}

    public DashboardSummaryDTO(long totalPatients, long totalDoctors, long todayAppointments,
                                long totalAppointments, long totalBills, long lowStockMedicineCount,
                                BigDecimal totalRevenue, BigDecimal outstandingAmount) {
        this.totalPatients = totalPatients;
        this.totalDoctors = totalDoctors;
        this.todayAppointments = todayAppointments;
        this.totalAppointments = totalAppointments;
        this.totalBills = totalBills;
        this.lowStockMedicineCount = lowStockMedicineCount;
        this.totalRevenue = totalRevenue;
        this.outstandingAmount = outstandingAmount;
    }

    public long getTotalPatients() { return totalPatients; }
    public long getTotalDoctors() { return totalDoctors; }
    public long getTodayAppointments() { return todayAppointments; }
    public long getTotalAppointments() { return totalAppointments; }
    public long getTotalBills() { return totalBills; }
    public long getLowStockMedicineCount() { return lowStockMedicineCount; }
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public BigDecimal getOutstandingAmount() { return outstandingAmount; }
}
