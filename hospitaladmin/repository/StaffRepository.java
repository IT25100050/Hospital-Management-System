package com.hospital.hms.hospitaladmin.repository;

import com.hospital.hms.hospitaladmin.model.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StaffRepository extends JpaRepository<Staff, Long> {
    List<Staff> findByDepartmentId(Long departmentId);
    boolean existsByEmail(String email);
}
