package in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.controller;

import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.dto.CategoryRequest;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.dto.CategoryResponse;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('LAB_MANAGER') or hasAuthority('DEPT_HEAD')")
    public ResponseEntity<CategoryResponse> createCategory(@RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.createCategory(request));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<java.util.List<CategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }
}