package com.smartflow.service;

import com.smartflow.dto.CategoryDtos;
import com.smartflow.entity.Category;
import com.smartflow.exception.BusinessException;
import com.smartflow.exception.ResourceNotFoundException;
import com.smartflow.repository.CategoryRepository;
import com.smartflow.repository.InterventionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gestion des catégories d'interventions.
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final InterventionRepository interventionRepository;

    @Transactional(readOnly = true)
    public List<CategoryDtos.CategoryResponse> listAll() {
        return categoryRepository.findAllByOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public CategoryDtos.CategoryResponse create(CategoryDtos.CategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.name())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Une catégorie porte déjà ce nom");
        }
        return toResponse(categoryRepository.save(toEntity(request)));
    }

    @Transactional
    public CategoryDtos.CategoryResponse update(Long id, CategoryDtos.CategoryRequest request) {
        Category category = find(id);
        category.setName(request.name());
        category.setDescription(request.description());
        category.setColor(request.color());
        category.setIcon(request.icon());
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        Category category = find(id);
        if (interventionRepository.countByCategoryId(id) > 0) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Impossible de supprimer la catégorie : des interventions lui sont rattachées");
        }
        categoryRepository.delete(category);
    }

    public Category find(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie", id));
    }

    private Category toEntity(CategoryDtos.CategoryRequest request) {
        return Category.builder()
                .name(request.name().trim())
                .description(request.description())
                .color(request.color())
                .icon(request.icon())
                .build();
    }

    private CategoryDtos.CategoryResponse toResponse(Category category) {
        return new CategoryDtos.CategoryResponse(
                category.getId(), category.getName(), category.getDescription(),
                category.getColor(), category.getIcon());
    }
}