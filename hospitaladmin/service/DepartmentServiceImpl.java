package com.hospital.hms.hospitaladmin.service;

import com.hospital.hms.common.exception.DuplicateResourceException;
import com.hospital.hms.common.exception.ResourceNotFoundException;
import com.hospital.hms.hospitaladmin.dto.DepartmentDTO;
import com.hospital.hms.hospitaladmin.model.Department;
import com.hospital.hms.hospitaladmin.repository.DepartmentRepository;
import com.hospital.hms.hospitaladmin.service.DepartmentService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentServiceImpl(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Override
    public DepartmentDTO createDepartment(DepartmentDTO dto) {
        if (departmentRepository.existsByName(dto.getName())) {
            throw new DuplicateResourceException("Department with name '" + dto.getName() + "' already exists.");
        }
        Department department = new Department(dto.getName(), dto.getDescription());
        Department saved = departmentRepository.save(department);
        return new DepartmentDTO(saved.getId(), saved.getName(), saved.getDescription());
    }

    @Override
    public DepartmentDTO getDepartmentById(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
        return new DepartmentDTO(department.getId(), department.getName(), department.getDescription());
    }

    @Override
    public List<DepartmentDTO> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(d -> new DepartmentDTO(d.getId(), d.getName(), d.getDescription()))
                .collect(Collectors.toList());
    }

    @Override
    public DepartmentDTO updateDepartment(Long id, DepartmentDTO dto) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));

        department.setName(dto.getName());
        department.setDescription(dto.getDescription());
        Department updated = departmentRepository.save(department);
        return new DepartmentDTO(updated.getId(), updated.getName(), updated.getDescription());
    }

    @Override
    public void deleteDepartment(Long id) {
        if (!departmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Department not found with id: " + id);
        }
        departmentRepository.deleteById(id);
    }
}
