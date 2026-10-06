package com.hospital.hms.pharmacy.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class LowStockEventListener {

    private static final Logger log = LoggerFactory.getLogger(LowStockEventListener.class);

    @EventListener
    public void onLowStock(LowStockEvent event) {
        log.warn("LOW STOCK ALERT: '{}' (id={}) has only {} unit(s) left — reorder soon.",
                event.getMedicineName(), event.getMedicineId(), event.getRemainingStock());
        // Extension point: send an email/SMS to the pharmacy manager, create a
        // re-order ticket, push a dashboard notification, etc. — all without
        // MedicineServiceImpl ever needing to know about it.
    }
}
