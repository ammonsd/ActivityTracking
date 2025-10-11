package com.ammons.taskactivity.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DropdownValue Entity
 *
 * @author Dean Ammons
 * @version 1.0
 */
@Entity
@Table(name = "\"DropdownValues\"", schema = "dbo")
public class DropdownValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "\"RECID\"")
    private Long recId;

    @NotBlank(message = "Category is required")
    @Size(max = 50, message = "Category cannot exceed 50 characters")
    @Column(name = "\"category\"", nullable = false, length = 50)
    private String category; // CLIENT, PROJECT, or PHASE

    @NotBlank(message = "Value is required")
    @Size(max = 255, message = "Value cannot exceed 255 characters")
    @Column(name = "\"value\"", nullable = false, length = 255)
    private String value;

    @Column(name = "\"displayOrder\"", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "\"isActive\"", nullable = false)
    private Boolean isActive = true;

    public DropdownValue() {}

    public DropdownValue(String category, String value, Integer displayOrder, Boolean isActive) {
        this.category = category;
        this.value = value;
        this.displayOrder = displayOrder;
        this.isActive = isActive;
    }

    public Long getRecId() {
        return recId;
    }

    public void setRecId(Long recId) {
        this.recId = recId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    @Override
    public String toString() {
        return "DropdownValue{" + "recId=" + recId + ", category='" + category + '\'' + ", value='"
                + value + '\'' + ", displayOrder=" + displayOrder + ", isActive=" + isActive + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof DropdownValue))
            return false;
        DropdownValue that = (DropdownValue) o;
        return recId != null && recId.equals(that.recId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
