package com.medicore.patientservice.mapper;

import com.medicore.patientservice.dto.PatientRequestDTO;
import com.medicore.patientservice.dto.PatientResponseDTO;
import com.medicore.patientservice.model.Patient;

public final class PatientMapper {

    private PatientMapper() {
    }

    public static PatientResponseDTO toDTO(Patient patient) {
        PatientResponseDTO dto = new PatientResponseDTO();
        dto.setId(patient.getId());
        dto.setName(patient.getName());
        dto.setEmail(patient.getEmail());
        dto.setAddress(patient.getAddress());
        dto.setDateOfBirth(patient.getDateOfBirth());
        return dto;
    }

    public static Patient toModel(PatientRequestDTO dto) {
        Patient patient = new Patient();
        patient.setName(dto.getName());
        patient.setEmail(dto.getEmail());
        patient.setAddress(dto.getAddress());
        patient.setDateOfBirth(dto.getDateOfBirth());
        patient.setRegisteredDate(dto.getRegisteredDate());
        return patient;
    }
}
