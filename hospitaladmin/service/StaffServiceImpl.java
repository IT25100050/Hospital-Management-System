package com.hospital.hms.hospitaladmin.service;

import com.hospital.hms.common.exception.DuplicateResourceException;
import com.hospital.hms.common.exception.ResourceNotFoundException;
import com.hospital.hms.hospitaladmin.dto.StaffDTO;
import com.hospital.hms.hospitaladmin.model.Department;
import com.hospital.hms.hospitaladmin.model.Staff;
import com.hospital.hms.hospitaladmin.repository.DepartmentRepository;
import com.hospital.hms.hospitaladmin.repository.StaffRepository;
import com.hospital.hms.hospitaladmin.service.StaffService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StaffServiceImpl implements StaffService {

    private final StaffRepository staffRepository;
    private final DepartmentRepository departmentRepository;

    public StaffServiceImpl(StaffRepository staffRepository, DepartmentRepository departmentRepository) {
        this.staffRepository = staffRepository;
        this.departmentRepository = departmentRepository;
    }

    @Override
    public StaffDTO createStaff(StaffDTO dto) {
        if (staffRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("Staff with email '" + dto.getEmail() + "' already exists.");
        }

        Department department = null;
        if (dto.getDepartmentId() != null) {
            department = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + dto.getDepartmentId()));
        }

        Staff staff = new Staff(dto.getName(), dto.getEmail(), dto.getPhone(), dto.getRole(), department);
        Staff saved = staffRepository.save(staff);
        return mapToDTO(saved);
    }

    @Override
    public StaffDTO getStaffById(Long id) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found with id: " + id));
        return mapToDTO(staff);
    }

    @Override
    public List<StaffDTO> getAllStaff() {
        return staffRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<StaffDTO> getStaffByDepartment(Long departmentId) {
        return staffRepository.findByDepartmentId(departmentId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public StaffDTO updateStaff(Long id, StaffDTO dto) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found with id: " + id));

        if (dto.getDepartmentId() != null) {
            Department department = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + dto.getDepartmentId()));
            staff.setDepartment(department);
        }

        staff.setName(dto.getName());
        staff.setEmail(dto.getEmail());
        staff.setPhone(dto.getPhone());
        staff.setRole(dto.getRole());

        Staff updated = staffRepository.save(staff);
        return mapToDTO(updated);
    }

    @Override
    public void deleteStaff(Long id) {
        if (!staffRepository.existsById(id)) {
            throw new ResourceNotFoundException("Staff not found with id: " + id);
        }
        staffRepository.deleteById(id);
    }

    private StaffDTO mapToDTO(Staff staff) {
        Long deptId = staff.getDepartment() != null ? staff.getDepartment().getId() : null;
        String deptName = staff.getDepartment() != null ? staff.getDepartment().getName() : null;
        return new StaffDTO(staff.getId(), staff.getName(), staff.getEmail(), staff.getPhone(), staff.getRole(), deptId, deptName);
    }
}
