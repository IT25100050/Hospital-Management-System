package com.hospital.hms.pharmacy.event;

import org.springframework.context.ApplicationEvent;

/**
 * Observer pattern (via Spring's built-in event bus): MedicineServiceImpl (the subject)
 * publishes this event whenever a medicine's stock drops to or below the reorder
 * threshold. It has no idea who, if anyone, is listening — LowStockEventListener (the
 * observer) picks it up separately. More listeners (e.g. an email notifier) could be
 * added later without changing MedicineServiceImpl at all.
 */
public class LowStockEvent extends ApplicationEvent {

    private final Long medicineId;
    private final String medicineName;
    private final int remainingStock;

    public LowStockEvent(Object source, Long medicineId, String medicineName, int remainingStock) {
        super(source);
        this.medicineId = medicineId;
        this.medicineName = medicineName;
        this.remainingStock = remainingStock;
    }

    public Long getMedicineId() { return medicineId; }
    public String getMedicineName() { return medicineName; }
    public int getRemainingStock() { return remainingStock; }
}
