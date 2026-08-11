package in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.controller;

import in.sbmtechservice.Lab_Resource_Utilization.auth_user.entity.User;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.repository.UserRepository;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.dto.CategoryRequest;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.dto.CategoryResponse;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('LAB_MANAGER') or hasAuthority('DEPT_HEAD')")
    public ResponseEntity<CategoryResponse> createCategory(
            @RequestBody CategoryRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Resolve caller for notification context
        java.util.UUID addedById = null;
        String addedByName = null;
        if (userDetails != null) {
            User caller = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
            if (caller != null) {
                addedById = caller.getId();
                addedByName = caller.getFirstName() + " " + caller.getLastName();
            }
        }

        return ResponseEntity.ok(categoryService.createCategory(request, addedById, addedByName));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }
}