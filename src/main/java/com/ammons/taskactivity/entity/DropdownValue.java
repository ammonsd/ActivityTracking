package com.ammons.taskactivity.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DropdownValue Entity
 *
 * Modified by: Dean Ammons - February 2026 Change: Added allUsers field to support client/project
 * dropdown access control Reason: Values flagged allUsers=true are visible to all users without
 * requiring a row in user_dropdown_access
 *
 * @author Dean Ammons
 * @version 1.0
 * @since November 2025
 */
@Entity
@Table(name = "dropdownvalues", schema = "public")
public class DropdownValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotBlank(message = "Category is required")
    @Size(max = 50, message = "Category cannot exceed 50 characters")
    @Column(name = "category", nullable = false, length = 50)
    private String category; // CLIENT, PROJECT, or PHASE

    @NotBlank(message = "Subcategory is required")
    @Size(max = 50, message = "Subcategory cannot exceed 50 characters")
    @Column(name = "subcategory", nullable = false, length = 50)
    private String subcategory;

    @NotBlank(message = "Value is required")
    @Size(max = 255, message = "Value cannot exceed 255 characters")
    @Column(name = "itemvalue", nullable = false, length = 255)
    private String itemValue;

    @Column(name = "displayorder", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "isactive", nullable = false)
    private Boolean isActive = true;

    @Column(name = "non_billable", nullable = false)
    private Boolean nonBillable = false;

    @Column(name = "all_users", nullable = false)
    private Boolean allUsers = false;

    public DropdownValue() {}

    public DropdownValue(String category, String subcategory, String itemValue,
            Integer displayOrder,
            Boolean isActive) {
        this.category = category;
        this.subcategory = subcategory;
        this.itemValue = itemValue;
        this.displayOrder = displayOrder;
        this.isActive = isActive;
        this.nonBillable = false; // Default to billable
    }

    public DropdownValue(String category, String subcategory, String itemValue,
            Integer displayOrder, Boolean isActive, Boolean nonBillable) {
        this.category = category;
        this.subcategory = subcategory;
        this.itemValue = itemValue;
        this.displayOrder = displayOrder;
        this.isActive = isActive;
        this.nonBillable = nonBillable;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSubcategory() {
        return subcategory;
    }

    public void setSubcategory(String subcategory) {
        this.subcategory = subcategory;
    }

    public String getItemValue() {
        return itemValue;
    }

    public void setItemValue(String itemValue) {
        this.itemValue = itemValue;
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

    public Boolean getNonBillable() {
        return nonBillable;
    }

    public void setNonBillable(Boolean nonBillable) {
        this.nonBillable = nonBillable;
    }

    public Boolean getAllUsers() {
        return allUsers;
    }

    public void setAllUsers(Boolean allUsers) {
        this.allUsers = allUsers;
    }

    @Override
    public String toString() {
        return "DropdownValue{" + "id=" + id + ", category='" + category + '\'' + ", subcategory='"
                + subcategory + '\'' + ", itemValue='" + itemValue + '\'' + ", displayOrder="
                + displayOrder + ", isActive=" + isActive + ", nonBillable=" + nonBillable
                + ", allUsers=" + allUsers + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof DropdownValue))
            return false;
        DropdownValue that = (DropdownValue) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
