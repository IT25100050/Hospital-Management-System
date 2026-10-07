package com.hospital.hms.billing.service;

import com.hospital.hms.billing.dto.BillDTO;
import com.hospital.hms.billing.model.Bill;
import com.hospital.hms.billing.model.BillItem;
import com.hospital.hms.billing.repository.BillRepository;
import com.hospital.hms.billing.repository.PaymentRepository;
import com.hospital.hms.common.enums.PaymentStatus;
import com.hospital.hms.common.exception.BadRequestException;
import com.hospital.hms.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BillServiceImpl implements BillService {

    private final BillRepository billRepository;
    private final PaymentRepository paymentRepository;

    @Override
    @Transactional
    public BillDTO createBill(BillDTO dto) {
        String invoiceNumber = "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Bill bill = Bill.builder()
                .invoiceNumber(invoiceNumber)
                .patientId(dto.getPatientId())
                .appointmentId(dto.getAppointmentId())
                .paidAmount(BigDecimal.ZERO)
                .status(PaymentStatus.PENDING)
                .createdDate(LocalDateTime.now())
                .build();

        bill.setItems(buildItems(dto, bill));
        bill.setTotalAmount(sumItems(bill.getItems()));

        Bill saved = billRepository.save(bill);
        return mapToDTO(saved);
    }

    @Override
    public BillDTO getBillById(Long id) {
        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with id: " + id));
        return mapToDTO(bill);
    }

    @Override
    public BillDTO getBillByInvoiceNumber(String invoiceNumber) {
        Bill bill = billRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with invoice number: " + invoiceNumber));
        return mapToDTO(bill);
    }

    @Override
    public List<BillDTO> getAllBills() {
        return billRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<BillDTO> getBillsByPatientId(Long patientId) {
        return billRepository.findByPatientId(patientId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BillDTO updateBill(Long id, BillDTO dto) {
        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with id: " + id));

        if (bill.getStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("Cannot modify a bill that has already been fully paid.");
        }

        if (dto.getAppointmentId() != null) {
            bill.setAppointmentId(dto.getAppointmentId());
        }

        if (dto.getItems() != null) {
            bill.getItems().clear();
            bill.getItems().addAll(buildItems(dto, bill));

            BigDecimal newTotal = sumItems(bill.getItems());
            if (newTotal.compareTo(bill.getPaidAmount()) < 0) {
                throw new BadRequestException("New bill total cannot be less than the amount already paid (" + bill.getPaidAmount() + ").");
            }
            bill.setTotalAmount(newTotal);

            if (bill.getPaidAmount().compareTo(BigDecimal.ZERO) == 0) {
                bill.setStatus(PaymentStatus.PENDING);
            } else if (bill.getPaidAmount().compareTo(newTotal) == 0) {
                bill.setStatus(PaymentStatus.PAID);
            } else {
                bill.setStatus(PaymentStatus.PARTIAL);
            }
        }

        Bill updated = billRepository.save(bill);
        return mapToDTO(updated);
    }

    @Override
    @Transactional
    public void deleteBill(Long id) {
        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with id: " + id));

        if (!paymentRepository.findByBillId(id).isEmpty()) {
            throw new BadRequestException("Cannot delete a bill that already has payments recorded against it.");
        }

        billRepository.delete(bill);
    }

    private List<BillItem> buildItems(BillDTO dto, Bill bill) {
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            return new ArrayList<>();
        }
        return dto.getItems().stream().map(itemDto -> {
            BigDecimal itemAmount = itemDto.getUnitPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            return BillItem.builder()
                    .bill(bill)
                    .description(itemDto.getDescription())
                    .quantity(itemDto.getQuantity())
                    .unitPrice(itemDto.getUnitPrice())
                    .amount(itemAmount)
                    .build();
        }).collect(Collectors.toList());
    }

    private BigDecimal sumItems(List<BillItem> items) {
        return items.stream()
                .map(BillItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BillDTO mapToDTO(Bill bill) {
        List<BillDTO.BillItemDTO> itemDTOs = bill.getItems().stream()
                .map(item -> BillDTO.BillItemDTO.builder()
                        .id(item.getId())
                        .description(item.getDescription())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .amount(item.getAmount())
                        .build())
                .collect(Collectors.toList());

        return BillDTO.builder()
                .id(bill.getId())
                .invoiceNumber(bill.getInvoiceNumber())
                .patientId(bill.getPatientId())
                .appointmentId(bill.getAppointmentId())
                .totalAmount(bill.getTotalAmount())
                .paidAmount(bill.getPaidAmount())
                .status(bill.getStatus())
                .createdDate(bill.getCreatedDate())
                .items(itemDTOs)
                .build();
    }
}
