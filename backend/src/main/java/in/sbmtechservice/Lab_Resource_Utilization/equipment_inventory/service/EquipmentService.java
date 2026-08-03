package in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.service;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.entity.User;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.repository.UserRepository;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.dto.EquipmentRequest;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.dto.EquipmentResponse;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.entity.Equipment;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.entity.EquipmentCategory;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.entity.Tag;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.enums.EquipmentStatus;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.repository.EquipmentCategoryRepository;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.repository.EquipmentRepository;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.repository.TagRepository;
import in.sbmtechservice.Lab_Resource_Utilization.institution.entity.Department;
import in.sbmtechservice.Lab_Resource_Utilization.institution.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final DepartmentRepository departmentRepository;
    private final EquipmentCategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;

    @Transactional
    public EquipmentResponse addEquipment(EquipmentRequest request, String currentUserEmail) {
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean isSystemAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> role.getName().name().equals("SYSTEM_ADMIN"));

        if (!isSystemAdmin) {
            Department requestDepartment = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Department not found."));
            UUID userInstId = currentUser.getInstitution() != null ? currentUser.getInstitution().getId() : (currentUser.getDepartment() != null && currentUser.getDepartment().getInstitution() != null ? currentUser.getDepartment().getInstitution().getId() : null);
            if (userInstId == null || !userInstId.equals(requestDepartment.getInstitution().getId())) {
                throw new SecurityException("You can only add equipment to your own institution.");
            }
        }

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new IllegalArgumentException("Department not found."));

        if (equipmentRepository.existsBySerialNumber(request.getSerialNumber())) {
            throw new IllegalArgumentException("Equipment with this Serial Number already exists.");
        }

        EquipmentCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found."));

        Set<Tag> tags = new HashSet<>();
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            List<Tag> fetchedTags = tagRepository.findAllById(request.getTagIds());
            tags.addAll(fetchedTags);
        }

        Equipment equipment = Equipment.builder()
                .name(request.getName())
                .manufacturer(request.getManufacturer())
                .modelNumber(request.getModelNumber())
                .serialNumber(request.getSerialNumber())
                .description(request.getDescription())
                .documentationUrl(request.getDocumentationUrl())
                .imageBase64(request.getImageBase64())
                .pricePerHour(request.getPricePerHour() != null ? request.getPricePerHour() : java.math.BigDecimal.ZERO)
                .department(department)
                .category(category)
                .tags(tags)
                .status(EquipmentStatus.AVAILABLE)
                .build();

        Equipment saved = equipmentRepository.save(equipment);
        return mapToResponse(saved);
    }

    // 🚨 RESTORED METHOD: Controller needs this to fetch equipment 🚨
    public List<EquipmentResponse> getEquipmentByDepartment(UUID departmentId) {
        return equipmentRepository.findByDepartmentId(departmentId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<EquipmentResponse> getEquipmentByInstitution(UUID institutionId) {
        return equipmentRepository.findAll().stream()
                .filter(e -> e.getDepartment() != null && e.getDepartment().getInstitution() != null && e.getDepartment().getInstitution().getId().equals(institutionId))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<EquipmentResponse> getAllEquipment() {
        return equipmentRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // 🚨 RESTORED METHOD: Controller needs this to update status 🚨
    @Transactional
    public EquipmentResponse updateEquipmentStatus(UUID equipmentId, EquipmentStatus newStatus, String currentUserEmail) {
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean isSystemAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> role.getName().name().equals("SYSTEM_ADMIN"));

        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new IllegalArgumentException("Equipment not found."));

        if (!isSystemAdmin) {
            UUID userInstId = currentUser.getInstitution() != null ? currentUser.getInstitution().getId() : (currentUser.getDepartment() != null && currentUser.getDepartment().getInstitution() != null ? currentUser.getDepartment().getInstitution().getId() : null);
            if (userInstId == null || !userInstId.equals(equipment.getDepartment().getInstitution().getId())) {
                throw new SecurityException("You can only update equipment status in your own institution.");
            }
        }

        equipment.setStatus(newStatus);
        Equipment saved = equipmentRepository.save(equipment);
        return mapToResponse(saved);
    }

    @Transactional
    public EquipmentResponse updateEquipment(UUID equipmentId, EquipmentRequest request, String currentUserEmail) {
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean isSystemAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> role.getName().name().equals("SYSTEM_ADMIN"));

        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new IllegalArgumentException("Equipment not found."));

        if (!isSystemAdmin) {
            UUID userInstId = currentUser.getInstitution() != null ? currentUser.getInstitution().getId() : (currentUser.getDepartment() != null && currentUser.getDepartment().getInstitution() != null ? currentUser.getDepartment().getInstitution().getId() : null);
            if (userInstId == null || !userInstId.equals(equipment.getDepartment().getInstitution().getId())) {
                throw new SecurityException("You can only update equipment in your own institution.");
            }
        }

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new IllegalArgumentException("Department not found."));
        EquipmentCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found."));

        equipment.setName(request.getName());
        equipment.setManufacturer(request.getManufacturer());
        equipment.setModelNumber(request.getModelNumber());
        equipment.setSerialNumber(request.getSerialNumber());
        equipment.setDescription(request.getDescription());
        equipment.setDocumentationUrl(request.getDocumentationUrl());
        if (request.getImageBase64() != null) {
            equipment.setImageBase64(request.getImageBase64());
        }
        equipment.setPricePerHour(request.getPricePerHour() != null ? request.getPricePerHour() : java.math.BigDecimal.ZERO);
        equipment.setDepartment(department);
        equipment.setCategory(category);
        
        Set<Tag> tags = new HashSet<>();
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            List<Tag> fetchedTags = tagRepository.findAllById(request.getTagIds());
            tags.addAll(fetchedTags);
        }
        equipment.setTags(tags);

        Equipment saved = equipmentRepository.save(equipment);
        return mapToResponse(saved);
    }

    public EquipmentResponse mapToResponse(Equipment equipment) {
        Set<String> tagNames = equipment.getTags().stream()
                .map(Tag::getName)
                .collect(Collectors.toSet());

        return EquipmentResponse.builder()
                .id(equipment.getId())
                .name(equipment.getName())
                .manufacturer(equipment.getManufacturer())
                .modelNumber(equipment.getModelNumber())
                .serialNumber(equipment.getSerialNumber())
                .description(equipment.getDescription())
                .status(equipment.getStatus())
                .documentationUrl(equipment.getDocumentationUrl())
                .imageBase64(equipment.getImageBase64())
                .pricePerHour(equipment.getPricePerHour())
                .departmentId(equipment.getDepartment().getId())
                .departmentName(equipment.getDepartment().getName())
                .categoryId(equipment.getCategory().getId())
                .categoryName(equipment.getCategory().getName())
                .tags(tagNames)
                .institutionId(equipment.getDepartment().getInstitution().getId())
                .institutionName(equipment.getDepartment().getInstitution().getName())
                .build();
    }
}