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

    // Methods to get lists from database
    public List<String> getClientsList() {
        return dropdownValueService.getActiveValuesByCategory(DropdownValueService.CATEGORY_CLIENT);
    }

    public List<String> getProjectsList() {
        return dropdownValueService
                .getActiveValuesByCategory(DropdownValueService.CATEGORY_PROJECT);
    }

    public List<String> getPhasesList() {
        return dropdownValueService.getActiveValuesByCategory(DropdownValueService.CATEGORY_PHASE);
    }
}
