package com.hospital.hms.hospitaladmin.service;

import com.hospital.hms.hospitaladmin.dto.StaffDTO;

import java.util.List;

public interface StaffService {
    StaffDTO createStaff(StaffDTO dto);
    StaffDTO getStaffById(Long id);
    List<StaffDTO> getAllStaff();
    List<StaffDTO> getStaffByDepartment(Long departmentId);
    StaffDTO updateStaff(Long id, StaffDTO dto);
    void deleteStaff(Long id);
}
