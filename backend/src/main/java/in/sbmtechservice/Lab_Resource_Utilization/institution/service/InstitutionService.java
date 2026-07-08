package in.sbmtechservice.Lab_Resource_Utilization.institution.service;

import in.sbmtechservice.Lab_Resource_Utilization.institution.dto.InstitutionRequest;
import in.sbmtechservice.Lab_Resource_Utilization.institution.dto.InstitutionResponse;
import in.sbmtechservice.Lab_Resource_Utilization.institution.entity.Institution;
import in.sbmtechservice.Lab_Resource_Utilization.institution.repository.InstitutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InstitutionService {

    private final InstitutionRepository institutionRepository;

    @Transactional
    public InstitutionResponse createInstitution(InstitutionRequest request) {
        if (institutionRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Institution with this name already exists.");
        }

        Institution institution = Institution.builder()
                .name(request.getName())
                .domain(request.getDomain())
                .address(request.getAddress())
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .isActive(true)
                .build();

        Institution saved = institutionRepository.save(institution);
        return mapToResponse(saved);
    }

    public List<InstitutionResponse> getAllInstitutions() {
        return institutionRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private InstitutionResponse mapToResponse(Institution institution) {
        return InstitutionResponse.builder()
                .id(institution.getId())
                .name(institution.getName())
                .address(institution.getAddress())
                .contactEmail(institution.getContactEmail())
                .contactPhone(institution.getContactPhone())
                .isActive(institution.getIsActive())
                .build();
    }
}