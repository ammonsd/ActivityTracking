package com.ammons.taskactivity.service;

import com.ammons.taskactivity.entity.DropdownValue;
import com.ammons.taskactivity.repository.DropdownValueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DropdownValueService Tests")
class DropdownValueServiceTest {

    @Mock
    private DropdownValueRepository dropdownValueRepository;

    @InjectMocks
    private DropdownValueService dropdownValueService;

    private DropdownValue testDropdownValue;
    private final String TEST_CATEGORY = "CLIENT";
    private static final String TEST_SUBCATEGORY = "TASK";
    private final String TEST_VALUE = "Test Client";

    @BeforeEach
    void setUp() {
        testDropdownValue = new DropdownValue();
        testDropdownValue.setId(1L);
        testDropdownValue.setCategory(TEST_CATEGORY);
        testDropdownValue.setSubcategory("TASK");
        testDropdownValue.setItemValue(TEST_VALUE);
        testDropdownValue.setDisplayOrder(1);
        testDropdownValue.setIsActive(true);
    }

    @Nested
    @DisplayName("Get Active Values Tests")
    class GetActiveValuesTests {

        @Test
        @DisplayName("Should get active values by category")
        void shouldGetActiveValuesByCategory() {
            // Given
            DropdownValue value1 = new DropdownValue("CLIENT", "TASK", "Client 1", 1, true);
            DropdownValue value2 = new DropdownValue("CLIENT", "TASK", "Client 2", 2, true);
            List<DropdownValue> dropdownValues = Arrays.asList(value1, value2);

            when(dropdownValueRepository.findActiveByCategoryOrderByDisplayOrder("CLIENT"))
                    .thenReturn(dropdownValues);

            // When
            List<String> result = dropdownValueService.getActiveValuesByCategory("client");

            // Then
            assertThat(result).hasSize(2).containsExactly("Client 1", "Client 2");
            verify(dropdownValueRepository).findActiveByCategoryOrderByDisplayOrder("CLIENT");
        }

        @Test
        @DisplayName("Should handle empty active values list")
        void shouldHandleEmptyActiveValuesList() {
            // Given
            when(dropdownValueRepository.findActiveByCategoryOrderByDisplayOrder("CLIENT"))
                    .thenReturn(Arrays.asList());

            // When
            List<String> result = dropdownValueService.getActiveValuesByCategory("CLIENT");

            // Then
            assertThat(result).isEmpty();
            verify(dropdownValueRepository).findActiveByCategoryOrderByDisplayOrder("CLIENT");
        }

        @Test
        @DisplayName("Should convert category to uppercase")
        void shouldConvertCategoryToUppercase() {
            // Given
            when(dropdownValueRepository.findActiveByCategoryOrderByDisplayOrder("CLIENT"))
                    .thenReturn(Arrays.asList());

            // When
            dropdownValueService.getActiveValuesByCategory("client");

            // Then
            verify(dropdownValueRepository).findActiveByCategoryOrderByDisplayOrder("CLIENT");
        }
    }

    @Nested
    @DisplayName("Get All Values Tests")
    class GetAllValuesTests {

        @Test
        @DisplayName("Should get all values by category")
        void shouldGetAllValuesByCategory() {
            // Given
            DropdownValue activeValue =
                    new DropdownValue("PROJECT", "TASK", "Active Project", 1, true);
            DropdownValue inactiveValue =
                    new DropdownValue("PROJECT", "TASK", "Inactive Project", 2, false);
            List<DropdownValue> allValues = Arrays.asList(activeValue, inactiveValue);

            when(dropdownValueRepository.findByCategoryOrderByDisplayOrder("PROJECT"))
                    .thenReturn(allValues);

            // When
            List<DropdownValue> result = dropdownValueService.getAllValuesByCategory("project");

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getItemValue()).isEqualTo("Active Project");
            assertThat(result.get(0).getIsActive()).isTrue();
            assertThat(result.get(1).getItemValue()).isEqualTo("Inactive Project");
            assertThat(result.get(1).getIsActive()).isFalse();
            verify(dropdownValueRepository).findByCategoryOrderByDisplayOrder("PROJECT");
        }
    }

    @Nested
    @DisplayName("Create Dropdown Value Tests")
    class CreateDropdownValueTests {

        @Test
        @DisplayName("Should create dropdown value successfully")
        void shouldCreateDropdownValueSuccessfully() {
            // Given
            String category = "CLIENT";
            String value = "New Client";
            Integer maxOrder = 5;

            DropdownValue expectedValue = new DropdownValue();
            expectedValue.setId(1L);
            expectedValue.setCategory(category);
            expectedValue.setSubcategory("TASK");
            expectedValue.setItemValue(value);
            expectedValue.setDisplayOrder(maxOrder + 1);
            expectedValue.setIsActive(true);

            when(dropdownValueRepository.findMaxDisplayOrderByCategory(category))
                    .thenReturn(maxOrder);
            when(dropdownValueRepository.save(any(DropdownValue.class))).thenReturn(expectedValue);

            // When
            DropdownValue result =
                    dropdownValueService.createDropdownValue(category, "TASK", value);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getCategory()).isEqualTo(category);
            assertThat(result.getItemValue()).isEqualTo(value);
            assertThat(result.getDisplayOrder()).isEqualTo(maxOrder + 1);
            assertThat(result.getIsActive()).isTrue();

            verify(dropdownValueRepository)
                    .existsByCategoryAndSubcategoryAndItemValueIgnoreCase(category, "TASK", value);
            verify(dropdownValueRepository).findMaxDisplayOrderByCategory(category);
            verify(dropdownValueRepository).save(any(DropdownValue.class));
        }

        @Test
        @DisplayName("Should throw exception for duplicate value")
        void shouldThrowExceptionForDuplicateValue() {
            // Given
            String category = "CLIENT";
            String value = "Existing Client";

            when(dropdownValueRepository
                    .existsByCategoryAndSubcategoryAndItemValueIgnoreCase(category, "TASK", value))
                    .thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> dropdownValueService.createDropdownValue(category, "TASK",
                    value))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage(
                            "Value 'Existing Client' already exists for category CLIENT and subcategory TASK");

            verify(dropdownValueRepository)
                    .existsByCategoryAndSubcategoryAndItemValueIgnoreCase(category, "TASK", value);
            verify(dropdownValueRepository, never()).save(any(DropdownValue.class));
        }

        @Test
        @DisplayName("Should handle null max order")
        void shouldHandleNullMaxOrder() {
            // Given
            String category = "PROJECT";
            String value = "First Project";

            when(dropdownValueRepository
                    .existsByCategoryAndSubcategoryAndItemValueIgnoreCase(category, "TASK", value))
                    .thenReturn(false);
            when(dropdownValueRepository.findMaxDisplayOrderByCategory(category)).thenReturn(null);

            // When/Then
            assertThatThrownBy(
                    () -> dropdownValueService.createDropdownValue(category, "TASK", value))
                    .isInstanceOf(NullPointerException.class);

            verify(dropdownValueRepository)
                    .existsByCategoryAndSubcategoryAndItemValueIgnoreCase(category, "TASK", value);
            verify(dropdownValueRepository).findMaxDisplayOrderByCategory(category);
        }
    }

    @Nested
    @DisplayName("Update Dropdown Value Tests")
    class UpdateDropdownValueTests {

        @Test
        @DisplayName("Should update dropdown value successfully")
        void shouldUpdateDropdownValueSuccessfully() {
            // Given
            Long id = 1L;
            String newValue = "Updated Client";
            Integer newDisplayOrder = 5;
            Boolean newIsActive = false;

            DropdownValue updatedValue = new DropdownValue();
            updatedValue.setId(id);
            updatedValue.setCategory(TEST_CATEGORY);
            updatedValue.setSubcategory("TASK");
            updatedValue.setItemValue(newValue);
            updatedValue.setDisplayOrder(newDisplayOrder);
            updatedValue.setIsActive(newIsActive);

            when(dropdownValueRepository.findById(id)).thenReturn(Optional.of(testDropdownValue));
            when(dropdownValueRepository.existsByCategoryAndSubcategoryAndItemValueIgnoreCase(
                    TEST_CATEGORY, TEST_SUBCATEGORY, newValue)).thenReturn(false);
            when(dropdownValueRepository.save(any(DropdownValue.class))).thenReturn(updatedValue);

            // When
            DropdownValue result = dropdownValueService.updateDropdownValue(id, newValue,
                    newDisplayOrder, newIsActive);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getItemValue()).isEqualTo(newValue);
            assertThat(result.getDisplayOrder()).isEqualTo(newDisplayOrder);
            assertThat(result.getIsActive()).isEqualTo(newIsActive);

            verify(dropdownValueRepository).findById(id);
            verify(dropdownValueRepository).existsByCategoryAndSubcategoryAndItemValueIgnoreCase(
                    TEST_CATEGORY, TEST_SUBCATEGORY, newValue);
            verify(dropdownValueRepository).save(any(DropdownValue.class));
        }

        @Test
        @DisplayName("Should update without duplicate check when value unchanged")
        void shouldUpdateWithoutDuplicateCheckWhenValueUnchanged() {
            // Given
            Long id = 1L;
            String sameValue = TEST_VALUE;
            Integer newDisplayOrder = 5;
            Boolean newIsActive = false;

            when(dropdownValueRepository.findById(id)).thenReturn(Optional.of(testDropdownValue));
            when(dropdownValueRepository.save(any(DropdownValue.class)))
                    .thenReturn(testDropdownValue);

            // When
            DropdownValue result = dropdownValueService.updateDropdownValue(id, sameValue,
                    newDisplayOrder, newIsActive);

            // Then
            assertThat(result).isNotNull();
            verify(dropdownValueRepository).findById(id);
            verify(dropdownValueRepository, never())
                    .existsByCategoryAndSubcategoryAndItemValueIgnoreCase(any(), any(), any());
            verify(dropdownValueRepository).save(any(DropdownValue.class));
        }

        @Test
        @DisplayName("Should throw exception for duplicate value during update")
        void shouldThrowExceptionForDuplicateValueDuringUpdate() {
            // Given
            Long id = 1L;
            String duplicateValue = "Existing Value";

            when(dropdownValueRepository.findById(id)).thenReturn(Optional.of(testDropdownValue));
            when(dropdownValueRepository.existsByCategoryAndSubcategoryAndItemValueIgnoreCase(
                    TEST_CATEGORY, TEST_SUBCATEGORY, duplicateValue)).thenReturn(true);

            // When/Then
            assertThatThrownBy(
                    () -> dropdownValueService.updateDropdownValue(id, duplicateValue, 1, true))
                            .isInstanceOf(RuntimeException.class).hasMessage(
                                    "Value 'Existing Value' already exists for category CLIENT and subcategory TASK");

            verify(dropdownValueRepository).findById(id);
            verify(dropdownValueRepository).existsByCategoryAndSubcategoryAndItemValueIgnoreCase(
                    TEST_CATEGORY, TEST_SUBCATEGORY, duplicateValue);
            verify(dropdownValueRepository, never()).save(any(DropdownValue.class));
        }

        @Test
        @DisplayName("Should throw exception when updating non-existent dropdown value")
        void shouldThrowExceptionWhenUpdatingNonExistentDropdownValue() {
            // Given
            Long nonExistentId = 999L;
            when(dropdownValueRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> dropdownValueService.updateDropdownValue(nonExistentId,
                    "Any Value", 1, true)).isInstanceOf(RuntimeException.class)
                            .hasMessage("Dropdown value not found with ID: 999");

            verify(dropdownValueRepository).findById(nonExistentId);
            verify(dropdownValueRepository, never()).save(any(DropdownValue.class));
        }
    }

    @Nested
    @DisplayName("Delete Dropdown Value Tests")
    class DeleteDropdownValueTests {

        @Test
        @DisplayName("Should delete dropdown value successfully")
        void shouldDeleteDropdownValueSuccessfully() {
            // Given
            Long id = 1L;
            when(dropdownValueRepository.existsById(id)).thenReturn(true);

            // When
            dropdownValueService.deleteDropdownValue(id);

            // Then
            verify(dropdownValueRepository).existsById(id);
            verify(dropdownValueRepository).deleteById(id);
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent dropdown value")
        void shouldThrowExceptionWhenDeletingNonExistentDropdownValue() {
            // Given
            Long nonExistentId = 999L;
            when(dropdownValueRepository.existsById(nonExistentId)).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> dropdownValueService.deleteDropdownValue(nonExistentId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Dropdown value not found with ID: 999");

            verify(dropdownValueRepository).existsById(nonExistentId);
            verify(dropdownValueRepository, never()).deleteById(nonExistentId);
        }
    }

    @Nested
    @DisplayName("Toggle Active Status Tests")
    class ToggleActiveStatusTests {

        @Test
        @DisplayName("Should toggle active status from true to false")
        void shouldToggleActiveStatusFromTrueToFalse() {
            // Given
            Long id = 1L;
            testDropdownValue.setIsActive(true);

            DropdownValue toggledValue = new DropdownValue();
            toggledValue.setId(id);
            toggledValue.setCategory(TEST_CATEGORY);
            toggledValue.setSubcategory("TASK");
            toggledValue.setItemValue(TEST_VALUE);
            toggledValue.setDisplayOrder(1);
            toggledValue.setIsActive(false);

            when(dropdownValueRepository.findById(id)).thenReturn(Optional.of(testDropdownValue));
            when(dropdownValueRepository.save(any(DropdownValue.class))).thenReturn(toggledValue);

            // When
            DropdownValue result = dropdownValueService.toggleActiveStatus(id);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getIsActive()).isFalse();

            verify(dropdownValueRepository).findById(id);
            verify(dropdownValueRepository).save(any(DropdownValue.class));
        }

        @Test
        @DisplayName("Should toggle active status from false to true")
        void shouldToggleActiveStatusFromFalseToTrue() {
            // Given
            Long id = 1L;
            testDropdownValue.setIsActive(false);

            DropdownValue toggledValue = new DropdownValue();
            toggledValue.setId(id);
            toggledValue.setCategory(TEST_CATEGORY);
            toggledValue.setSubcategory("TASK");
            toggledValue.setItemValue(TEST_VALUE);
            toggledValue.setDisplayOrder(1);
            toggledValue.setIsActive(true);

            when(dropdownValueRepository.findById(id)).thenReturn(Optional.of(testDropdownValue));
            when(dropdownValueRepository.save(any(DropdownValue.class))).thenReturn(toggledValue);

            // When
            DropdownValue result = dropdownValueService.toggleActiveStatus(id);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getIsActive()).isTrue();

            verify(dropdownValueRepository).findById(id);
            verify(dropdownValueRepository).save(any(DropdownValue.class));
        }

        @Test
        @DisplayName("Should throw exception when toggling non-existent dropdown value")
        void shouldThrowExceptionWhenTogglingNonExistentDropdownValue() {
            // Given
            Long nonExistentId = 999L;
            when(dropdownValueRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> dropdownValueService.toggleActiveStatus(nonExistentId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Dropdown value not found with ID: 999");

            verify(dropdownValueRepository).findById(nonExistentId);
            verify(dropdownValueRepository, never()).save(any(DropdownValue.class));
        }
    }

    @Nested
    @DisplayName("Get Dropdown Value By ID Tests")
    class GetDropdownValueByIdTests {

        @Test
        @DisplayName("Should get dropdown value by ID")
        void shouldGetDropdownValueById() {
            // Given
            Long id = 1L;
            when(dropdownValueRepository.findById(id)).thenReturn(Optional.of(testDropdownValue));

            // When
            Optional<DropdownValue> result = dropdownValueService.getDropdownValueById(id);

            // Then
            assertThat(result).isPresent().contains(testDropdownValue);
            verify(dropdownValueRepository).findById(id);
        }

        @Test
        @DisplayName("Should return empty when dropdown value not found")
        void shouldReturnEmptyWhenDropdownValueNotFound() {
            // Given
            Long nonExistentId = 999L;
            when(dropdownValueRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When
            Optional<DropdownValue> result =
                    dropdownValueService.getDropdownValueById(nonExistentId);

            // Then
            assertThat(result).isEmpty();
            verify(dropdownValueRepository).findById(nonExistentId);
        }
    }

    @Nested
    @DisplayName("Category Constants Tests")
    class CategoryConstantsTests {

        @Test
        @DisplayName("Should have correct category constants")
        void shouldHaveCorrectCategoryConstants() {
            // Then
            assertThat(DropdownValueService.CATEGORY_CLIENT).isEqualTo("CLIENT");
            assertThat(DropdownValueService.CATEGORY_PROJECT).isEqualTo("PROJECT");
            assertThat(DropdownValueService.CATEGORY_PHASE).isEqualTo("PHASE");
        }
    }
}
