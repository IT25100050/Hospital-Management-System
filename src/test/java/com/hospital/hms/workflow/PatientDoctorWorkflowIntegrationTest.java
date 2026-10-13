package com.hospital.hms.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.hms.auth.model.User;
import com.hospital.hms.auth.repository.UserRepository;
import com.hospital.hms.common.enums.UserRole;
import com.hospital.hms.appointment.model.Appointment;
import com.hospital.hms.appointment.repository.AppointmentRepository;
import com.hospital.hms.doctorrecords.model.Doctor;
import com.hospital.hms.doctorrecords.model.MedicalRecord;
import com.hospital.hms.doctorrecords.repository.DoctorRepository;
import com.hospital.hms.doctorrecords.repository.MedicalRecordRepository;
import com.hospital.hms.patient.model.Patient;
import com.hospital.hms.patient.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PatientDoctorWorkflowIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private MedicalRecordRepository medicalRecordRepository;
    @Autowired private PatientRepository patientRepository;

    private String suffix;
    private String adminToken;

    @BeforeEach
    void createManagementAccount() throws Exception {
        suffix = UUID.randomUUID().toString().replace("-", "");
        userRepository.save(new User("admin-" + suffix, "admin-" + suffix + "@example.test",
                passwordEncoder.encode("AdminPassword123"), UserRole.ADMIN));
        adminToken = login("admin-" + suffix, "AdminPassword123");
    }

    @Test
    void patientAndDoctorRegistrationAppointmentAndAuthorizationWorkflow() throws Exception {
        String patientName = "Patient " + suffix;
        String patientEmail = "patient-" + suffix + "@example.test";
        registerPatient(patientName, patientEmail);
        String patientToken = login(patientName, "PatientPass123");
        assertEquals("PATIENT", postJson("/api/auth/login", null,
                json("username", patientName, "password", "PatientPass123")).path("data").path("role").asText());

        String specialty1 = "Cardiology-" + suffix;
        JsonNode application1 = submitDoctorApplication("doctor1-" + suffix, specialty1);
        String doctorEmail1 = application1.path("email").asText();
        LocalDate date = nextWeekday();
        JsonNode beforeApproval = getJson("/api/doctors/available?date=" + date, patientToken);
        assertFalse(containsSpecialty(beforeApproval.path("data"), specialty1));
        assertNotEquals(200, loginStatus(doctorEmail1, "DoctorPass123"));

        reviewApplication(application1.path("id").asLong(), "APPROVED", null);
        assertEquals(400, reviewStatus(application1.path("id").asLong(), "APPROVED", null));
        String doctorToken1 = login(doctorEmail1, "DoctorPass123");
        assertEquals("DOCTOR", postJson("/api/auth/login", null,
                json("username", doctorEmail1, "password", "DoctorPass123")).path("data").path("role").asText());

        String specialty2 = "Neurology-" + suffix;
        JsonNode application2 = submitDoctorApplication("doctor2-" + suffix, specialty2);
        reviewApplication(application2.path("id").asLong(), "APPROVED", null);
        String doctorToken2 = login(application2.path("email").asText(), "DoctorPass123");

        JsonNode available = getJson("/api/doctors/available?date=" + date, patientToken);
        JsonNode doctor1 = findSpecialty(available.path("data"), specialty1);
        assertNotNull(doctor1);
        assertEquals("MBBS, MD", doctor1.path("qualifications").asText());
        assertFalse(doctor1.path("availableSlots").isEmpty());
        String slot1 = doctor1.path("availableSlots").get(0).asText();
        JsonNode booking1 = book(patientToken, 999999L, doctor1.path("id").asLong(), slot1, "Persistent cough");
        long bookingId1 = booking1.path("data").path("id").asLong();
        assertEquals("PENDING", booking1.path("data").path("status").asText());
        assertNotEquals(999999L, booking1.path("data").path("patientId").asLong());
        assertEquals(409, bookingStatus(patientToken, doctor1.path("id").asLong(), slot1));
        assertEquals(404, doctorDecisionStatus(doctorToken2, bookingId1, "APPROVED", null));
        assertEquals(403, getStatus("/api/doctor-applications", patientToken));
        assertEquals(403, reviewStatusWithToken(patientToken, application1.path("id").asLong(), "APPROVED", null));

        decide(doctorToken1, bookingId1, "APPROVED", null);
        assertEquals("APPROVED", appointmentWithId(getJson("/api/appointments/mine", patientToken)
                .path("data"), bookingId1).path("status").asText());
        JsonNode createdRecord = postJson("/api/medical-records/appointment/" + bookingId1, doctorToken1,
                json("diagnosis", "Seasonal allergy", "treatment", "Antihistamine", "notes", "Follow up if symptoms continue"))
                .path("data");
        assertEquals(booking1.path("data").path("patientId").asLong(), createdRecord.path("patientId").asLong());
        assertEquals(doctor1.path("id").asLong(), createdRecord.path("doctorId").asLong());
        long recordId = createdRecord.path("id").asLong();
        assertEquals(200, mvc.perform(put("/api/medical-records/{id}", recordId)
                .header("Authorization", "Bearer " + doctorToken1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("diagnosis", "Updated seasonal allergy"))).andReturn().getResponse().getStatus());
        assertEquals("Updated seasonal allergy", getJson("/api/medical-records/mine", doctorToken1)
                .path("data").get(0).path("diagnosis").asText());
        assertEquals(403, mvc.perform(post("/api/medical-records/appointment/{id}", bookingId1)
                .header("Authorization", "Bearer " + doctorToken2)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("diagnosis", "Unauthorized"))).andReturn().getResponse().getStatus());
        assertEquals(403, mvc.perform(delete("/api/medical-records/{id}", recordId)
                .header("Authorization", "Bearer " + doctorToken2)).andReturn().getResponse().getStatus());
        assertEquals(200, mvc.perform(delete("/api/medical-records/{id}", recordId)
                .header("Authorization", "Bearer " + doctorToken1)).andReturn().getResponse().getStatus());

        JsonNode booking2 = book(patientToken, null, doctor1.path("id").asLong(),
                date + "T10:30", "Follow-up visit");
        long bookingId2 = booking2.path("data").path("id").asLong();
        decide(doctorToken1, bookingId2, "REJECTED", "Please book a specialist clinic.");
        JsonNode rejected = getJson("/api/appointments/mine", patientToken).path("data");
        JsonNode rejectedBooking = appointmentWithId(rejected, bookingId2);
        assertEquals("REJECTED", rejectedBooking.path("status").asText());
        assertEquals("Please book a specialist clinic.", rejectedBooking.path("rejectionReason").asText());
        assertFalse(getJson("/api/appointments/doctor/mine", doctorToken2).path("data").iterator().hasNext());

        String otherPatientName = "Other " + suffix;
        registerPatient(otherPatientName, "other-" + suffix + "@example.test");
        String otherPatientToken = login(otherPatientName, "PatientPass123");
        assertTrue(getJson("/api/appointments/mine", otherPatientToken).path("data").isEmpty());
        assertEquals(404, cancelBookingStatus(otherPatientToken, bookingId1));

        JsonNode rejectedApplication = submitDoctorApplication("doctor3-" + suffix, "Dermatology-" + suffix);
        assertEquals(404, loginStatus(rejectedApplication.path("email").asText(), "DoctorPass123"));
        reviewApplication(rejectedApplication.path("id").asLong(), "REJECTED", "Registration could not be verified.");
        assertEquals("REJECTED", getJson("/api/doctor-applications", adminToken)
                .path("data").get(0).path("status").asText());
        assertEquals(404, loginStatus(rejectedApplication.path("email").asText(), "DoctorPass123"));
    }

    @Test
    void doctorRecordsManagerCanManageOnlyDoctorAndMedicalRecordAreas() throws Exception {
        String managerUsername = "records-manager-" + suffix;
        userRepository.save(new User(managerUsername, managerUsername + "@example.test",
                passwordEncoder.encode("ManagerPassword123"), UserRole.DOCTOR_RECORDS_MANAGER));
        String managerToken = login(managerUsername, "ManagerPassword123");

        assertEquals(200, getStatus("/api/doctor-applications", managerToken));
        assertEquals(200, getStatus("/api/doctors", managerToken));
        assertEquals(200, getStatus("/api/medical-records", managerToken));
        assertEquals(200, getStatus("/api/patients", managerToken));
        assertEquals(200, getStatus("/api/admin/departments", managerToken));

        assertEquals(403, getStatus("/api/auth/admin/users", managerToken));
        assertEquals(403, getStatus("/api/admin/staff", managerToken));
        assertEquals(403, getStatus("/api/dashboard", managerToken));
        assertEquals(403, getStatus("/api/bills", managerToken));

        JsonNode application = submitDoctorApplication("managed-doctor-" + suffix, "Cardiology-" + suffix);
        assertEquals(200, reviewStatusWithToken(managerToken, application.path("id").asLong(), "APPROVED", null));
        assertEquals(200, loginStatus(application.path("email").asText(), "DoctorPass123"));
        Doctor approvedDoctor = doctorRepository.findByUserUsername(application.path("email").asText()).orElseThrow();
        registerPatient("records-patient-" + suffix, "records-patient-" + suffix + "@example.test");
        Patient patient = patientRepository.findByUserUsername("records-patient-" + suffix).orElseThrow();
        MedicalRecord record = medicalRecordRepository.save(new MedicalRecord(patient, approvedDoctor,
                "Manager-visible diagnosis", null, null, LocalDateTime.now()));
        assertEquals(200, getStatus("/api/medical-records/" + record.getId(), managerToken));
        assertEquals(403, mvc.perform(put("/api/medical-records/{id}", record.getId())
                .header("Authorization", "Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("diagnosis", "Not allowed"))).andReturn().getResponse().getStatus());
        assertEquals(403, mvc.perform(post("/api/medical-records")
                .header("Authorization", "Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("patientId", patient.getId(), "doctorId", approvedDoctor.getId(), "diagnosis", "Not allowed")))
                .andReturn().getResponse().getStatus());
        String approvedDoctorToken = login(application.path("email").asText(), "DoctorPass123");
        JsonNode editRequest = postJson("/api/medical-records/" + record.getId() + "/change-requests", managerToken,
                json("changeType", "EDIT", "diagnosis", "Proposed diagnosis", "treatment", "Updated treatment",
                        "notes", "Requested note", "requestReason", "Correction requested")).path("data");
        assertEquals("PENDING", editRequest.path("status").asText());
        assertEquals("Manager-visible diagnosis", getJson("/api/medical-records/" + record.getId(), managerToken)
                .path("data").path("diagnosis").asText());
        JsonNode doctorRequests = getJson("/api/medical-records/change-requests/mine", approvedDoctorToken);
        assertEquals(editRequest.path("id").asLong(), doctorRequests.path("data").get(0).path("id").asLong());
        JsonNode otherDoctorApplication = submitDoctorApplication("other-reviewer-" + suffix, "Neurology-" + suffix);
        reviewApplication(otherDoctorApplication.path("id").asLong(), "APPROVED", null);
        String otherDoctorToken = login(otherDoctorApplication.path("email").asText(), "DoctorPass123");
        assertEquals(404, mvc.perform(post("/api/medical-records/change-requests/{id}/review", editRequest.path("id").asLong())
                .header("Authorization", "Bearer " + otherDoctorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("status", "APPROVED"))).andReturn().getResponse().getStatus());
        mvc.perform(post("/api/medical-records/change-requests/{id}/review", editRequest.path("id").asLong())
                        .header("Authorization", "Bearer " + approvedDoctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("status", "APPROVED")))
                .andExpect(status().isOk());
        assertEquals("Proposed diagnosis", getJson("/api/medical-records/" + record.getId(), managerToken)
                .path("data").path("diagnosis").asText());

        JsonNode secondEditRequest = postJson("/api/medical-records/" + record.getId() + "/change-requests", managerToken,
                json("changeType", "EDIT", "diagnosis", "Second proposed diagnosis",
                        "requestReason", "Another correction")).path("data");
        assertEquals("PENDING", secondEditRequest.path("status").asText());
        mvc.perform(delete("/api/medical-records/{id}", record.getId())
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk());
        assertEquals(404, getStatus("/api/medical-records/" + record.getId(), managerToken));
        JsonNode closedRequests = getJson("/api/medical-records/change-requests/mine", approvedDoctorToken);
        assertEquals(secondEditRequest.path("id").asLong(), closedRequests.path("data").get(0).path("id").asLong());
        assertEquals("REJECTED", closedRequests.path("data").get(0).path("status").asText());

        mvc.perform(delete("/api/auth/admin/users/{username}", managerUsername)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        JsonNode preservedHistory = getJson("/api/medical-records/change-requests/mine", approvedDoctorToken);
        assertEquals(managerUsername, preservedHistory.path("data").get(0).path("requestedBy").asText());
    }

    @Test
    void onlyPharmacyRolesCanAccessPharmacyApisAndFinanceCanOpenBilling() throws Exception {
        Map<UserRole, String> tokens = new HashMap<>();
        tokens.put(UserRole.ADMIN, adminToken);

        for (UserRole role : UserRole.values()) {
            if (role == UserRole.ADMIN || role == UserRole.DOCTOR) {
                continue;
            }
            String username = "pharmacy-" + role.name().toLowerCase() + "-" + suffix;
            userRepository.save(new User(username, username + "@example.test",
                    passwordEncoder.encode("PharmacyPassword123"), role));
            tokens.put(role, login(username, "PharmacyPassword123"));
        }

        JsonNode doctorApplication = submitDoctorApplication("pharmacy-doctor-" + suffix, "PharmacyTest");
        reviewApplication(doctorApplication.path("id").asLong(), "APPROVED", null);
        tokens.put(UserRole.DOCTOR, login(doctorApplication.path("email").asText(), "DoctorPass123"));

        for (Map.Entry<UserRole, String> entry : tokens.entrySet()) {
            UserRole role = entry.getKey();
            String token = entry.getValue();
            assertEquals(role.name(), getJson("/api/auth/me", token).path("data").path("role").asText());
            if (role == UserRole.ADMIN || role == UserRole.PHARMACIST) {
                assertEquals(200, getStatus("/api/medicines", token), role + " cannot view medicines");
                assertEquals(200, getStatus("/api/prescriptions", token), role + " cannot view prescriptions");
            } else {
                assertEquals(403, getStatus("/api/medicines", token), role + " can access pharmacy inventory");
                assertEquals(403, getStatus("/api/prescriptions", token), role + " can access prescriptions");
                assertEquals(403, mvc.perform(post("/api/medicines")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "Unauthorized medicine")))
                        .andReturn().getResponse().getStatus(), role + " can modify pharmacy inventory");
                assertEquals(403, mvc.perform(post("/api/prescriptions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("notes", "Unauthorized prescription")))
                        .andReturn().getResponse().getStatus(), role + " can modify prescriptions");
            }
        }
        String financeToken = tokens.get(UserRole.FINANCE_OFFICER);
        assertEquals(200, getStatus("/api/patients/lookup", financeToken));
        assertEquals(200, getStatus("/api/bills", financeToken));
        assertEquals(200, getStatus("/api/payments/bill/999999", financeToken));
        assertEquals(403, getStatus("/api/patients", financeToken));
    }

    @Test
    void onlyAdminCanCreateRoleAccountsAndPasswordsAreHashed() throws Exception {
        String username = "created-finance-" + suffix;
        JsonNode created = postJson("/api/auth/admin/users", adminToken, json(
                "username", username,
                "email", username + "@example.test",
                "password", "SecurePass123",
                "role", "FINANCE_OFFICER")).path("data");

        assertEquals(username, created.path("username").asText());
        assertEquals("FINANCE_OFFICER", created.path("role").asText());
        User saved = userRepository.findByUsername(username).orElseThrow();
        assertTrue(passwordEncoder.matches("SecurePass123", saved.getPassword()));
        assertNotEquals("SecurePass123", saved.getPassword());

        String managerUsername = "account-manager-" + suffix;
        userRepository.save(new User(managerUsername, managerUsername + "@example.test",
                passwordEncoder.encode("ManagerPassword123"), UserRole.DOCTOR_RECORDS_MANAGER));
        String managerToken = login(managerUsername, "ManagerPassword123");
        assertEquals(403, mvc.perform(post("/api/auth/admin/users")
                .header("Authorization", "Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("username", "forbidden-" + suffix, "email", "forbidden-" + suffix + "@example.test",
                        "password", "SecurePass123", "role", "ADMIN")))
                .andReturn().getResponse().getStatus());

        assertEquals(400, mvc.perform(post("/api/auth/admin/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("username", "unapproved-doctor-" + suffix,
                        "email", "unapproved-doctor-" + suffix + "@example.test",
                        "password", "SecurePass123", "role", "DOCTOR")))
                .andReturn().getResponse().getStatus());
    }

    @Test
    void adminCanDeleteLoginAccountWhileRetainingPatientProfileAndRejectingSelfDelete() throws Exception {
        String username = "delete-patient-" + suffix;
        postJson("/api/auth/admin/users", adminToken, json(
                "username", username,
                "email", username + "@example.test",
                "password", "SecurePass123",
                "role", "PATIENT"));
        Patient profile = patientRepository.findByUserUsername(username).orElseThrow();

        mvc.perform(delete("/api/auth/admin/users/{username}", username)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        assertTrue(userRepository.findByUsername(username).isEmpty());
        Patient retainedProfile = patientRepository.findById(profile.getId()).orElseThrow();
        assertNull(retainedProfile.getUser());
        assertNull(retainedProfile.getEmail());

        assertEquals(400, mvc.perform(delete("/api/auth/admin/users/{username}", "admin-" + suffix)
                .header("Authorization", "Bearer " + adminToken))
                .andReturn().getResponse().getStatus());

        String managerUsername = "account-delete-manager-" + suffix;
        userRepository.save(new User(managerUsername, managerUsername + "@example.test",
                passwordEncoder.encode("ManagerPassword123"), UserRole.DOCTOR_RECORDS_MANAGER));
        String managerToken = login(managerUsername, "ManagerPassword123");
        assertEquals(403, mvc.perform(delete("/api/auth/admin/users/{username}", username)
                .header("Authorization", "Bearer " + managerToken))
                .andReturn().getResponse().getStatus());
    }

    private void registerPatient(String username, String email) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json("username", username, "email", email, "password", "PatientPass123",
                                "role", "ADMIN")))
                .andExpect(status().isOk());
    }

    private JsonNode submitDoctorApplication(String localPart, String specialty) throws Exception {
        return postJson("/api/doctor-applications", null, json(
                "name", "Dr " + localPart,
                "email", localPart + "@example.test",
                "password", "DoctorPass123",
                "phoneNumber", "0771234567",
                "medicalRegistrationNumber", "REG-" + localPart,
                "specialty", specialty,
                "qualifications", "MBBS, MD",
                "experience", 8)).path("data");
    }

    private void reviewApplication(long id, String status, String reason) throws Exception {
        mvc.perform(post("/api/doctor-applications/{id}/review", id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("status", status, "rejectionReason", reason)))
                .andExpect(status().isOk());
    }

    private int reviewStatus(long id, String status, String reason) throws Exception {
        return reviewStatusWithToken(adminToken, id, status, reason);
    }

    private int reviewStatusWithToken(String token, long id, String status, String reason) throws Exception {
        return mvc.perform(post("/api/doctor-applications/{id}/review", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("status", status, "rejectionReason", reason)))
                .andReturn().getResponse().getStatus();
    }

    private JsonNode book(String token, Long patientId, long doctorId, String slot, String reason) throws Exception {
        String body = patientId == null
                ? json("doctorId", doctorId, "appointmentTime", slot, "reason", reason)
                : json("patientId", patientId, "doctorId", doctorId, "appointmentTime", slot, "reason", reason);
        return postJson("/api/appointments", token, body);
    }

    private int bookingStatus(String token, long doctorId, String slot) throws Exception {
        return mvc.perform(post("/api/appointments").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("doctorId", doctorId, "appointmentTime", slot, "reason", "duplicate")))
                .andReturn().getResponse().getStatus();
    }

    private void decide(String token, long id, String status, String reason) throws Exception {
        mvc.perform(post("/api/appointments/doctor/{id}/decision", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("status", status, "rejectionReason", reason)))
                .andExpect(status().isOk());
    }

    private int doctorDecisionStatus(String token, long id, String status, String reason) throws Exception {
        return mvc.perform(post("/api/appointments/doctor/{id}/decision", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("status", status, "rejectionReason", reason)))
                .andReturn().getResponse().getStatus();
    }

    private int cancelBookingStatus(String token, long id) throws Exception {
        return mvc.perform(post("/api/appointments/mine/{id}/cancel", id)
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getStatus();
    }

    private JsonNode getJson(String path, String token) throws Exception {
        MvcResult result = mvc.perform(get(path).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private int getStatus(String path, String token) throws Exception {
        return mvc.perform(get(path).header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getStatus();
    }

    private JsonNode postJson(String path, String token, String body) throws Exception {
        var request = post(path).contentType(MediaType.APPLICATION_JSON).content(body);
        if (token != null) request.header("Authorization", "Bearer " + token);
        MvcResult result = mvc.perform(request).andReturn();
        assertEquals(200, result.getResponse().getStatus(), result.getResponse().getContentAsString());
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String login(String username, String password) throws Exception {
        return postJson("/api/auth/login", null, json("username", username, "password", password))
                .path("data").path("token").asText();
    }

    private int loginStatus(String username, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("username", username, "password", password)))
                .andReturn().getResponse().getStatus();
    }

    private String json(Object... pairs) throws Exception {
        var node = objectMapper.createObjectNode();
        for (int index = 0; index < pairs.length; index += 2) {
            String key = (String) pairs[index];
            Object value = pairs[index + 1];
            if (value == null) node.putNull(key);
            else if (value instanceof Number number) node.put(key, number.intValue());
            else node.put(key, value.toString());
        }
        return objectMapper.writeValueAsString(node);
    }

    private LocalDate nextWeekday() {
        LocalDate date = LocalDate.now().plusDays(1);
        while (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return date;
    }

    private boolean containsSpecialty(JsonNode doctors, String specialty) {
        for (JsonNode doctor : doctors) if (doctor.path("specialty").asText().equals(specialty)) return true;
        return false;
    }

    private JsonNode findSpecialty(JsonNode doctors, String specialty) {
        for (JsonNode doctor : doctors) if (doctor.path("specialty").asText().equals(specialty)) return doctor;
        return null;
    }

    private JsonNode appointmentWithId(JsonNode appointments, long id) {
        for (JsonNode appointment : appointments) if (appointment.path("id").asLong() == id) return appointment;
        fail("Appointment " + id + " was not returned for its patient.");
        return null;
    }
}
