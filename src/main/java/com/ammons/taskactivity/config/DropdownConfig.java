package com.ammons.taskactivity.config;

import com.ammons.taskactivity.service.DropdownValueService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * DropdownConfig
 *
 * @author Dean Ammons
 * @version 1.0
 */
@Component
public class DropdownConfig {

    private final DropdownValueService dropdownValueService;

    public DropdownConfig(DropdownValueService dropdownValueService) {
        this.dropdownValueService = dropdownValueService;
    }

    // Methods to get lists from database (now using TASK category with subcategories)
    public List<String> getClientsList() {
        return dropdownValueService
                .getActiveValuesByCategoryAndSubcategory(DropdownValueService.CATEGORY_TASK,
                        DropdownValueService.SUBCATEGORY_CLIENT)
                .stream().map(dv -> dv.getItemValue()).toList();
    }

    public List<String> getProjectsList() {
        return dropdownValueService
                .getActiveValuesByCategoryAndSubcategory(DropdownValueService.CATEGORY_TASK,
                        DropdownValueService.SUBCATEGORY_PROJECT)
                .stream().map(dv -> dv.getItemValue()).toList();
    }

    public List<String> getPhasesList() {
        return dropdownValueService
                .getActiveValuesByCategoryAndSubcategory(DropdownValueService.CATEGORY_TASK,
                        DropdownValueService.SUBCATEGORY_PHASE)
                .stream().map(dv -> dv.getItemValue()).toList();
    }
}
