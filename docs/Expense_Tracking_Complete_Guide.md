# Expense Tracking Feature - Complete Implementation Guide

**Document Version:** 2.0  
**Date:** November 14, 2025  
**Last Reviewed:** February 5, 2026  
**Author:** System Analysis Based on Task Activity Management Pattern  
**Status:** Implementation Guide - Current and Accurate

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Application Purpose & Scope](#application-purpose--scope)
3. [Dropdown Subcategories](#dropdown-subcategories)
4. [Database Schema](#database-schema)
5. [Backend Implementation](#backend-implementation)
6. [Frontend Implementation](#frontend-implementation)
7. [Security & Access Control](#security--access-control)
8. [Testing Strategy](#testing-strategy)
9. [Implementation Phases](#implementation-phases)
10. [Questions for Consideration](#questions-for-consideration)
11. [Implementation Checklist](#implementation-checklist)

---

## Executive Summary

This document provides a comprehensive implementation plan for adding expense tracking functionality to the Task Activity Management System. The Expense Tracking add-on mirrors the Task Activity application's purpose: just as the Task Activity app allows team members to record their project hours by client for entry into the corporate timesheet system, the Expense app allows team members to track their **travel expenses and home office expenses** by client and project for reimbursement and entry into the corporate expense system.

### Key Deliverables

- Complete expense management backend (Entity, Repository, Service, Controllers)
- Angular frontend components (List, Add/Edit Dialog, Weekly Expenses, Dashboard)
- Database schema with indexes and dropdown values
- REST API endpoints with filtering and pagination
- Dropdown value management for expense types, payment methods, and statuses
- Status workflow (Draft → Submitted → Approved → Reimbursed)
- Receipt upload and management capability
- Comprehensive unit and integration tests

---

## Application Purpose & Scope

### What This Application IS

**Purpose**: A user-facing expense entry and tracking tool to simplify the expense report process for team members.

**Primary Functions**:

- **Travel Expense Tracking**: Team members record travel expenses (airfare, hotel, meals, transportation, etc.) associated with client projects
- **Home Office Expense Tracking**: Remote users can enter home office expenses (internet, office supplies, equipment, etc.)
- **Corporate System Integration**: Like the weekly timesheet entry, this provides a staging area for users to organize their expenses before entering them into the corporate expense system
- **Status Tracking**: Users can track their expense status (Draft, Submitted, Approved, Reimbursed) throughout the reimbursement process
- **Receipt Management**: Users can attach receipt images to their expenses for documentation

### What This Application IS NOT

- **NOT a billing system**: While expenses are associated with clients/projects, this app does NOT handle client billing or invoicing
- **NOT an accounting system**: No accounts payable, general ledger, or financial reporting beyond basic expense tracking
- **NOT a corporate account system**: While expenses may eventually go to a corporate account table, we are not managing corporate accounting functions
- **NOT a tax management system**: Tax categorization is handled by the corporate system

### Parallel with Task Activity App

| Task Activity App                     | Expense Tracking App                                     |
| ------------------------------------- | -------------------------------------------------------- |
| Team members track project hours      | Team members track travel & home office expenses         |
| By client and project                 | By client and project                                    |
| Enter into corporate timesheet system | Enter into corporate expense system                      |
| Status tracking (Draft, Submitted)    | Status tracking (Draft, Submitted, Approved, Reimbursed) |

---

## Dropdown Subcategories

The expense tracking system uses the existing `dropdownvalues` table with the EXPENSE category and the following subcategories:

### 1. EXPENSE_TYPE

**Purpose**: Classify the nature/purpose of the expense

**Values**:

**Travel Expenses:**

- Travel - Airfare
- Travel - Hotel
- Travel - Ground Transportation
- Travel - Rental Car
- Travel - Parking
- Travel - Mileage
- Travel - Meals (Client Meeting)
- Travel - Meals (Travel Days)
- Travel - Other

**Home Office Expenses (Remote Workers):**

- Home Office - Internet
- Home Office - Phone/Mobile
- Home Office - Office Supplies
- Home Office - Equipment
- Home Office - Furniture
- Home Office - Software/Subscriptions
- Home Office - Utilities (Portion)
- Home Office - Other

**Other Business Expenses:**

- Training/Education
- Professional Development
- Miscellaneous

---

### 2. PAYMENT_METHOD

**Purpose**: Track how the expense was paid

**Values**:

- Corporate Credit Card
- Personal Credit Card
- Cash
- Check
- Direct Debit
- Reimbursement Due

**Rationale**: Essential for reconciliation and reimbursement tracking. Distinguishing between corporate and personal cards is important for expense report processing.

---

### 3. EXPENSE_STATUS

**Purpose**: Track the expense through the reimbursement workflow

**Values**:

- Draft
- Submitted
- Pending Approval
- Approved
- Rejected - Needs Revision
- Resubmitted
- Approved - Pending Reimbursement
- Reimbursed
- Entered in Corporate System

**Rationale**: This is critical for allowing users to track where their expenses are in the approval and reimbursement process. Provides transparency throughout the expense lifecycle.

---

### 4. VENDOR (Optional)

**Purpose**: Track the payee/merchant (optional for most entries)

**Values**:

- United Airlines
- Delta Airlines
- American Airlines
- Southwest Airlines
- Marriott
- Hilton
- Hyatt
- Holiday Inn
- Uber
- Lyft
- Enterprise Rent-A-Car
- Hertz
- Budget
- Shell
- BP
- Exxon
- Various Restaurants
- Various Local Vendors
- Other

**Rationale**: Vendor tracking can be useful for pattern recognition. Should be optional or allow freeform text entry for user convenience.

---

### 5. RECEIPT_STATUS (Optional)

**Purpose**: Track receipt documentation status

**Values**:

- No Receipt Required
- Receipt Attached
- Receipt Pending
- Receipt Missing
- Receipt Uploaded to Corporate System

---

### 6. CLIENT_PROJECT_ASSOCIATION

**Purpose**: Link expenses to clients and projects for corporate system entry

**Implementation**:

- Use existing CLIENT dropdown values
- Use existing TASK/PROJECT dropdown values

**Rationale**: Expenses need to be associated with specific clients and projects for accurate corporate system entry and proper expense allocation.

---

### SQL Insert Script for Dropdown Values

```sql
-- Insert EXPENSE subcategories
-- Run this AFTER the main dropdown migration

INSERT INTO dropdownvalues (category, subcategory, itemvalue, displayorder, isactive)
VALUES
-- EXPENSE_TYPE subcategory - Travel Expenses
('EXPENSE', 'EXPENSE_TYPE', 'Travel - Airfare', 1, true),
('EXPENSE', 'EXPENSE_TYPE', 'Travel - Hotel', 2, true),
('EXPENSE', 'EXPENSE_TYPE', 'Travel - Ground Transportation', 3, true),
('EXPENSE', 'EXPENSE_TYPE', 'Travel - Rental Car', 4, true),
('EXPENSE', 'EXPENSE_TYPE', 'Travel - Parking', 5, true),
('EXPENSE', 'EXPENSE_TYPE', 'Travel - Mileage', 6, true),
('EXPENSE', 'EXPENSE_TYPE', 'Travel - Meals (Client Meeting)', 7, true),
('EXPENSE', 'EXPENSE_TYPE', 'Travel - Meals (Travel Days)', 8, true),
('EXPENSE', 'EXPENSE_TYPE', 'Travel - Other', 9, true),

-- EXPENSE_TYPE subcategory - Home Office Expenses
('EXPENSE', 'EXPENSE_TYPE', 'Home Office - Internet', 10, true),
('EXPENSE', 'EXPENSE_TYPE', 'Home Office - Phone/Mobile', 11, true),
('EXPENSE', 'EXPENSE_TYPE', 'Home Office - Office Supplies', 12, true),
('EXPENSE', 'EXPENSE_TYPE', 'Home Office - Equipment', 13, true),
('EXPENSE', 'EXPENSE_TYPE', 'Home Office - Furniture', 14, true),
('EXPENSE', 'EXPENSE_TYPE', 'Home Office - Software/Subscriptions', 15, true),
('EXPENSE', 'EXPENSE_TYPE', 'Home Office - Utilities (Portion)', 16, true),
('EXPENSE', 'EXPENSE_TYPE', 'Home Office - Other', 17, true),

-- EXPENSE_TYPE subcategory - Other Business Expenses
('EXPENSE', 'EXPENSE_TYPE', 'Training/Education', 18, true),
('EXPENSE', 'EXPENSE_TYPE', 'Professional Development', 19, true),
('EXPENSE', 'EXPENSE_TYPE', 'Miscellaneous', 20, true),

-- PAYMENT_METHOD subcategory
('EXPENSE', 'PAYMENT_METHOD', 'Corporate Credit Card', 1, true),
('EXPENSE', 'PAYMENT_METHOD', 'Personal Credit Card', 2, true),
('EXPENSE', 'PAYMENT_METHOD', 'Cash', 3, true),
('EXPENSE', 'PAYMENT_METHOD', 'Check', 4, true),
('EXPENSE', 'PAYMENT_METHOD', 'Direct Debit', 5, true),
('EXPENSE', 'PAYMENT_METHOD', 'Reimbursement Due', 6, true),

-- EXPENSE_STATUS subcategory
('EXPENSE', 'EXPENSE_STATUS', 'Draft', 1, true),
('EXPENSE', 'EXPENSE_STATUS', 'Submitted', 2, true),
('EXPENSE', 'EXPENSE_STATUS', 'Pending Approval', 3, true),
('EXPENSE', 'EXPENSE_STATUS', 'Approved', 4, true),
('EXPENSE', 'EXPENSE_STATUS', 'Rejected', 5, true),
('EXPENSE', 'EXPENSE_STATUS', 'Resubmitted', 6, true),
('EXPENSE', 'EXPENSE_STATUS', 'Reimbursed', 7, true),

-- VENDOR subcategory
('EXPENSE', 'VENDOR', 'Amazon', 1, true),
('EXPENSE', 'VENDOR', 'Delta Airlines', 2, true),
('EXPENSE', 'VENDOR', 'United Airlines', 3, true),
('EXPENSE', 'VENDOR', 'Hilton', 4, true),
('EXPENSE', 'VENDOR', 'Marriott', 5, true),
('EXPENSE', 'VENDOR', 'Uber', 6, true),
('EXPENSE', 'VENDOR', 'Lyft', 7, true),
('EXPENSE', 'VENDOR', 'Enterprise', 8, true),
('EXPENSE', 'VENDOR', 'Hertz', 9, true),
('EXPENSE', 'VENDOR', 'Staples', 10, true),
('EXPENSE', 'VENDOR', 'Office Depot', 11, true),
('EXPENSE', 'VENDOR', 'Other', 12, true),

-- RECEIPT_STATUS subcategory
('EXPENSE', 'RECEIPT_STATUS', 'No Receipt', 1, true),
('EXPENSE', 'RECEIPT_STATUS', 'Receipt Uploaded', 2, true),
('EXPENSE', 'RECEIPT_STATUS', 'Receipt Pending', 3, true),
('EXPENSE', 'RECEIPT_STATUS', 'Receipt Missing', 4, true),

-- CLIENT subcategory (shares with TASK)
('EXPENSE', 'CLIENT', 'Corporate', 1, true),

-- PROJECT subcategory (shares with TASK)
('EXPENSE', 'PROJECT', 'General Administration', 1, true),
('EXPENSE', 'PROJECT', 'Non-Billable', 2, true)
ON CONFLICT (category, subcategory, itemvalue) DO NOTHING;
```

---

## Database Schema

### Recommended Schema

```sql
CREATE TABLE expenses (
    -- Core & Identification
    id                  BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    username            VARCHAR(50) NOT NULL,

    -- Client/Project Association (Like Task Activity)
    client              VARCHAR(50) NOT NULL,
    project             VARCHAR(50),  -- Optional, some expenses may not tie to specific project

    -- Expense Details
    expense_date        DATE NOT NULL,
    expense_type        VARCHAR(50) NOT NULL,  -- From EXPENSE/EXPENSE_TYPE dropdown
    description         VARCHAR(255) NOT NULL,
    amount              NUMERIC(10, 2) NOT NULL CHECK (amount > 0),
    currency            VARCHAR(3) DEFAULT 'USD',

    -- Payment & Vendor Information
    payment_method      VARCHAR(50) NOT NULL,  -- From EXPENSE/PAYMENT_METHOD dropdown
    vendor              VARCHAR(100),          -- Vendor name (freeform or from dropdown)
    reference_number    VARCHAR(50),           -- Receipt/confirmation number

    -- Receipt Management
    receipt_path        VARCHAR(500),          -- File path to uploaded receipt image
    receipt_status      VARCHAR(50),           -- From EXPENSE/RECEIPT_STATUS dropdown

    -- Status Tracking (Critical for User Visibility)
    expense_status      VARCHAR(50) NOT NULL DEFAULT 'Draft',  -- From EXPENSE/EXPENSE_STATUS

    -- Approval Details
    approved_by         VARCHAR(100),
    approval_date       DATE,
    approval_notes      VARCHAR(500),          -- Approval/rejection comments

    -- Reimbursement Tracking
    reimbursed_amount   NUMERIC(10, 2) CHECK (reimbursed_amount >= 0 AND reimbursed_amount <= amount),
    reimbursement_date  DATE,
    reimbursement_notes VARCHAR(500),

    -- Additional Notes
    notes               VARCHAR(500),          -- User's additional notes

    -- Auditing (Auto-managed timestamps)
    created_date        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by    VARCHAR(100),

    -- Foreign Key Constraints
    CONSTRAINT fk_expense_username
        FOREIGN KEY (username)
        REFERENCES users(username)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    -- Business Logic Constraints
    CONSTRAINT chk_approval_logic
        CHECK (
            (approval_date IS NULL AND approved_by IS NULL) OR
            (approval_date IS NOT NULL AND approved_by IS NOT NULL)
        ),
    CONSTRAINT chk_reimbursement_logic
        CHECK (
            (reimbursement_date IS NULL) OR
            (reimbursement_date IS NOT NULL AND expense_status = 'Reimbursed')
        )
);

-- Performance Indexes
CREATE INDEX idx_expenses_username ON expenses (username);
CREATE INDEX idx_expenses_date ON expenses (expense_date);
CREATE INDEX idx_expenses_client ON expenses (client);
CREATE INDEX idx_expenses_project ON expenses (project);
CREATE INDEX idx_expenses_status ON expenses (expense_status);
CREATE INDEX idx_expenses_type ON expenses (expense_type);
CREATE INDEX idx_expenses_payment_method ON expenses (payment_method);

-- Combined indexes for common queries
CREATE INDEX idx_expenses_user_date ON expenses (username, expense_date DESC);
CREATE INDEX idx_expenses_user_status ON expenses (username, expense_status);
CREATE INDEX idx_expenses_client_project ON expenses (client, project);

-- Comments for documentation
COMMENT ON TABLE expenses IS 'Travel and home office expense tracking with approval and reimbursement workflow';
COMMENT ON COLUMN expenses.expense_type IS 'Type of expense from EXPENSE/EXPENSE_TYPE dropdown (travel, home office, etc.)';
COMMENT ON COLUMN expenses.expense_status IS 'Workflow status from EXPENSE/EXPENSE_STATUS dropdown (Draft, Submitted, Approved, Reimbursed, etc.)';
COMMENT ON COLUMN expenses.payment_method IS 'How expense was paid from EXPENSE/PAYMENT_METHOD dropdown';
COMMENT ON COLUMN expenses.currency IS 'ISO 4217 currency code (e.g., USD, EUR, GBP)';
COMMENT ON COLUMN expenses.receipt_path IS 'File system path or URL to uploaded receipt image';
```

### Key Schema Decisions

1. **NO `isBillable` field** - This is not a billing system
2. **`expense_status` field** - Tracks the complete lifecycle from Draft to Reimbursed
3. **`receipt_path` and `receipt_status`** - Support for receipt management
4. **Client/Project association** - For organization, NOT for billing
5. **Approval and Reimbursement tracking** - Separate fields for each workflow stage
6. **Audit trail** - Created date, last modified, and last modified by

---

## Receipt Storage Implementation

### Overview

Receipt images will be stored in **AWS S3** for production and **local file system** for development/testing. This approach provides:

- Scalable, reliable cloud storage
- Low cost (estimated $0.10-$0.50/month for typical usage)
- Easy local development without AWS dependencies
- Leverages existing AWS infrastructure (ECS task IAM role)

### Storage Strategy

**Development/Testing**: Local file system (`c:/Task Activity/Receipts`)  
**Production**: AWS S3 bucket (`taskactivity-receipts-prod`)

### AWS Setup

#### 1. Create S3 Bucket

**Option A: AWS CLI** (if you have appropriate IAM permissions)

```bash
# Create the bucket
aws s3 mb s3://taskactivity-receipts-prod --region us-east-1

# Enable versioning (optional but recommended)
aws s3api put-bucket-versioning --bucket taskactivity-receipts-prod --versioning-configuration Status=Enabled

# Enable encryption
aws s3api put-bucket-encryption --bucket taskactivity-receipts-prod --server-side-encryption-configuration '{\"Rules\":[{\"ApplyServerSideEncryptionByDefault\":{\"SSEAlgorithm\":\"AES256\"}}]}'

# Block public access
aws s3api put-public-access-block --bucket taskactivity-receipts-prod --public-access-block-configuration BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true
```

**Option B: AWS Console** (recommended if CLI permissions are restricted)

1. Go to **S3** in AWS Console
2. Click **Create bucket**
3. Bucket name: `taskactivity-receipts-prod`
4. Region: `us-east-1`
5. Under **Block Public Access settings**:
    - ☑ Block all public access (ensure all 4 checkboxes are selected)
6. Under **Bucket Versioning**:
    - Select **Enable** (recommended for data protection)
7. Under **Default encryption**:
    - Encryption type: **Server-side encryption with Amazon S3 managed keys (SSE-S3)**
    - Bucket Key: **Enable** (reduces costs)
8. Click **Create bucket**
9. After creation, go to bucket → **Management** tab → **Create lifecycle rule** (optional):
    - Rule name: `DeleteOldReceipts` or `ArchiveOldReceipts`
    - Choose scope: Apply to all objects
    - Lifecycle rule actions:
        - ☑ Expire current versions of objects: 365 days (or as needed)
        - Optionally check "Permanently delete noncurrent versions" with 30 days
    - Click **Create rule**

#### 2. Update IAM Task Role

Add S3 permissions to your existing `taskactivity-task-role` (the role your ECS task already uses):

**Option A: AWS CLI** (if you have appropriate IAM permissions)

**File**: `aws/taskactivity-s3-receipts-policy.json`

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "ReceiptObjectAccess",
            "Effect": "Allow",
            "Action": ["s3:PutObject", "s3:GetObject", "s3:DeleteObject"],
            "Resource": "arn:aws:s3:::taskactivity-receipts-prod/*"
        },
        {
            "Sid": "ReceiptBucketAccess",
            "Effect": "Allow",
            "Action": ["s3:ListBucket", "s3:GetBucketLocation"],
            "Resource": "arn:aws:s3:::taskactivity-receipts-prod"
        }
    ]
}
```

Create and attach the policy:

```bash
# Create the policy
aws iam create-policy --policy-name TaskActivityS3ReceiptsPolicy --policy-document file://aws/taskactivity-s3-receipts-policy.json

# Attach to your existing task role (replace YOUR_ACCOUNT_ID)
aws iam attach-role-policy --role-name taskactivity-task-role --policy-arn arn:aws:iam::YOUR_ACCOUNT_ID:policy/TaskActivityS3ReceiptsPolicy
```

**Option B: AWS Console** (recommended if CLI permissions are restricted or role doesn't exist)

**Step 1: Create the IAM Role (if it doesn't exist)**

1. Go to **IAM** → **Roles** → **Create role**
2. Trusted entity type: **AWS service**
3. Use case: **Elastic Container Service** → **Elastic Container Service Task**
4. Click **Next**
5. Search for and select these policies:
    - `AmazonECSTaskExecutionRolePolicy` (for ECS to run containers)
    - Click **Next** (we'll add S3 policy next)
6. Role name: `taskactivity-task-role`
7. Description: "IAM role for TaskActivity ECS tasks to access S3 receipts bucket"
8. Click **Create role**

**Step 2: Create the S3 Policy**

1. Go to **IAM** → **Policies** → **Create policy**
2. Click **JSON** tab
3. Paste the JSON policy above (from `taskactivity-s3-receipts-policy.json`)
4. Click **Next**
5. Policy name: `TaskActivityS3ReceiptsPolicy`
6. Description: "Allows TaskActivity app to manage receipt files in S3"
7. Click **Create policy**

**Step 3: Attach Policy to Role**

1. Go to **IAM** → **Roles**
2. Search for and click `taskactivity-task-role`
3. Click **Permissions** tab
4. Click **Add permissions** → **Attach policies**
5. Search for `TaskActivityS3ReceiptsPolicy`
6. Check the box next to it
7. Click **Add permissions**

**Step 4: Verify Role Configuration**
Your `taskactivity-task-role` should now have these policies attached:

- `AmazonECSTaskExecutionRolePolicy` (for ECS operations)
- `TaskActivityS3ReceiptsPolicy` (for S3 receipt access)

**Note**: This single role can be used for both Task Role and Task Execution Role in your ECS task definition.

#### 3. S3 Lifecycle Policy (Optional - Cost Optimization)

Archive old receipts to cheaper storage after 1 year:

**Option A: AWS CLI**

**File**: `aws/s3-lifecycle-policy.json`

```json
{
    "Rules": [
        {
            "Id": "ArchiveOldReceipts",
            "Status": "Enabled",
            "Transitions": [
                {
                    "Days": 365,
                    "StorageClass": "GLACIER_IR"
                }
            ]
        }
    ]
}
```

Apply:

```bash
aws s3api put-bucket-lifecycle-configuration --bucket taskactivity-receipts-prod --lifecycle-configuration file://aws/s3-lifecycle-policy.json
```

**Option B: AWS Console**

This was already covered in Step 1, Option B above (lifecycle rule creation).

### Maven Dependency

Add AWS SDK for S3 to `pom.xml`:

```xml
<!-- AWS S3 SDK for receipt storage -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
    <version>2.20.26</version>
</dependency>
```

### Configuration

**application.properties (Development)**:

```properties
# Storage Configuration
storage.type=local
storage.local.path=c:/Task Activity/Receipts

# File Upload Limits
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=5MB

# Allowed file types
storage.allowed-types=image/jpeg,image/png,image/jpg,application/pdf
```

**application.properties (Production)**:

```properties
# Storage Configuration
storage.type=s3
storage.s3.bucket=taskactivity-receipts-prod
storage.s3.region=us-east-1

# File Upload Limits
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=5MB

# Allowed file types
storage.allowed-types=image/jpeg,image/png,image/jpg,application/pdf
```

### Storage Service Interface

**File**: `src/main/java/com/ammons/taskactivity/service/ReceiptStorageService.java`

```java
package com.ammons.taskactivity.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;

/**
 * Interface for receipt storage operations.
 * Implementations can use local file system or cloud storage (S3).
 *
 * @author Dean Ammons
 * @version 1.0
 */
public interface ReceiptStorageService {

    /**
     * Store a receipt file and return the storage path/key
     *
     * @param file The uploaded file
     * @param username The username of the expense owner
     * @param expenseId The expense ID
     * @return The storage path/key (relative path for local, S3 key for S3)
     * @throws IOException if storage fails
     */
    String storeReceipt(MultipartFile file, String username, Long expenseId) throws IOException;

    /**
     * Retrieve a receipt file as an InputStream
     *
     * @param receiptPath The storage path/key
     * @return InputStream of the file
     * @throws IOException if retrieval fails
     */
    InputStream getReceipt(String receiptPath) throws IOException;

    /**
     * Delete a receipt file
     *
     * @param receiptPath The storage path/key
     * @throws IOException if deletion fails
     */
    void deleteReceipt(String receiptPath) throws IOException;

    /**
     * Check if a receipt exists
     *
     * @param receiptPath The storage path/key
     * @return true if exists, false otherwise
     */
    boolean receiptExists(String receiptPath);

    /**
     * Get the content type of a receipt
     *
     * @param receiptPath The storage path/key
     * @return Content type (e.g., "image/jpeg")
     */
    String getContentType(String receiptPath);
}
```

### Local File Storage Implementation

**File**: `src/main/java/com/ammons/taskactivity/service/LocalFileStorageService.java`

```java
package com.ammons.taskactivity.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Local file system implementation of ReceiptStorageService
 * Used for development and testing
 *
 * @author Dean Ammons
 * @version 1.0
 */
public class LocalFileStorageService implements ReceiptStorageService {

    private static final Logger logger = LoggerFactory.getLogger(LocalFileStorageService.class);

    private final Path rootLocation;

    public LocalFileStorageService(@Value("${storage.local.path}") String storagePath) {
        this.rootLocation = Paths.get(storagePath);
        try {
            Files.createDirectories(this.rootLocation);
            logger.info("Local storage initialized at: {}", this.rootLocation.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage directory", e);
        }
    }

    @Override
    public String storeReceipt(MultipartFile file, String username, Long expenseId) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("Cannot store empty file");
        }

        // Create directory structure: username/YYYY/MM/
        LocalDate now = LocalDate.now();
        String userDir = username;
        String yearDir = String.valueOf(now.getYear());
        String monthDir = String.format("%02d", now.getMonthValue());

        Path targetDir = rootLocation.resolve(userDir).resolve(yearDir).resolve(monthDir);
        Files.createDirectories(targetDir);

        // Generate unique filename: receipt_expenseId_uuid.ext
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
        String filename = String.format("receipt_%d_%s%s", expenseId, UUID.randomUUID(), extension);

        Path targetFile = targetDir.resolve(filename);

        // Copy file
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
        }

        // Return relative path: username/YYYY/MM/filename
        String relativePath = userDir + "/" + yearDir + "/" + monthDir + "/" + filename;
        logger.info("Stored receipt locally: {}", relativePath);

        return relativePath;
    }

    @Override
    public InputStream getReceipt(String receiptPath) throws IOException {
        Path file = rootLocation.resolve(receiptPath);
        if (!Files.exists(file)) {
            throw new FileNotFoundException("Receipt not found: " + receiptPath);
        }
        return Files.newInputStream(file);
    }

    @Override
    public void deleteReceipt(String receiptPath) throws IOException {
        Path file = rootLocation.resolve(receiptPath);
        Files.deleteIfExists(file);
        logger.info("Deleted receipt: {}", receiptPath);
    }

    @Override
    public boolean receiptExists(String receiptPath) {
        Path file = rootLocation.resolve(receiptPath);
        return Files.exists(file);
    }

    @Override
    public String getContentType(String receiptPath) {
        try {
            Path file = rootLocation.resolve(receiptPath);
            return Files.probeContentType(file);
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }
}
```

### S3 Storage Implementation

**File**: `src/main/java/com/ammons/taskactivity/service/S3StorageService.java`

```java
package com.ammons.taskactivity.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.UUID;

/**
 * AWS S3 implementation of ReceiptStorageService
 * Used in production - leverages ECS task IAM role for authentication
 *
 * @author Dean Ammons
 * @version 1.0
 */
public class S3StorageService implements ReceiptStorageService {

    private static final Logger logger = LoggerFactory.getLogger(S3StorageService.class);

    private final S3Client s3Client;
    private final String bucketName;

    public S3StorageService(
            @Value("${storage.s3.bucket}") String bucketName,
            @Value("${storage.s3.region}") String region) {
        this.bucketName = bucketName;
        // S3Client automatically uses ECS task IAM role - no credentials needed!
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .build();
        logger.info("S3 storage initialized for bucket: {} in region: {}", bucketName, region);
    }

    @Override
    public String storeReceipt(MultipartFile file, String username, Long expenseId) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("Cannot store empty file");
        }

        // Create S3 key structure: username/YYYY/MM/filename
        LocalDate now = LocalDate.now();
        String userDir = username;
        String yearDir = String.valueOf(now.getYear());
        String monthDir = String.format("%02d", now.getMonthValue());

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
        String filename = String.format("receipt_%d_%s%s", expenseId, UUID.randomUUID(), extension);

        String s3Key = userDir + "/" + yearDir + "/" + monthDir + "/" + filename;

        // Upload to S3
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            logger.info("Stored receipt in S3: s3://{}/{}", bucketName, s3Key);
            return s3Key;

        } catch (S3Exception e) {
            logger.error("Failed to store receipt in S3: {}", e.getMessage());
            throw new IOException("Failed to store receipt in S3", e);
        }
    }

    @Override
    public InputStream getReceipt(String receiptPath) throws IOException {
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(receiptPath)
                    .build();

            return s3Client.getObject(getRequest);

        } catch (NoSuchKeyException e) {
            throw new IOException("Receipt not found: " + receiptPath, e);
        } catch (S3Exception e) {
            logger.error("Failed to retrieve receipt from S3: {}", e.getMessage());
            throw new IOException("Failed to retrieve receipt from S3", e);
        }
    }

    @Override
    public void deleteReceipt(String receiptPath) throws IOException {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(receiptPath)
                    .build();

            s3Client.deleteObject(deleteRequest);
            logger.info("Deleted receipt from S3: {}", receiptPath);

        } catch (S3Exception e) {
            logger.error("Failed to delete receipt from S3: {}", e.getMessage());
            throw new IOException("Failed to delete receipt from S3", e);
        }
    }

    @Override
    public boolean receiptExists(String receiptPath) {
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(receiptPath)
                    .build();

            s3Client.headObject(headRequest);
            return true;

        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            logger.error("Error checking receipt existence: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getContentType(String receiptPath) {
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(receiptPath)
                    .build();

            HeadObjectResponse response = s3Client.headObject(headRequest);
            return response.contentType();

        } catch (S3Exception e) {
            logger.error("Error getting content type: {}", e.getMessage());
            return "application/octet-stream";
        }
    }
}
```

### Storage Configuration Bean

**File**: `src/main/java/com/ammons/taskactivity/config/StorageConfig.java`

```java
package com.ammons.taskactivity.config;

import com.ammons.taskactivity.service.LocalFileStorageService;
import com.ammons.taskactivity.service.ReceiptStorageService;
import com.ammons.taskactivity.service.S3StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for receipt storage
 * Switches between local and S3 storage based on configuration
 *
 * @author Dean Ammons
 * @version 1.0
 */
@Configuration
public class StorageConfig {

    @Value("${storage.type:local}")
    private String storageType;

    @Bean
    public ReceiptStorageService receiptStorageService(
            @Value("${storage.local.path:C:/Task Activity/Receipts}") String localPath,
            @Value("${storage.s3.bucket:taskactivity-receipts-prod}") String s3Bucket,
            @Value("${storage.s3.region:us-east-1}") String s3Region) {

        if ("s3".equalsIgnoreCase(storageType)) {
            return new S3StorageService(s3Bucket, s3Region);
        } else {
            return new LocalFileStorageService(localPath);
        }
    }
}
```

### Receipt Upload Controller

**File**: `src/main/java/com/ammons/taskactivity/controller/ReceiptController.java`

```java
package com.ammons.taskactivity.controller;

import com.ammons.taskactivity.dto.ApiResponse;
import com.ammons.taskactivity.entity.Expense;
import com.ammons.taskactivity.service.ExpenseService;
import com.ammons.taskactivity.service.ReceiptStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * REST Controller for receipt upload and download
 *
 * @author Dean Ammons
 * @version 1.0
 */
@RestController
@RequestMapping("/api/receipts")
public class ReceiptController {

    private static final Logger logger = LoggerFactory.getLogger(ReceiptController.class);
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "application/pdf"
    );

    private final ReceiptStorageService storageService;
    private final ExpenseService expenseService;

    public ReceiptController(ReceiptStorageService storageService, ExpenseService expenseService) {
        this.storageService = storageService;
        this.expenseService = expenseService;
    }

    /**
     * Upload a receipt for an expense
     */
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'GUEST')")
    @PostMapping("/{expenseId}")
    public ResponseEntity<ApiResponse<String>> uploadReceipt(
            @PathVariable Long expenseId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        String username = authentication.getName();
        logger.info("User {} uploading receipt for expense {}", username, expenseId);

        // Validate expense exists and user owns it (or is admin)
        Optional<Expense> expenseOpt = expenseService.getExpenseById(expenseId);
        if (expenseOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Expense not found", null));
        }

        Expense expense = expenseOpt.get();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !expense.getUsername().equals(username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Not authorized to upload receipt for this expense", null));
        }

        // Validate file
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("File is empty", null));
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("File size exceeds maximum of 5MB", null));
        }

        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("File type not allowed. Please upload JPG, PNG, or PDF", null));
        }

        try {
            // Delete old receipt if exists
            if (expense.getReceiptPath() != null && !expense.getReceiptPath().isEmpty()) {
                try {
                    storageService.deleteReceipt(expense.getReceiptPath());
                } catch (IOException e) {
                    logger.warn("Failed to delete old receipt: {}", e.getMessage());
                }
            }

            // Store new receipt
            String receiptPath = storageService.storeReceipt(file, username, expenseId);

            // Update expense with receipt path
            expense.setReceiptPath(receiptPath);
            expense.setReceiptStatus("Receipt Attached");
            expenseService.updateExpense(expenseId, convertToDto(expense));

            logger.info("Receipt uploaded successfully for expense {}: {}", expenseId, receiptPath);

            return ResponseEntity.ok(ApiResponse.success("Receipt uploaded successfully", receiptPath));

        } catch (IOException e) {
            logger.error("Failed to upload receipt: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to upload receipt: " + e.getMessage(), null));
        }
    }

    /**
     * Download a receipt
     */
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'GUEST')")
    @GetMapping("/{expenseId}")
    public ResponseEntity<?> downloadReceipt(
            @PathVariable Long expenseId,
            Authentication authentication) {

        String username = authentication.getName();

        // Validate expense exists and user can access it
        Optional<Expense> expenseOpt = expenseService.getExpenseById(expenseId);
        if (expenseOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Expense not found", null));
        }

        Expense expense = expenseOpt.get();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !expense.getUsername().equals(username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Not authorized to view this receipt", null));
        }

        if (expense.getReceiptPath() == null || expense.getReceiptPath().isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("No receipt attached to this expense", null));
        }

        try {
            InputStream receiptStream = storageService.getReceipt(expense.getReceiptPath());
            String contentType = storageService.getContentType(expense.getReceiptPath());

            // Extract filename from path
            String filename = expense.getReceiptPath().substring(
                    expense.getReceiptPath().lastIndexOf('/') + 1
            );

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(new InputStreamResource(receiptStream));

        } catch (IOException e) {
            logger.error("Failed to download receipt: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to download receipt: " + e.getMessage(), null));
        }
    }

    /**
     * Delete a receipt
     */
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @DeleteMapping("/{expenseId}")
    public ResponseEntity<ApiResponse<Void>> deleteReceipt(
            @PathVariable Long expenseId,
            Authentication authentication) {

        String username = authentication.getName();
        logger.info("User {} deleting receipt for expense {}", username, expenseId);

        // Validate expense exists and user owns it (or is admin)
        Optional<Expense> expenseOpt = expenseService.getExpenseById(expenseId);
        if (expenseOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Expense not found", null));
        }

        Expense expense = expenseOpt.get();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !expense.getUsername().equals(username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Not authorized to delete receipt for this expense", null));
        }

        if (expense.getReceiptPath() == null || expense.getReceiptPath().isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("No receipt attached to this expense", null));
        }

        try {
            storageService.deleteReceipt(expense.getReceiptPath());

            // Update expense
            expense.setReceiptPath(null);
            expense.setReceiptStatus("Receipt Missing");
            expenseService.updateExpense(expenseId, convertToDto(expense));

            logger.info("Receipt deleted successfully for expense {}", expenseId);

            return ResponseEntity.ok(ApiResponse.success("Receipt deleted successfully", null));

        } catch (IOException e) {
            logger.error("Failed to delete receipt: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete receipt: " + e.getMessage(), null));
        }
    }

    // Helper method to convert Expense to ExpenseDto
    private com.ammons.taskactivity.dto.ExpenseDto convertToDto(Expense expense) {
        // Implementation similar to ExpenseService
        com.ammons.taskactivity.dto.ExpenseDto dto = new com.ammons.taskactivity.dto.ExpenseDto();
        dto.setExpenseDate(expense.getExpenseDate());
        dto.setClient(expense.getClient());
        dto.setProject(expense.getProject());
        dto.setExpenseType(expense.getExpenseType());
        dto.setDescription(expense.getDescription());
        dto.setAmount(expense.getAmount());
        dto.setCurrency(expense.getCurrency());
        dto.setPaymentMethod(expense.getPaymentMethod());
        dto.setVendor(expense.getVendor());
        dto.setReferenceNumber(expense.getReferenceNumber());
        dto.setReceiptPath(expense.getReceiptPath());
        dto.setReceiptStatus(expense.getReceiptStatus());
        dto.setExpenseStatus(expense.getExpenseStatus());
        dto.setApprovedBy(expense.getApprovedBy());
        dto.setApprovalDate(expense.getApprovalDate());
        dto.setApprovalNotes(expense.getApprovalNotes());
        dto.setReimbursedAmount(expense.getReimbursedAmount());
        dto.setReimbursementDate(expense.getReimbursementDate());
        dto.setReimbursementNotes(expense.getReimbursementNotes());
        dto.setNotes(expense.getNotes());
        dto.setUsername(expense.getUsername());
        return dto;
    }
}
```

### Cost Estimates

**Storage Costs** (S3 Standard):

- $0.023 per GB/month
- Average receipt: 200KB
- 1,000 receipts/year = 200MB = **$0.005/month**
- 10,000 receipts/year = 2GB = **$0.05/month**

**Request Costs**:

- PUT (upload): $0.005 per 1,000 requests
- GET (download): $0.0004 per 1,000 requests
- Typical usage: 100 uploads + 500 views/month = **$0.0007/month**

**Total Estimated Cost**:

- **First Year**: $0.06-$0.60/month
- **5 Years**: $2-$5/month (much cheaper than alternatives)

---

## Backend Implementation

### 1. Entity Layer

**File:** `src/main/java/com/ammons/taskactivity/entity/Expense.java`

```java
package com.ammons.taskactivity.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Expense Entity - Tracks travel and home office expenses for reimbursement
 *
 * @author Dean Ammons
 * @version 2.0
 */
@Entity
@Table(name = "expenses", schema = "public")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // User who incurred the expense
    @NotBlank(message = "Username is required")
    @Size(max = 50, message = "Username cannot exceed 50 characters")
    @Column(name = "username", nullable = false, length = 50)
    private String username;

    // Client/Project Association (like Task Activity)
    @NotBlank(message = "Client is required")
    @Size(max = 50, message = "Client cannot exceed 50 characters")
    @Column(name = "client", nullable = false, length = 50)
    private String client;

    @Size(max = 50, message = "Project cannot exceed 50 characters")
    @Column(name = "project", length = 50)
    private String project;  // Optional

    // Expense Details
    @NotNull(message = "Expense date is required")
    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @NotBlank(message = "Expense type is required")
    @Size(max = 50, message = "Expense type cannot exceed 50 characters")
    @Column(name = "expense_type", nullable = false, length = 50)
    private String expenseType;  // From EXPENSE/EXPENSE_TYPE dropdown

    @NotBlank(message = "Description is required")
    @Size(max = 255, message = "Description cannot exceed 255 characters")
    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Size(max = 3, message = "Currency code must be 3 characters")
    @Column(name = "currency", length = 3)
    private String currency = "USD";

    // Payment & Vendor Information
    @NotBlank(message = "Payment method is required")
    @Size(max = 50, message = "Payment method cannot exceed 50 characters")
    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod;  // From EXPENSE/PAYMENT_METHOD dropdown

    @Size(max = 100, message = "Vendor name cannot exceed 100 characters")
    @Column(name = "vendor", length = 100)
    private String vendor;

    @Size(max = 50, message = "Reference number cannot exceed 50 characters")
    @Column(name = "reference_number", length = 50)
    private String referenceNumber;

    // Receipt Management
    @Size(max = 500, message = "Receipt path cannot exceed 500 characters")
    @Column(name = "receipt_path", length = 500)
    private String receiptPath;

    @Size(max = 50, message = "Receipt status cannot exceed 50 characters")
    @Column(name = "receipt_status", length = 50)
    private String receiptStatus;  // From EXPENSE/RECEIPT_STATUS dropdown

    // Status Tracking (CRITICAL for user visibility)
    @NotBlank(message = "Expense status is required")
    @Size(max = 50, message = "Status cannot exceed 50 characters")
    @Column(name = "expense_status", nullable = false, length = 50)
    private String expenseStatus = "Draft";  // From EXPENSE/EXPENSE_STATUS dropdown

    // Approval Details
    @Size(max = 100, message = "Approver name cannot exceed 100 characters")
    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "approval_date")
    private LocalDate approvalDate;

    @Size(max = 500, message = "Approval notes cannot exceed 500 characters")
    @Column(name = "approval_notes", length = 500)
    private String approvalNotes;

    // Reimbursement Tracking
    @DecimalMin(value = "0.00", message = "Reimbursed amount cannot be negative")
    @Column(name = "reimbursed_amount", precision = 10, scale = 2)
    private BigDecimal reimbursedAmount;

    @Column(name = "reimbursement_date")
    private LocalDate reimbursementDate;

    @Size(max = 500, message = "Reimbursement notes cannot exceed 500 characters")
    @Column(name = "reimbursement_notes", length = 500)
    private String reimbursementNotes;

    // Additional Notes
    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    @Column(name = "notes", length = 500)
    private String notes;

    // Auditing
    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "last_modified", nullable = false)
    private LocalDateTime lastModified;

    @Size(max = 100, message = "Last modified by cannot exceed 100 characters")
    @Column(name = "last_modified_by", length = 100)
    private String lastModifiedBy;

    // Constructors
    public Expense() {
        this.createdDate = LocalDateTime.now(ZoneOffset.UTC);
        this.lastModified = LocalDateTime.now(ZoneOffset.UTC);
        this.expenseStatus = "Draft";
        this.currency = "USD";
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastModified = LocalDateTime.now(ZoneOffset.UTC);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(LocalDate expenseDate) {
        this.expenseDate = expenseDate;
    }

    public String getExpenseType() {
        return expenseType;
    }

    public void setExpenseType(String expenseType) {
        this.expenseType = expenseType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getReceiptPath() {
        return receiptPath;
    }

    public void setReceiptPath(String receiptPath) {
        this.receiptPath = receiptPath;
    }

    public String getReceiptStatus() {
        return receiptStatus;
    }

    public void setReceiptStatus(String receiptStatus) {
        this.receiptStatus = receiptStatus;
    }

    public String getExpenseStatus() {
        return expenseStatus;
    }

    public void setExpenseStatus(String expenseStatus) {
        this.expenseStatus = expenseStatus;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public LocalDate getApprovalDate() {
        return approvalDate;
    }

    public void setApprovalDate(LocalDate approvalDate) {
        this.approvalDate = approvalDate;
    }

    public String getApprovalNotes() {
        return approvalNotes;
    }

    public void setApprovalNotes(String approvalNotes) {
        this.approvalNotes = approvalNotes;
    }

    public BigDecimal getReimbursedAmount() {
        return reimbursedAmount;
    }

    public void setReimbursedAmount(BigDecimal reimbursedAmount) {
        this.reimbursedAmount = reimbursedAmount;
    }

    public LocalDate getReimbursementDate() {
        return reimbursementDate;
    }

    public void setReimbursementDate(LocalDate reimbursementDate) {
        this.reimbursementDate = reimbursementDate;
    }

    public String getReimbursementNotes() {
        return reimbursementNotes;
    }

    public void setReimbursementNotes(String reimbursementNotes) {
        this.reimbursementNotes = reimbursementNotes;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    @Override
    public String toString() {
        return "Expense{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", expenseDate=" + expenseDate +
                ", client='" + client + '\'' +
                ", project='" + project + '\'' +
                ", expenseType='" + expenseType + '\'' +
                ", amount=" + amount +
                ", expenseStatus='" + expenseStatus + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Expense)) return false;
        Expense expense = (Expense) o;
        return id != null && id.equals(expense.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
```

---

### 2. Service Layer Constants

Add to `DropdownValueService.java`:

```java
// Expense subcategories
public static final String CATEGORY_EXPENSE = "EXPENSE";
public static final String SUBCATEGORY_EXPENSE_TYPE = "EXPENSE_TYPE";
public static final String SUBCATEGORY_PAYMENT_METHOD = "PAYMENT_METHOD";
public static final String SUBCATEGORY_EXPENSE_STATUS = "EXPENSE_STATUS";
public static final String SUBCATEGORY_VENDOR = "VENDOR";
public static final String SUBCATEGORY_RECEIPT_STATUS = "RECEIPT_STATUS";

/**
 * Convenience methods for EXPENSE subcategories
 */
@Transactional(readOnly = true)
public List<DropdownValue> getActiveExpenseTypes() {
    return dropdownValueRepository.findActiveByCategoryAndSubcategoryOrderByDisplayOrder(
            CATEGORY_EXPENSE, SUBCATEGORY_EXPENSE_TYPE);
}

@Transactional(readOnly = true)
public List<DropdownValue> getActivePaymentMethods() {
    return dropdownValueRepository.findActiveByCategoryAndSubcategoryOrderByDisplayOrder(
            CATEGORY_EXPENSE, SUBCATEGORY_PAYMENT_METHOD);
}

@Transactional(readOnly = true)
public List<DropdownValue> getActiveExpenseStatuses() {
    return dropdownValueRepository.findActiveByCategoryAndSubcategoryOrderByDisplayOrder(
            CATEGORY_EXPENSE, SUBCATEGORY_EXPENSE_STATUS);
}

@Transactional(readOnly = true)
public List<DropdownValue> getActiveVendors() {
    return dropdownValueRepository.findActiveByCategoryAndSubcategoryOrderByDisplayOrder(
            CATEGORY_EXPENSE, SUBCATEGORY_VENDOR);
}

@Transactional(readOnly = true)
public List<DropdownValue> getActiveReceiptStatuses() {
    return dropdownValueRepository.findActiveByCategoryAndSubcategoryOrderByDisplayOrder(
            CATEGORY_EXPENSE, SUBCATEGORY_RECEIPT_STATUS);
}
```

---

### 3. DTO Layer

**File:** `src/main/java/com/ammons/taskactivity/dto/ExpenseDto.java`

```java
package com.ammons.taskactivity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ExpenseDto - Data Transfer Object for Expense
 *
 * @author Dean Ammons
 * @version 2.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExpenseDto {

    // Expense Details
    @NotNull(message = "Expense date is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expenseDate;

    @NotBlank(message = "Client is required")
    @Size(max = 50, message = "Client name cannot exceed 50 characters")
    private String client;

    @Size(max = 50, message = "Project name cannot exceed 50 characters")
    private String project;  // Optional

    @NotBlank(message = "Expense type is required")
    @Size(max = 50, message = "Expense type cannot exceed 50 characters")
    private String expenseType;

    @NotBlank(message = "Description is required")
    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @Size(max = 3, message = "Currency code must be 3 characters")
    private String currency;

    // Payment Information
    @NotBlank(message = "Payment method is required")
    @Size(max = 50, message = "Payment method cannot exceed 50 characters")
    private String paymentMethod;

    @Size(max = 100, message = "Vendor name cannot exceed 100 characters")
    private String vendor;

    @Size(max = 50, message = "Reference number cannot exceed 50 characters")
    private String referenceNumber;

    // Receipt Management
    @Size(max = 500, message = "Receipt path cannot exceed 500 characters")
    private String receiptPath;

    @Size(max = 50, message = "Receipt status cannot exceed 50 characters")
    private String receiptStatus;

    // Status Tracking
    @NotBlank(message = "Expense status is required")
    @Size(max = 50, message = "Status cannot exceed 50 characters")
    private String expenseStatus;

    // Approval Details (set by manager/admin)
    @Size(max = 100, message = "Approver name cannot exceed 100 characters")
    private String approvedBy;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate approvalDate;

    @Size(max = 500, message = "Approval notes cannot exceed 500 characters")
    private String approvalNotes;

    // Reimbursement Tracking (set by admin)
    private BigDecimal reimbursedAmount;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate reimbursementDate;

    @Size(max = 500, message = "Reimbursement notes cannot exceed 500 characters")
    private String reimbursementNotes;

    // Notes
    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;

    // Username - set programmatically from logged-in user
    private String username;

    // Default constructor
    public ExpenseDto() {
        this.currency = "USD";
        this.expenseStatus = "Draft";
    }

    // Getters and Setters
    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(LocalDate expenseDate) {
        this.expenseDate = expenseDate;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getExpenseType() {
        return expenseType;
    }

    public void setExpenseType(String expenseType) {
        this.expenseType = expenseType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getReceiptPath() {
        return receiptPath;
    }

    public void setReceiptPath(String receiptPath) {
        this.receiptPath = receiptPath;
    }

    public String getReceiptStatus() {
        return receiptStatus;
    }

    public void setReceiptStatus(String receiptStatus) {
        this.receiptStatus = receiptStatus;
    }

    public String getExpenseStatus() {
        return expenseStatus;
    }

    public void setExpenseStatus(String expenseStatus) {
        this.expenseStatus = expenseStatus;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public LocalDate getApprovalDate() {
        return approvalDate;
    }

    public void setApprovalDate(LocalDate approvalDate) {
        this.approvalDate = approvalDate;
    }

    public String getApprovalNotes() {
        return approvalNotes;
    }

    public void setApprovalNotes(String approvalNotes) {
        this.approvalNotes = approvalNotes;
    }

    public BigDecimal getReimbursedAmount() {
        return reimbursedAmount;
    }

    public void setReimbursedAmount(BigDecimal reimbursedAmount) {
        this.reimbursedAmount = reimbursedAmount;
    }

    public LocalDate getReimbursementDate() {
        return reimbursementDate;
    }

    public void setReimbursementDate(LocalDate reimbursementDate) {
        this.reimbursementDate = reimbursementDate;
    }

    public String getReimbursementNotes() {
        return reimbursementNotes;
    }

    public void setReimbursementNotes(String reimbursementNotes) {
        this.reimbursementNotes = reimbursementNotes;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return "ExpenseDto{" +
                "expenseDate=" + expenseDate +
                ", client='" + client + '\'' +
                ", project='" + project + '\'' +
                ", expenseType='" + expenseType + '\'' +
                ", amount=" + amount +
                ", expenseStatus='" + expenseStatus + '\'' +
                ", username='" + username + '\'' +
                '}';
    }
}
```

---

### 4. Repository Layer

**File:** `src/main/java/com/ammons/taskactivity/repository/ExpenseRepository.java`

```java
package com.ammons.taskactivity.repository;

import com.ammons.taskactivity.entity.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * ExpenseRepository - Data access layer for Expense entities.
 *
 * @author Dean Ammons
 * @version 2.0
 */
@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    // Basic finders
    List<Expense> findByUsername(String username);
    boolean existsByUsername(String username);

    // Date-based queries
    List<Expense> findByExpenseDateBetween(LocalDate startDate, LocalDate endDate);
    List<Expense> findByUsernameAndExpenseDateBetween(
        String username, LocalDate startDate, LocalDate endDate);

    // Filter queries
    List<Expense> findByClientIgnoreCase(String client);
    List<Expense> findByProjectIgnoreCase(String project);
    List<Expense> findByExpenseTypeIgnoreCase(String expenseType);
    List<Expense> findByExpenseStatusIgnoreCase(String status);
    List<Expense> findByPaymentMethodIgnoreCase(String paymentMethod);

    // Custom queries with ordering
    @Query("SELECT e FROM Expense e WHERE e.expenseDate = :date " +
           "ORDER BY e.client, e.project, e.expenseType")
    List<Expense> findExpensesByDate(@Param("date") LocalDate date);

    @Query("SELECT e FROM Expense e WHERE e.expenseDate BETWEEN :startDate AND :endDate " +
           "ORDER BY e.expenseDate DESC, e.client, e.project")
    List<Expense> findExpensesInDateRange(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);

    // Status-based queries
    @Query("SELECT e FROM Expense e WHERE e.username = :username " +
           "AND e.expenseStatus = :status ORDER BY e.expenseDate DESC")
    List<Expense> findByUsernameAndStatus(
        @Param("username") String username,
        @Param("status") String status);

    @Query("SELECT e FROM Expense e WHERE e.expenseStatus = 'Pending Approval' " +
           "ORDER BY e.expenseDate ASC")
    List<Expense> findPendingApprovals();

    // Aggregate queries
    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.expenseDate = :date")
    Double getTotalAmountByDate(@Param("date") LocalDate date);

    @Query("SELECT SUM(e.amount) FROM Expense e " +
           "WHERE e.username = :username AND e.expenseDate BETWEEN :startDate AND :endDate")
    Double getTotalAmountByUserAndDateRange(
        @Param("username") String username,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(e.amount) FROM Expense e " +
           "WHERE e.username = :username AND e.expenseStatus = :status")
    Double getTotalAmountByUserAndStatus(
        @Param("username") String username,
        @Param("status") String status);

    // Client-based aggregates
    @Query("SELECT SUM(e.amount) FROM Expense e " +
           "WHERE e.client = :client AND e.expenseDate BETWEEN :startDate AND :endDate")
    Double getTotalAmountByClientAndDateRange(
        @Param("client") String client,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);

    // Pageable versions
    Page<Expense> findAll(Pageable pageable);
    Page<Expense> findByUsername(String username, Pageable pageable);
    Page<Expense> findByUsernameAndExpenseDateBetween(
        String username, LocalDate startDate, LocalDate endDate, Pageable pageable);
    Page<Expense> findByUsernameAndExpenseDate(
        String username, LocalDate date, Pageable pageable);
    Page<Expense> findByUsernameAndClientIgnoreCase(
        String username, String client, Pageable pageable);
    Page<Expense> findByUsernameAndProjectIgnoreCase(
        String username, String project, Pageable pageable);
    Page<Expense> findByUsernameAndExpenseStatusIgnoreCase(
        String username, String status, Pageable pageable);

    // Flexible filter query (matches TaskActivity pattern)
    @Query(value = "SELECT * FROM expenses e WHERE " +
                   "(CAST(:username AS text) IS NULL OR e.username = :username) AND " +
                   "(CAST(:client AS text) IS NULL OR e.client = :client) AND " +
                   "(CAST(:project AS text) IS NULL OR e.project = :project) AND " +
                   "(CAST(:expenseType AS text) IS NULL OR e.expense_type = :expenseType) AND " +
                   "(CAST(:status AS text) IS NULL OR e.expense_status = :status) AND " +
                   "(CAST(:paymentMethod AS text) IS NULL OR e.payment_method = :paymentMethod) AND " +
                   "(CAST(:startDate AS date) IS NULL OR e.expense_date >= CAST(:startDate AS date)) AND " +
                   "(CAST(:endDate AS date) IS NULL OR e.expense_date <= CAST(:endDate AS date))",
           nativeQuery = true)
    Page<Expense> findByFilters(
        @Param("username") String username,
        @Param("client") String client,
        @Param("project") String project,
        @Param("expenseType") String expenseType,
        @Param("status") String status,
        @Param("paymentMethod") String paymentMethod,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        Pageable pageable);
}
```

---

### 5. Service Layer

**File:** `src/main/java/com/ammons/taskactivity/service/ExpenseService.java`

```java
package com.ammons.taskactivity.service;

import com.ammons.taskactivity.dto.ExpenseDto;
import com.ammons.taskactivity.entity.Expense;
import com.ammons.taskactivity.exception.ExpenseNotFoundException;
import com.ammons.taskactivity.repository.ExpenseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * ExpenseService - Business logic layer for Expense operations.
 *
 * Provides comprehensive expense management including CRUD operations,
 * filtering, status tracking, and approval workflow.
 *
 * @author Dean Ammons
 * @version 2.0
 */
@Service
@Transactional
public class ExpenseService {

    private final ExpenseRepository expenseRepository;

    public ExpenseService(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    // ========== CRUD Operations ==========

    /**
     * Create a new expense
     */
    public Expense createExpense(ExpenseDto expenseDto) {
        Expense expense = convertDtoToEntity(expenseDto);
        expense.setCreatedDate(LocalDateTime.now(ZoneOffset.UTC));
        expense.setLastModified(LocalDateTime.now(ZoneOffset.UTC));
        expense.setLastModifiedBy(expenseDto.getUsername());
        return expenseRepository.save(expense);
    }

    /**
     * Get all expenses (admin only)
     */
    @Transactional(readOnly = true)
    public List<Expense> getAllExpenses() {
        return expenseRepository.findAll();
    }

    /**
     * Get expenses for a specific user
     */
    @Transactional(readOnly = true)
    public List<Expense> getExpensesByUsername(String username) {
        return expenseRepository.findByUsername(username);
    }

    /**
     * Get a single expense by ID
     */
    @Transactional(readOnly = true)
    public Optional<Expense> getExpenseById(Long id) {
        return expenseRepository.findById(id);
    }

    /**
     * Update an existing expense
     */
    public Expense updateExpense(Long id, ExpenseDto expenseDto) {
        Optional<Expense> existingExpense = expenseRepository.findById(id);
        if (existingExpense.isPresent()) {
            Expense expense = existingExpense.get();
            updateEntityFromDto(expense, expenseDto);
            expense.setLastModified(LocalDateTime.now(ZoneOffset.UTC));
            expense.setLastModifiedBy(expenseDto.getUsername());
            return expenseRepository.save(expense);
        }
        throw new ExpenseNotFoundException(id);
    }

    /**
     * Delete an expense
     */
    public void deleteExpense(Long id) {
        if (expenseRepository.existsById(id)) {
            expenseRepository.deleteById(id);
        } else {
            throw new ExpenseNotFoundException(id);
        }
    }

    // ========== Date-Based Queries ==========

    /**
     * Get expenses for a specific date
     */
    @Transactional(readOnly = true)
    public List<Expense> getExpensesByDate(LocalDate date) {
        return expenseRepository.findExpensesByDate(date);
    }

    /**
     * Get expenses within a date range
     */
    @Transactional(readOnly = true)
    public List<Expense> getExpensesInDateRange(LocalDate startDate, LocalDate endDate) {
        return expenseRepository.findExpensesInDateRange(startDate, endDate);
    }

    /**
     * Get expenses within a date range for a specific user
     */
    @Transactional(readOnly = true)
    public List<Expense> getExpensesInDateRangeForUser(String username, LocalDate startDate, LocalDate endDate) {
        return expenseRepository.findByUsernameAndExpenseDateBetween(username, startDate, endDate);
    }

    // ========== Filter Queries ==========

    /**
     * Get expenses by client
     */
    @Transactional(readOnly = true)
    public List<Expense> getExpensesByClient(String client) {
        return expenseRepository.findByClientIgnoreCase(client);
    }

    /**
     * Get expenses by project
     */
    @Transactional(readOnly = true)
    public List<Expense> getExpensesByProject(String project) {
        return expenseRepository.findByProjectIgnoreCase(project);
    }

    /**
     * Get expenses by type
     */
    @Transactional(readOnly = true)
    public List<Expense> getExpensesByType(String expenseType) {
        return expenseRepository.findByExpenseTypeIgnoreCase(expenseType);
    }

    /**
     * Get expenses by status
     */
    @Transactional(readOnly = true)
    public List<Expense> getExpensesByStatus(String status) {
        return expenseRepository.findByExpenseStatusIgnoreCase(status);
    }

    /**
     * Get expenses by username and status
     */
    @Transactional(readOnly = true)
    public List<Expense> getExpensesByUsernameAndStatus(String username, String status) {
        return expenseRepository.findByUsernameAndStatus(username, status);
    }

    /**
     * Get all pending approval expenses (admin/manager use)
     */
    @Transactional(readOnly = true)
    public List<Expense> getPendingApprovals() {
        return expenseRepository.findPendingApprovals();
    }

    // ========== Aggregate Queries ==========

    /**
     * Get total amount for a specific date
     */
    @Transactional(readOnly = true)
    public Double getTotalAmountByDate(LocalDate date) {
        Double total = expenseRepository.getTotalAmountByDate(date);
        return total != null ? total : 0.0;
    }

    /**
     * Get total amount for user within date range
     */
    @Transactional(readOnly = true)
    public Double getTotalAmountByUserAndDateRange(String username, LocalDate startDate, LocalDate endDate) {
        Double total = expenseRepository.getTotalAmountByUserAndDateRange(username, startDate, endDate);
        return total != null ? total : 0.0;
    }

    /**
     * Get total amount by user and status
     */
    @Transactional(readOnly = true)
    public Double getTotalAmountByUserAndStatus(String username, String status) {
        Double total = expenseRepository.getTotalAmountByUserAndStatus(username, status);
        return total != null ? total : 0.0;
    }

    /**
     * Get total amount by client within date range
     */
    @Transactional(readOnly = true)
    public Double getTotalAmountByClientAndDateRange(String client, LocalDate startDate, LocalDate endDate) {
        Double total = expenseRepository.getTotalAmountByClientAndDateRange(client, startDate, endDate);
        return total != null ? total : 0.0;
    }

    // ========== Pageable Queries ==========

    @Transactional(readOnly = true)
    public Page<Expense> getAllExpenses(Pageable pageable) {
        return expenseRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Expense> getExpensesByUsername(String username, Pageable pageable) {
        return expenseRepository.findByUsername(username, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Expense> getExpensesByUsernameAndDate(String username, LocalDate date, Pageable pageable) {
        return expenseRepository.findByUsernameAndExpenseDate(username, date, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Expense> getExpensesByUsernameAndDateRange(String username, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return expenseRepository.findByUsernameAndExpenseDateBetween(username, startDate, endDate, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Expense> getExpensesByUsernameAndClient(String username, String client, Pageable pageable) {
        return expenseRepository.findByUsernameAndClientIgnoreCase(username, client, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Expense> getExpensesByUsernameAndProject(String username, String project, Pageable pageable) {
        return expenseRepository.findByUsernameAndProjectIgnoreCase(username, project, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Expense> getExpensesByUsernameAndStatus(String username, String status, Pageable pageable) {
        return expenseRepository.findByUsernameAndExpenseStatusIgnoreCase(username, status, pageable);
    }

    /**
     * Get expenses with flexible filters and pagination
     */
    @Transactional(readOnly = true)
    public Page<Expense> getExpensesByFilters(String username, String client, String project,
            String expenseType, String status, String paymentMethod,
            LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return expenseRepository.findByFilters(username, client, project, expenseType,
                status, paymentMethod, startDate, endDate, pageable);
    }

    // ========== Status Management ==========

    /**
     * Submit expense for approval (changes status from Draft to Submitted)
     */
    public Expense submitForApproval(Long id, String username) {
        Optional<Expense> expenseOpt = expenseRepository.findById(id);
        if (expenseOpt.isEmpty()) {
            throw new ExpenseNotFoundException(id);
        }

        Expense expense = expenseOpt.get();

        // Validate user owns this expense
        if (!expense.getUsername().equals(username)) {
            throw new IllegalStateException("Cannot submit expense that belongs to another user");
        }

        // Validate current status allows submission
        if (!"Draft".equals(expense.getExpenseStatus()) && !"Rejected - Needs Revision".equals(expense.getExpenseStatus())) {
            throw new IllegalStateException("Only Draft or Rejected expenses can be submitted");
        }

        expense.setExpenseStatus("Submitted");
        expense.setLastModified(LocalDateTime.now(ZoneOffset.UTC));
        expense.setLastModifiedBy(username);

        return expenseRepository.save(expense);
    }

    /**
     * Approve an expense (admin/manager only)
     */
    public Expense approveExpense(Long id, String approverUsername, String approvalNotes) {
        Optional<Expense> expenseOpt = expenseRepository.findById(id);
        if (expenseOpt.isEmpty()) {
            throw new ExpenseNotFoundException(id);
        }

        Expense expense = expenseOpt.get();

        // Validate current status allows approval
        if (!"Submitted".equals(expense.getExpenseStatus()) && !"Resubmitted".equals(expense.getExpenseStatus())) {
            throw new IllegalStateException("Only Submitted or Resubmitted expenses can be approved");
        }

        expense.setExpenseStatus("Approved");
        expense.setApprovedBy(approverUsername);
        expense.setApprovalDate(LocalDate.now());
        expense.setApprovalNotes(approvalNotes);
        expense.setLastModified(LocalDateTime.now(ZoneOffset.UTC));
        expense.setLastModifiedBy(approverUsername);

        return expenseRepository.save(expense);
    }

    /**
     * Reject an expense (admin/manager only)
     */
    public Expense rejectExpense(Long id, String approverUsername, String rejectionNotes) {
        Optional<Expense> expenseOpt = expenseRepository.findById(id);
        if (expenseOpt.isEmpty()) {
            throw new ExpenseNotFoundException(id);
        }

        Expense expense = expenseOpt.get();

        // Validate current status allows rejection
        if (!"Submitted".equals(expense.getExpenseStatus()) && !"Resubmitted".equals(expense.getExpenseStatus())) {
            throw new IllegalStateException("Only Submitted or Resubmitted expenses can be rejected");
        }

        expense.setExpenseStatus("Rejected - Needs Revision");
        expense.setApprovedBy(approverUsername);
        expense.setApprovalDate(LocalDate.now());
        expense.setApprovalNotes(rejectionNotes);
        expense.setLastModified(LocalDateTime.now(ZoneOffset.UTC));
        expense.setLastModifiedBy(approverUsername);

        return expenseRepository.save(expense);
    }

    /**
     * Mark expense as reimbursed (admin only)
     */
    public Expense markAsReimbursed(Long id, String adminUsername, Double reimbursedAmount, String notes) {
        Optional<Expense> expenseOpt = expenseRepository.findById(id);
        if (expenseOpt.isEmpty()) {
            throw new ExpenseNotFoundException(id);
        }

        Expense expense = expenseOpt.get();

        // Validate current status allows reimbursement
        if (!"Approved".equals(expense.getExpenseStatus())) {
            throw new IllegalStateException("Only Approved expenses can be marked as reimbursed");
        }

        expense.setExpenseStatus("Reimbursed");
        expense.setReimbursementDate(LocalDate.now());
        expense.setReimbursementNotes(notes);
        expense.setLastModified(LocalDateTime.now(ZoneOffset.UTC));
        expense.setLastModifiedBy(adminUsername);

        return expenseRepository.save(expense);
    }

    // ========== Utility Methods ==========

    /**
     * Check if a user has any expenses
     */
    @Transactional(readOnly = true)
    public boolean userHasExpenses(String username) {
        return expenseRepository.existsByUsername(username);
    }

    // ========== DTO Conversion ==========

    /**
     * Convert DTO to Entity
     */
    private Expense convertDtoToEntity(ExpenseDto dto) {
        Expense entity = new Expense();
        entity.setUsername(dto.getUsername());
        entity.setClient(dto.getClient());
        entity.setProject(dto.getProject());
        entity.setExpenseDate(dto.getExpenseDate());
        entity.setExpenseType(dto.getExpenseType());
        entity.setDescription(dto.getDescription());
        entity.setAmount(dto.getAmount());
        entity.setCurrency(dto.getCurrency() != null ? dto.getCurrency() : "USD");
        entity.setPaymentMethod(dto.getPaymentMethod());
        entity.setVendor(dto.getVendor());
        entity.setReferenceNumber(dto.getReferenceNumber());
        entity.setReceiptPath(dto.getReceiptPath());
        entity.setReceiptStatus(dto.getReceiptStatus());
        entity.setExpenseStatus(dto.getExpenseStatus() != null ? dto.getExpenseStatus() : "Draft");
        entity.setApprovedBy(dto.getApprovedBy());
        entity.setApprovalDate(dto.getApprovalDate());
        entity.setApprovalNotes(dto.getApprovalNotes());
        entity.setReimbursedAmount(dto.getReimbursedAmount());
        entity.setReimbursementDate(dto.getReimbursementDate());
        entity.setReimbursementNotes(dto.getReimbursementNotes());
        entity.setNotes(dto.getNotes());
        return entity;
    }

    /**
     * Update Entity from DTO (username is immutable after creation)
     */
    private void updateEntityFromDto(Expense entity, ExpenseDto dto) {
        entity.setClient(dto.getClient());
        entity.setProject(dto.getProject());
        entity.setExpenseDate(dto.getExpenseDate());
        entity.setExpenseType(dto.getExpenseType());
        entity.setDescription(dto.getDescription());
        entity.setAmount(dto.getAmount());
        entity.setCurrency(dto.getCurrency());
        entity.setPaymentMethod(dto.getPaymentMethod());
        entity.setVendor(dto.getVendor());
        entity.setReferenceNumber(dto.getReferenceNumber());
        entity.setReceiptPath(dto.getReceiptPath());
        entity.setReceiptStatus(dto.getReceiptStatus());
        entity.setExpenseStatus(dto.getExpenseStatus());
        entity.setApprovedBy(dto.getApprovedBy());
        entity.setApprovalDate(dto.getApprovalDate());
        entity.setApprovalNotes(dto.getApprovalNotes());
        entity.setReimbursedAmount(dto.getReimbursedAmount());
        entity.setReimbursementDate(dto.getReimbursementDate());
        entity.setReimbursementNotes(dto.getReimbursementNotes());
        entity.setNotes(dto.getNotes());
        // Username is NOT updated - it remains the original creator
    }
}
```

---

### 6. Exception Handler

**File:** `src/main/java/com/ammons/taskactivity/exception/ExpenseNotFoundException.java`

```java
package com.ammons.taskactivity.exception;

/**
 * ExpenseNotFoundException - Custom exception for expense not found scenarios
 *
 * @author Dean Ammons
 * @version 1.0
 */
public class ExpenseNotFoundException extends RuntimeException {

    public ExpenseNotFoundException(Long id) {
        super("Expense not found with ID: " + id);
    }

    public ExpenseNotFoundException(String message) {
        super(message);
    }
}
```

---

### 7. Controller Layer

**File:** `src/main/java/com/ammons/taskactivity/controller/ExpenseController.java`

```java
package com.ammons.taskactivity.controller;

import com.ammons.taskactivity.dto.ApiResponse;
import com.ammons.taskactivity.dto.ExpenseDto;
import com.ammons.taskactivity.entity.Expense;
import com.ammons.taskactivity.exception.ExpenseNotFoundException;
import com.ammons.taskactivity.service.ExpenseService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * ExpenseController - REST API Controller for Expense operations
 *
 * Provides endpoints for expense CRUD operations, filtering, status management,
 * and approval workflow. Implements role-based security to ensure users can only
 * access their own expenses unless they are admins.
 *
 * @author Dean Ammons
 * @version 1.0
 */
@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private static final Logger logger = LoggerFactory.getLogger(ExpenseController.class);
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    // ========== CREATE ==========

    /**
     * Create a new expense
     */
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'GUEST')")
    @PostMapping
    public ResponseEntity<ApiResponse<Expense>> createExpense(
            @Valid @RequestBody ExpenseDto expenseDto,
            Authentication authentication) {

        String username = authentication.getName();
        logger.info("User {} creating expense", username);

        // Set the username from the authenticated user (security measure)
        expenseDto.setUsername(username);

        Expense createdExpense = expenseService.createExpense(expenseDto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Expense created successfully", createdExpense));
    }

    // ========== READ ==========

    /**
     * Get all expenses with pagination and filtering
     * - ADMIN: sees all expenses
     * - USER/GUEST: sees only their own expenses
     */
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'GUEST')")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<Expense>>> getAllExpenses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String client,
            @RequestParam(required = false) String project,
            @RequestParam(required = false) String expenseType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {

        String username = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals(ROLE_ADMIN));

        logger.info("User {} requesting expenses (admin={}, filters: client={}, project={}, type={}, status={})",
                username, isAdmin, client, project, expenseType, status);

        // Create pageable with sorting (newest first)
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "expenseDate"));

        // Non-admins can only see their own expenses
        String filterUsername = isAdmin ? null : username;

        Page<Expense> expenses = expenseService.getExpensesByFilters(
                filterUsername, client, project, expenseType, status, paymentMethod,
                startDate, endDate, pageable);

        return ResponseEntity.ok(ApiResponse.success("Expenses retrieved successfully", expenses));
    }

    /**
     * Get a single expense by ID
     * - Users can only get their own expenses
     * - Admins can get any expense
     */
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'GUEST')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Expense>> getExpenseById(
            @PathVariable Long id,
            Authentication authentication) {

        String username = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals(ROLE_ADMIN));

        Optional<Expense> expense = expenseService.getExpenseById(id);

        if (expense.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Expense not found", null));
        }

        // Verify ownership unless admin
        if (!isAdmin && !expense.get().getUsername().equals(username)) {
            logger.warn("User {} attempted to access expense {} owned by {}",
                    username, id, expense.get().getUsername());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You can only view your own expenses", null));
        }

        return ResponseEntity.ok(ApiResponse.success("Expense retrieved successfully", expense.get()));
    }

    /**
     * Get expenses for the current week (convenience endpoint)
     */
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'GUEST')")
    @GetMapping("/current-week")
    public ResponseEntity<ApiResponse<List<Expense>>> getCurrentWeekExpenses(
            Authentication authentication) {

        String username = authentication.getName();
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate endOfWeek = startOfWeek.plusDays(6);

        List<Expense> expenses = expenseService.getExpensesInDateRangeForUser(
                username, startOfWeek, endOfWeek);

        return ResponseEntity.ok(ApiResponse.success("Current week expenses retrieved", expenses));
    }

    // ========== UPDATE ==========

    /**
     * Update an existing expense
     * - Users can only update their own expenses
     * - Users cannot modify approval/reimbursement fields
     * - Admins can update any expense
     */
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'GUEST')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Expense>> updateExpense(
            @PathVariable Long id,
            @Valid @RequestBody ExpenseDto expenseDto,
            Authentication authentication) {

        String username = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals(ROLE_ADMIN));

        logger.info("User {} updating expense {}", username, id);

        // Verify expense exists
        Optional<Expense> existingExpense = expenseService.getExpenseById(id);
        if (existingExpense.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Expense not found", null));
        }

        // Verify ownership unless admin
        if (!isAdmin && !existingExpense.get().getUsername().equals(username)) {
            logger.warn("User {} attempted to update expense {} owned by {}",
                    username, id, existingExpense.get().getUsername());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You can only update your own expenses", null));
        }

        // Non-admins cannot modify protected fields
        if (!isAdmin) {
            if (expenseDto.getApprovedBy() != null ||
                expenseDto.getApprovalDate() != null ||
                expenseDto.getReimbursedAmount() != null ||
                expenseDto.getReimbursementDate() != null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("You cannot modify approval or reimbursement fields", null));
            }

            // Non-admins can only set status to Draft, Submitted, or Resubmitted
            if (expenseDto.getExpenseStatus() != null &&
                !expenseDto.getExpenseStatus().equals("Draft") &&
                !expenseDto.getExpenseStatus().equals("Submitted") &&
                !expenseDto.getExpenseStatus().equals("Resubmitted")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("You can only set status to Draft, Submitted, or Resubmitted", null));
            }
        }

        // Set username from authentication (prevent changing owner)
        expenseDto.setUsername(username);

        Expense updatedExpense = expenseService.updateExpense(id, expenseDto);

        return ResponseEntity.ok(ApiResponse.success("Expense updated successfully", updatedExpense));
    }

    // ========== DELETE ==========

    /**
     * Delete an expense
     * - Users can only delete their own expenses
     * - Admins can delete any expense
     */
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteExpense(
            @PathVariable Long id,
            Authentication authentication) {

        String username = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals(ROLE_ADMIN));

        logger.info("User {} deleting expense {}", username, id);

        // Verify expense exists
        Optional<Expense> expense = expenseService.getExpenseById(id);
        if (expense.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Expense not found", null));
        }

        // Verify ownership unless admin
        if (!isAdmin && !expense.get().getUsername().equals(username)) {
            logger.warn("User {} attempted to delete expense {} owned by {}",
                    username, id, expense.get().getUsername());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You can only delete your own expenses", null));
        }

        expenseService.deleteExpense(id);

        return ResponseEntity.ok(ApiResponse.success("Expense deleted successfully", null));
    }

    // ========== STATUS MANAGEMENT ==========

    /**
     * Submit expense for approval
     */
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'GUEST')")
    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<Expense>> submitForApproval(
            @PathVariable Long id,
            Authentication authentication) {

        String username = authentication.getName();
        logger.info("User {} submitting expense {} for approval", username, id);

        try {
            Expense submittedExpense = expenseService.submitForApproval(id, username);
            return ResponseEntity.ok(ApiResponse.success("Expense submitted for approval", submittedExpense));
        } catch (ExpenseNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage(), null));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), null));
        }
    }

    /**
     * Approve an expense (admin only)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<Expense>> approveExpense(
            @PathVariable Long id,
            @RequestParam(required = false) String notes,
            Authentication authentication) {

        String username = authentication.getName();
        logger.info("Admin {} approving expense {}", username, id);

        try {
            Expense approvedExpense = expenseService.approveExpense(id, username, notes);
            return ResponseEntity.ok(ApiResponse.success("Expense approved", approvedExpense));
        } catch (ExpenseNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage(), null));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), null));
        }
    }

    /**
     * Reject an expense (admin only)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<Expense>> rejectExpense(
            @PathVariable Long id,
            @RequestParam String notes,
            Authentication authentication) {

        String username = authentication.getName();
        logger.info("Admin {} rejecting expense {}", username, id);

        try {
            Expense rejectedExpense = expenseService.rejectExpense(id, username, notes);
            return ResponseEntity.ok(ApiResponse.success("Expense rejected", rejectedExpense));
        } catch (ExpenseNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage(), null));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), null));
        }
    }

    /**
     * Mark expense as reimbursed (admin only)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/reimburse")
    public ResponseEntity<ApiResponse<Expense>> markAsReimbursed(
            @PathVariable Long id,
            @RequestParam Double amount,
            @RequestParam(required = false) String notes,
            Authentication authentication) {

        String username = authentication.getName();
        logger.info("Admin {} marking expense {} as reimbursed", username, id);

        try {
            Expense reimbursedExpense = expenseService.markAsReimbursed(id, username, amount, notes);
            return ResponseEntity.ok(ApiResponse.success("Expense marked as reimbursed", reimbursedExpense));
        } catch (ExpenseNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage(), null));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), null));
        }
    }

    /**
     * Get pending approval queue (admin only)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/pending-approvals")
    public ResponseEntity<ApiResponse<List<Expense>>> getPendingApprovals() {

        logger.info("Retrieving pending approval queue");
        List<Expense> pendingExpenses = expenseService.getPendingApprovals();

        return ResponseEntity.ok(ApiResponse.success("Pending approvals retrieved", pendingExpenses));
    }

    // ========== AGGREGATE QUERIES ==========

    /**
     * Get total expenses for user in date range
     */
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'GUEST')")
    @GetMapping("/total")
    public ResponseEntity<ApiResponse<Double>> getTotalExpenses(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {

        String username = authentication.getName();
        Double total = expenseService.getTotalAmountByUserAndDateRange(username, startDate, endDate);

        return ResponseEntity.ok(ApiResponse.success("Total expenses calculated", total));
    }

    /**
     * Get total expenses by status for user
     */
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'GUEST')")
    @GetMapping("/total-by-status")
    public ResponseEntity<ApiResponse<Double>> getTotalByStatus(
            @RequestParam String status,
            Authentication authentication) {

        String username = authentication.getName();
        Double total = expenseService.getTotalAmountByUserAndStatus(username, status);

        return ResponseEntity.ok(ApiResponse.success("Total expenses by status calculated", total));
    }
}
```

**Key Features of ExpenseController:**

1. **Role-Based Security**:
    - `@PreAuthorize` annotations enforce role requirements
    - Non-admin users can only access their own expenses
    - Admins can access all expenses

2. **CRUD Operations**:
    - Create, Read, Update, Delete with proper validation
    - Ownership verification for non-admin users

3. **Status Management**:
    - Submit for approval
    - Approve/Reject (admin only)
    - Mark as reimbursed (admin only)

4. **Filtering & Pagination**:
    - Support for multiple filter parameters
    - Page-based results with sorting
    - Date range queries

5. **Security Checks**:
    - Prevents users from modifying approval/reimbursement fields
    - Prevents users from accessing expenses they don't own
    - Validates status transitions

6. **Aggregate Queries**:
    - Calculate totals by date range
    - Calculate totals by status

---

## Frontend Implementation

### UI Considerations

The frontend should provide a simple, user-friendly interface similar to the Task Activity entry form:

### 1. Expense Dashboard

**Location**: Update existing Dashboard component

**New Expense Summary Cards**:

- Total Draft Expenses: $X.XX
- Submitted (Pending Approval): $X.XX
- Approved (Awaiting Reimbursement): $X.XX
- Reimbursed This Month: $X.XX

### 2. Expense Entry Form

**Form Sections**:

1. **Basic Expense Information**
    - Expense Date (required) - Date picker
    - Expense Type (required) - Dropdown from EXPENSE/EXPENSE_TYPE
    - Amount (required) - Number input with currency symbol
    - Description (required) - Text field

2. **Payment Information**
    - Payment Method (required) - Dropdown from EXPENSE/PAYMENT_METHOD
    - Vendor - Text input or optional dropdown
    - Reference Number - Text field

3. **Client/Project Association**
    - Client (required) - Dropdown from existing CLIENTS
    - Project (optional) - Dropdown from TASK/PROJECT values

4. **Receipt Upload**
    - Upload Receipt Image - File upload (jpg, png, pdf)
    - Receipt Preview - Show thumbnail if uploaded

5. **Notes** (optional)
    - Additional Notes - Text area

6. **Action Buttons**
    - Save as Draft - Keeps status as "Draft"
    - Submit for Approval - Changes status to "Submitted"
    - Cancel - Discard changes

### 3. Expense List/Dashboard View

**Features**:

- **Status Filter**: Draft | Submitted | Approved | Reimbursed | All
- **Date Range Filter**: This Month | Last Month | This Quarter | Custom Range
- **Client Filter**: All Clients | Specific Client
- **Expense Type Filter**: All Types | Travel | Home Office | Other

**List Columns**:

- Date
- Expense Type
- Description
- Amount
- Client/Project
- Status (with color coding)
- Receipt (icon indicator)
- Actions (View/Edit/Delete)

### 4. Manager Approval Queue

**For Managers/Admins**:

- List of expenses with status "Pending Approval"
- Approve/Reject buttons
- Add approval notes
- View receipt images
- Batch approval capability

---

## Security & Access Control

### Role-Based Access

**USER/GUEST:**

- Can create, view, update, and delete their own expenses
- Can submit expenses for approval
- Cannot view other users' expenses
- Cannot approve expenses or change status beyond "Submitted"
- Cannot modify reimbursement fields

**EXPENSE_APPROVER:**

- Can view and approve/reject expense requests from all users
- Can access the expense approval queue
- Can view all submitted/pending expenses
- Can set approval status and notes
- **Cannot** access user management functions
- **Cannot** access system configuration or dropdown management
- **Cannot** mark expenses as reimbursed (accounting function)
- Limited to expense approval workflow only

**ADMIN:**

- Can view all expenses across all users
- Can approve/reject expense requests
- Can modify expense status
- Can set reimbursement information
- Can generate company-wide expense reports
- Full access to all administrative functions (user management, system settings, etc.)

### Security Implementation

The security implementation ensures that:

1. **Regular users can only edit their own expenses** - they cannot modify expenses created by other users
2. **Regular users cannot change approval/reimbursement fields** - only admins and expense approvers can approve expenses
3. **Expense approvers can approve/reject expenses** - they have access to the approval queue but not to other admin functions
4. **Admins have full access** - they can view and modify all expenses, including approval and reimbursement information, plus all system administration functions

**Role Assignment:**

The EXPENSE*APPROVER role is stored in the database `users` table without the "ROLE*" prefix. Spring Security automatically adds the "ROLE\_" prefix when checking permissions. For example:

- Database stores: `EXPENSE_APPROVER`
- Spring Security checks: `hasRole('EXPENSE_APPROVER')` or `hasAnyRole('ADMIN', 'EXPENSE_APPROVER')`

To assign the EXPENSE_APPROVER role to a user, update their role in the database:

```sql
UPDATE users SET role = 'EXPENSE_APPROVER' WHERE username = 'manager_username';
```

**Example Implementation Pattern in ExpenseController.java:**

```java
// Example: Update Expense endpoint with security checks
@PreAuthorize("hasAnyRole('USER', 'ADMIN', 'GUEST')")
@PutMapping("/{id}")
public ResponseEntity<ApiResponse<Expense>> updateExpense(
        @PathVariable Long id,
        @RequestBody @Valid ExpenseDto expenseDto,
        Authentication authentication) {

    String username = authentication.getName();

    // Check if the current user is an admin
    boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

    // If NOT an admin, apply restrictions
    if (!isAdmin) {
        // 1. Verify the user owns this expense
        Optional<Expense> existing = expenseService.getExpenseById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if (!existing.get().getUsername().equals(username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You can only edit your own expenses", null));
        }

        // 2. Prevent users from modifying protected fields
        // Users should not be able to set: approvedBy, approvalDate, approvalNotes,
        // reimbursedAmount, reimbursementDate, reimbursementNotes
        // These should be null in the incoming DTO or match existing values
        if (expenseDto.getApprovedBy() != null ||
            expenseDto.getApprovalDate() != null ||
            expenseDto.getReimbursedAmount() != null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You cannot modify approval or reimbursement fields", null));
        }

        // 3. Prevent users from setting status beyond "Submitted"
        if (expenseDto.getExpenseStatus() != null &&
            !expenseDto.getExpenseStatus().equals("Draft") &&
            !expenseDto.getExpenseStatus().equals("Submitted")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You can only set status to Draft or Submitted", null));
        }
    }

    // If admin, or if all checks passed for regular user, proceed with update
    Expense updated = expenseService.updateExpense(id, expenseDto);
    return ResponseEntity.ok(ApiResponse.success("Expense updated successfully", updated));
}
```

**Key Security Rules:**

| Field/Action                                                      | Regular User (USER/GUEST) | EXPENSE_APPROVER | ADMIN  |
| ----------------------------------------------------------------- | ------------------------- | ---------------- | ------ |
| Edit own expenses                                                 | ✅ Yes                    | ✅ Yes           | ✅ Yes |
| Edit other users' expenses                                        | ❌ No                     | ❌ No            | ✅ Yes |
| View approval queue                                               | ❌ No                     | ✅ Yes           | ✅ Yes |
| Approve/reject expenses                                           | ❌ No                     | ✅ Yes           | ✅ Yes |
| Set `approvedBy`, `approvalDate`, `approvalNotes`                 | ❌ No                     | ✅ Yes           | ✅ Yes |
| Set `reimbursedAmount`, `reimbursementDate`, `reimbursementNotes` | ❌ No                     | ❌ No            | ✅ Yes |
| Change status to "Draft" or "Submitted"                           | ✅ Yes                    | ✅ Yes           | ✅ Yes |
| Change status to "Approved" or "Rejected"                         | ❌ No                     | ✅ Yes           | ✅ Yes |
| Change status to "Reimbursed"                                     | ❌ No                     | ❌ No            | ✅ Yes |
| View other users' expenses                                        | ❌ No                     | ✅ Yes (pending) | ✅ Yes |
| Access user management                                            | ❌ No                     | ❌ No            | ✅ Yes |
| Access system settings                                            | ❌ No                     | ❌ No            | ✅ Yes |

**Approval Endpoint Example:**

```java
// Example: Approve expense endpoint with EXPENSE_APPROVER role
@PreAuthorize("hasAnyRole('ADMIN', 'EXPENSE_APPROVER')")
@PostMapping("/{id}/approve")
public ResponseEntity<ApiResponse<Expense>> approveExpense(
        @PathVariable Long id,
        @RequestParam(required = false) String notes,
        Authentication authentication) {

    String approverUsername = authentication.getName();
    Expense approved = expenseService.approveExpense(id, approverUsername, notes);
    return ResponseEntity.ok(ApiResponse.success("Expense approved successfully", approved));
}
```

**Why This Matters:**

- Prevents users from approving their own expenses
- Prevents users from falsely marking expenses as reimbursed
- Separates approval authority from full administrative access (principle of least privilege)
- Allows managers to approve expenses without access to user management or system configuration
- Ensures proper approval workflow is followed
- Maintains data integrity and audit trail

### Field-Level Security

1. **Users (USER/GUEST) CANNOT modify**:
    - `approved_by`, `approval_date`, `approval_notes`
    - `reimbursed_amount`, `reimbursement_date`, `reimbursement_notes`
    - Cannot change status beyond "Submitted" or "Resubmitted"

2. **Expense Approvers (EXPENSE_APPROVER) CAN**:
    - Approve/reject expenses (set `approved_by`, `approval_date`, `approval_notes`)
    - Change status to "Approved" or "Rejected"
    - View all submitted/pending expenses in approval queue

    **But CANNOT**:
    - Mark expenses as reimbursed (accounting function reserved for ADMIN)
    - Access user management, system settings, or dropdown management
    - Edit arbitrary expense fields (only approval workflow actions)

3. **Admins (ADMIN) CAN modify**:
    - All fields including approval and reimbursement fields
    - Can change any status
    - Can approve/reject with notes
    - Can mark as reimbursed with payment details
    - Full access to all administrative functions

---

## Testing Strategy

### 1. Unit Tests

**Repository Tests:**

- `ExpenseRepositoryTest.java` - Test all query methods

**Service Tests:**

- `ExpenseServiceTest.java` - Test CRUD operations, status transitions, totals

**Controller Tests:**

- `ExpenseControllerTest.java` - Test REST API endpoints, security, authorization

### 2. Integration Tests

- End-to-end expense creation workflow
- Status transition workflow (Draft → Submitted → Approved → Reimbursed)
- Filter and pagination testing
- Receipt upload functionality
- Manager approval workflow

### 3. Frontend Tests

**Component Tests:**

- `expense-list.component.spec.ts`
- `expense-edit-dialog.component.spec.ts`
- `expense-dashboard.component.spec.ts`

**Service Tests:**

- `expense.service.spec.ts`

---

## Implementation Phases

### Phase 1: Core Expense Entry (MVP) - Week 1-2

- Create database schema and run migration
- Insert dropdown seed data
- Create Expense entity with essential fields
- Implement 3 core EXPENSE subcategories (EXPENSE_TYPE, PAYMENT_METHOD, EXPENSE_STATUS)
- Create ExpenseRepository with basic queries
- Create ExpenseService with CRUD operations
- Create ExpenseController REST API
- **Build simple expense entry form (Angular)**
- **Create expense list view with status filter**
- Write unit tests

### Phase 2: Status Workflow - Week 2-3

- Implement status transitions (Draft → Submitted → Approved → Reimbursed)
- Add "Submit for Approval" functionality
- Create manager approval queue component
- Status change validations
- Basic notifications (email/in-app)
- Admin approval/rejection with notes

### Phase 3: Receipt Management - Week 3

- Add file upload capability for receipt images
- Receipt preview/viewing
- Receipt status tracking
- Link receipts to expenses
- Download receipt functionality
- Receipt validation (file type, size)

### Phase 4: Reporting & Dashboard - Week 3-4

- User expense dashboard with status summary cards
- Expense list filtering (by status, date, client, type)
- "My Pending Reimbursements" view
- Manager "Pending Approvals" view
- Export to CSV/Excel for corporate system entry
- Client expense summary reports

### Phase 5: Enhanced Features - Week 4-5

- Bulk expense entry (multiple expenses at once)
- Advanced filtering and search
- Expense history and audit trail
- Mobile-responsive design improvements
- Performance optimization
- User preferences and settings

### Phase 6: Integration & Polish - Week 5-6

- Corporate system export templates
- Batch operations (bulk approve, bulk export)
- Advanced reporting and analytics
- Documentation updates
- User acceptance testing
- Deployment preparation

---

## Questions for Consideration

1. **Receipt Management**:
    - What file formats should be supported? (jpg, png, pdf, all?)
    - Maximum file size limit for receipt uploads?
    - Should we allow multiple receipt images per expense?

2. **Approval Workflow**:
    - How many approval levels? (One manager? Multiple levels?)
    - Who can approve? (Direct manager only? Any admin? Specific roles?)
    - Can users edit expenses after submission or only before?

3. **Status Transitions**:
    - Can users un-submit an expense if not yet approved?
    - What happens to rejected expenses? Back to draft or separate state?
    - Should there be an "Entered in Corporate System" final status?

4. **Home Office Expenses**:
    - Are there different approval rules for home office vs travel?
    - Do home office expenses require receipts the same as travel?
    - Any monthly limits or caps on home office expense categories?

5. **Client/Project Association**:
    - Are all expenses required to have a client/project association?
    - Can one expense be split across multiple clients? (Probably not MVP)

6. **Reimbursement**:
    - Do you track reimbursement separately from approval?
    - Is reimbursement date entered manually or automatically?
    - Should users be able to see reimbursement payment details?

7. **Vendor Field**:
    - Should vendor be a dropdown (from VENDOR subcategory) or freeform text?
    - If dropdown, should users be able to add new vendors?

8. **Mobile Access**:
    - Is mobile-responsive web sufficient or need native mobile app?
    - Priority for mobile: Entry? Viewing? Approval?

9. **Notifications**:
    - Email notifications for status changes?
    - In-app notifications?
    - Reminders for pending approvals?

10. **Export/Integration**:
    - What format does the corporate expense system require?
    - Manual export (CSV/Excel) or automated integration?
    - Include receipt images in export?

---

## Implementation Checklist

### Backend (Week 1-2)

- [x] Create database schema and run migration
- [x] Insert dropdown seed data for EXPENSE subcategories
- [x] Create Expense entity (without isBillable field)
- [x] Create ExpenseDto
- [x] Create ExpenseRepository with query methods
- [x] Create ExpenseService with business logic
- [x] Add expense methods to DropdownValueService
- [x] Create ExpenseNotFoundException
- [x] Create ExpenseController with REST endpoints
- [x] Add security annotations and ownership validation
- [x] Write unit tests for Service and Repository
- [x] Write controller tests
- [ ] Test API endpoints with Postman/Swagger

### Frontend Services (Week 2)

- [ ] Create Expense model interface
- [ ] Create ExpenseService (Angular)
- [ ] Add routing for expense pages (app.routes.ts)
- [ ] Update navigation menu with Expenses link
- [ ] Update Dashboard component with Expenses card

### Frontend Components (Week 2-4)

- [ ] Update DashboardComponent
    - [ ] Add Expenses card with icon and status summary
    - [ ] Configure routing to /expenses
    - [ ] Display expense statistics by status
- [ ] Create ExpenseListComponent
    - [ ] Table layout with filtering
    - [ ] Pagination
    - [ ] Status indicators with color coding
    - [ ] Add/Edit/Delete actions
    - [ ] Receipt indicator icons
- [ ] Create ExpenseEditDialogComponent
    - [ ] Form with all required fields
    - [ ] Dropdown integrations (expense type, payment method, client, project)
    - [ ] Validation
    - [ ] Receipt upload component
    - [ ] Submit for Approval button
- [ ] Create ExpenseApprovalComponent (Admin)
    - [ ] Pending approvals queue
    - [ ] Approve/Reject actions
    - [ ] View receipt capability
    - [ ] Add approval notes
- [ ] Create WeeklyExpensesComponent (Optional)
    - [ ] Week selector
    - [ ] Daily expense breakdown
    - [ ] Total calculations
    - [ ] Export functionality

### Integration & Testing (Week 3-4)

- [ ] End-to-end testing
- [ ] Status workflow testing
- [ ] Receipt upload and download testing
- [ ] Frontend component testing
- [ ] User acceptance testing
- [ ] Performance testing with large datasets
- [ ] Security audit
- [ ] CSV/Excel export functionality

### Documentation (Week 4)

- [ ] Update API documentation (Swagger)
- [ ] Update User Guide with Expense Tracking section
- [ ] Update Administrator Guide with Approval workflow
- [ ] Add expense tracking to Developer Guide
- [ ] Create expense management tutorial
- [ ] Document dropdown values and their usage

### Deployment (Week 5)

- [ ] Database backup before deployment
- [ ] Run migration scripts
- [ ] Deploy backend changes
- [ ] Deploy frontend changes
- [ ] Verify production deployment
- [ ] Monitor for issues
- [ ] User training (if needed)

---

## Estimated Effort

### Time Breakdown (Development Hours)

| Phase             | Task                      | Hours         |
| ----------------- | ------------------------- | ------------- |
| **Backend**       | Entity, DTO, Repository   | 8             |
|                   | Service Layer             | 10            |
|                   | REST Controller           | 8             |
|                   | Exception Handling        | 2             |
|                   | Status Workflow Logic     | 4             |
|                   | Unit Tests                | 14            |
| **Frontend**      | Models & Services         | 6             |
|                   | Dashboard Integration     | 4             |
|                   | Expense List Component    | 14            |
|                   | Edit Dialog Component     | 12            |
|                   | Approval Queue Component  | 10            |
|                   | Receipt Upload Component  | 8             |
|                   | Weekly Expenses Component | 12            |
|                   | Component Tests           | 12            |
| **Database**      | Schema Design & Migration | 4             |
|                   | Dropdown Seed Data        | 3             |
| **Integration**   | End-to-End Testing        | 10            |
|                   | Status Workflow Testing   | 6             |
|                   | Bug Fixes & Refinement    | 12            |
| **Documentation** | All Documentation Updates | 10            |
| **Deployment**    | Preparation & Execution   | 6             |
| **Total**         |                           | **175 hours** |

### Calendar Estimate

- **1 Developer, Full-Time:** ~4-5 weeks
- **1 Developer, Part-Time (50%):** ~8-10 weeks
- **With existing Task Activity knowledge:** Can save 15-20% time

---

## Summary

The Expense Tracking add-on is designed to parallel the Task Activity application:

**Task Activity App**: Team members track their project hours → Enter into corporate timesheet system

**Expense Tracking App**: Team members track their travel and home office expenses → Enter into corporate expense system

### Key Features:

- Simple expense entry with client/project association
- Status tracking (Draft → Submitted → Approved → Reimbursed)
- Receipt upload and management
- Manager approval workflow
- Reporting for corporate system entry
- Travel expense types (airfare, hotel, meals, transportation, etc.)
- Home office expense types (internet, equipment, office supplies, etc.)

### Explicitly NOT Included:

- Client billing or invoicing
- Accounting or general ledger functions
- Tax calculation or categorization
- Budget management
- Vendor management systems
- Accounts payable processing

This is a **user-centric tool** to make expense reporting easier, more organized, and more transparent, following the same pattern as the Task Activity application.

---

## References

### Existing Code Patterns to Follow

1. **TaskActivity Entity Pattern:** `src/main/java/com/ammons/taskactivity/entity/TaskActivity.java`
2. **Service Layer Pattern:** `src/main/java/com/ammons/taskactivity/service/TaskActivityService.java`
3. **Controller Security Pattern:** `src/main/java/com/ammons/taskactivity/controller/TaskActivitiesController.java`
4. **Angular Component Pattern:** `frontend/src/app/components/task-list/task-list.component.ts`
5. **Dropdown Management:** `src/main/java/com/ammons/taskactivity/entity/DropdownValue.java`

### Key Technologies

- **Backend:** Spring Boot 3.x, Spring Data JPA, Spring Security
- **Frontend:** Angular 18+, Angular Material
- **Database:** PostgreSQL
- **Build:** Maven (Backend), npm/Angular CLI (Frontend)

---

## Part 7: Thymeleaf View Layer Implementation

**Status**: Required for Backend Completion  
**Date Added**: November 29, 2025

### Overview

The Expense Tracking feature requires Thymeleaf templates and a view controller to match the existing Task Activity pattern. This section provides the complete implementation for server-side rendered views.

### Required Files

#### 1. ExpenseViewController.java

**Location**: `src/main/java/com/ammons/taskactivity/controller/ExpenseViewController.java`

```java
package com.ammons.taskactivity.controller;

import com.ammons.taskactivity.dto.ExpenseDto;
import com.ammons.taskactivity.entity.Expense;
import com.ammons.taskactivity.service.ExpenseService;
import com.ammons.taskactivity.service.DropdownValueService;
import com.ammons.taskactivity.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/expenses")
public class ExpenseViewController {

    private static final org.slf4j.Logger logger =
        org.slf4j.LoggerFactory.getLogger(ExpenseViewController.class);

    // View names
    private static final String EXPENSE_LIST_VIEW = "expense-list";
    private static final String EXPENSE_FORM_VIEW = "expense-form";
    private static final String EXPENSE_DETAIL_VIEW = "expense-detail";
    private static final String EXPENSE_SHEET_VIEW = "expense-sheet";
    private static final String APPROVAL_QUEUE_VIEW = "admin/expense-approval-queue";
    private static final String REDIRECT_EXPENSE_LIST = "redirect:/expenses/list";

    // Model attributes
    private static final String ERROR_MESSAGE_ATTR = "errorMessage";
    private static final String SUCCESS_MESSAGE_ATTR = "successMessage";

    private final ExpenseService expenseService;
    private final DropdownValueService dropdownValueService;
    private final UserService userService;

    public ExpenseViewController(ExpenseService expenseService,
                                DropdownValueService dropdownValueService,
                                UserService userService) {
        this.expenseService = expenseService;
        this.dropdownValueService = dropdownValueService;
        this.userService = userService;
    }

    @GetMapping
    public String showMain(Authentication authentication) {
        return REDIRECT_EXPENSE_LIST;
    }

    @GetMapping("/list")
    public String showExpenseList(
            @RequestParam(required = false) String client,
            @RequestParam(required = false) String project,
            @RequestParam(required = false) String expenseType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            Model model, Authentication authentication) {

        addUserInfo(model, authentication);

        Pageable pageable = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "expenseDate"));
        boolean isAdmin = isAdmin(authentication);
        String currentUsername = isAdmin ? null : authentication.getName();
        String filterUsername = (isAdmin && username != null) ? username : currentUsername;

        com.ammons.taskactivity.dto.ExpenseFilterDto filter =
            new com.ammons.taskactivity.dto.ExpenseFilterDto(
                filterUsername, client, project, expenseType, status, null, startDate, endDate);

        Page<Expense> expensesPage = expenseService.getExpensesByFilters(filter, pageable);

        model.addAttribute("expenses", expensesPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", expensesPage.getTotalPages());
        model.addAttribute("totalItems", expensesPage.getTotalElements());

        addDropdownOptions(model);
        addFilterAttributes(model, client, project, expenseType, status, username, startDate, endDate);

        if (isAdmin) {
            model.addAttribute("users", userService.getAllUsers());
        }

        return EXPENSE_LIST_VIEW;
    }

    @GetMapping("/add")
    public String showForm(Model model, Authentication authentication) {
        addUserInfo(model, authentication);
        ExpenseDto expenseDto = new ExpenseDto();
        expenseDto.setExpenseDate(LocalDate.now());
        model.addAttribute("expenseDto", expenseDto);
        model.addAttribute("isEdit", false);
        addDropdownOptions(model);
        return EXPENSE_FORM_VIEW;
    }

    @PostMapping("/submit")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'GUEST')")
    public String submitForm(@Valid @ModelAttribute ExpenseDto expenseDto,
                            BindingResult bindingResult,
                            Model model, RedirectAttributes redirectAttributes,
                            Authentication authentication) {
        if (bindingResult.hasErrors()) {
            addUserInfo(model, authentication);
            model.addAttribute("isEdit", false);
            addDropdownOptions(model);
            return EXPENSE_FORM_VIEW;
        }

        try {
            expenseDto.setUsername(authentication.getName());
            expenseService.createExpense(expenseDto);
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR, "Expense created successfully");
            return REDIRECT_EXPENSE_LIST;
        } catch (Exception e) {
            logger.error("Error creating expense: {}", e.getMessage(), e);
            model.addAttribute(ERROR_MESSAGE_ATTR, "Failed to create expense: " + e.getMessage());
            addUserInfo(model, authentication);
            addDropdownOptions(model);
            return EXPENSE_FORM_VIEW;
        }
    }

    @GetMapping("/detail/{id}")
    public String showExpenseDetail(@PathVariable Long id, Model model,
                                   RedirectAttributes redirectAttributes,
                                   Authentication authentication) {
        addUserInfo(model, authentication);

        Optional<Expense> expenseOpt = expenseService.getExpenseById(id);
        if (expenseOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR, "Expense not found");
            return REDIRECT_EXPENSE_LIST;
        }

        Expense expense = expenseOpt.get();
        boolean isAdmin = isAdmin(authentication);

        if (!isAdmin && !expense.getUsername().equals(authentication.getName())) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                "You can only view your own expenses");
            return REDIRECT_EXPENSE_LIST;
        }

        model.addAttribute("expense", expense);
        model.addAttribute("expenseDto", convertToDto(expense));
        model.addAttribute("expenseId", id);
        addDropdownOptions(model);
        return EXPENSE_DETAIL_VIEW;
    }

    @PostMapping("/update/{id}")
    public String updateExpense(@PathVariable Long id,
                               @Valid @ModelAttribute ExpenseDto expenseDto,
                               BindingResult bindingResult,
                               Model model, RedirectAttributes redirectAttributes,
                               Authentication authentication) {
        if (bindingResult.hasErrors()) {
            addUserInfo(model, authentication);
            model.addAttribute("expenseId", id);
            addDropdownOptions(model);
            return EXPENSE_DETAIL_VIEW;
        }

        try {
            Optional<Expense> existingOpt = expenseService.getExpenseById(id);
            if (existingOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR, "Expense not found");
                return REDIRECT_EXPENSE_LIST;
            }

            Expense existing = existingOpt.get();
            boolean isAdmin = isAdmin(authentication);

            if (!isAdmin && !existing.getUsername().equals(authentication.getName())) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                    "You can only update your own expenses");
                return REDIRECT_EXPENSE_LIST;
            }

            expenseDto.setUsername(existing.getUsername());
            expenseService.updateExpense(id, expenseDto);
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR, "Expense updated successfully");
            return "redirect:/expenses/detail/" + id;
        } catch (Exception e) {
            logger.error("Error updating expense: {}", e.getMessage(), e);
            model.addAttribute(ERROR_MESSAGE_ATTR, "Failed to update expense: " + e.getMessage());
            addUserInfo(model, authentication);
            addDropdownOptions(model);
            return EXPENSE_DETAIL_VIEW;
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteExpense(@PathVariable Long id,
                               RedirectAttributes redirectAttributes,
                               Authentication authentication) {
        try {
            Optional<Expense> expenseOpt = expenseService.getExpenseById(id);
            if (expenseOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR, "Expense not found");
                return REDIRECT_EXPENSE_LIST;
            }

            Expense expense = expenseOpt.get();
            boolean isAdmin = isAdmin(authentication);

            if (!isAdmin && !expense.getUsername().equals(authentication.getName())) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                    "You can only delete your own expenses");
                return REDIRECT_EXPENSE_LIST;
            }

            expenseService.deleteExpense(id);
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR, "Expense deleted successfully");
            return REDIRECT_EXPENSE_LIST;
        } catch (Exception e) {
            logger.error("Error deleting expense: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                "Failed to delete expense: " + e.getMessage());
            return REDIRECT_EXPENSE_LIST;
        }
    }

    @GetMapping("/weekly-sheet")
    public String showWeeklyExpenseSheet(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model, Authentication authentication) {
        addUserInfo(model, authentication);

        LocalDate targetDate = date != null ? date : LocalDate.now();
        LocalDate startOfWeek = targetDate.minusDays(targetDate.getDayOfWeek().getValue() - 1);
        LocalDate endOfWeek = startOfWeek.plusDays(6);

        String username = authentication.getName();
        List<Expense> expenses = expenseService.getExpensesInDateRangeForUser(
            username, startOfWeek, endOfWeek);

        model.addAttribute("expenses", expenses);
        model.addAttribute("startDate", startOfWeek);
        model.addAttribute("endDate", endOfWeek);
        model.addAttribute("targetDate", targetDate);

        return EXPENSE_SHEET_VIEW;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/approval-queue")
    public String showApprovalQueue(Model model, Authentication authentication) {
        addUserInfo(model, authentication);
        List<Expense> pendingExpenses = expenseService.getPendingApprovals();
        model.addAttribute("expenses", pendingExpenses);
        return APPROVAL_QUEUE_VIEW;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/approve/{id}")
    public String approveExpense(@PathVariable Long id,
                                @RequestParam(required = false) String notes,
                                RedirectAttributes redirectAttributes,
                                Authentication authentication) {
        try {
            expenseService.approveExpense(id, authentication.getName(), notes);
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR, "Expense approved");
            return "redirect:/expenses/admin/approval-queue";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                "Failed to approve expense: " + e.getMessage());
            return "redirect:/expenses/admin/approval-queue";
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/reject/{id}")
    public String rejectExpense(@PathVariable Long id,
                               @RequestParam String notes,
                               RedirectAttributes redirectAttributes,
                               Authentication authentication) {
        try {
            expenseService.rejectExpense(id, authentication.getName(), notes);
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR, "Expense rejected");
            return "redirect:/expenses/admin/approval-queue";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                "Failed to reject expense: " + e.getMessage());
            return "redirect:/expenses/admin/approval-queue";
        }
    }

    // Helper methods
    private void addUserInfo(Model model, Authentication authentication) {
        model.addAttribute("username", authentication.getName());
        model.addAttribute("isAdmin", isAdmin(authentication));
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
    }

    private void addDropdownOptions(Model model) {
        model.addAttribute("clients", dropdownValueService.getValuesBySubcategory("EXPENSE", "CLIENT"));
        model.addAttribute("projects", dropdownValueService.getValuesBySubcategory("EXPENSE", "PROJECT"));
        model.addAttribute("expenseTypes", dropdownValueService.getValuesBySubcategory("EXPENSE", "EXPENSE_TYPE"));
        model.addAttribute("paymentMethods", dropdownValueService.getValuesBySubcategory("EXPENSE", "PAYMENT_METHOD"));
        model.addAttribute("expenseStatuses", dropdownValueService.getValuesBySubcategory("EXPENSE", "EXPENSE_STATUS"));
    }

    private void addFilterAttributes(Model model, String client, String project,
                                    String expenseType, String status, String username,
                                    LocalDate startDate, LocalDate endDate) {
        model.addAttribute("selectedClient", client);
        model.addAttribute("selectedProject", project);
        model.addAttribute("selectedExpenseType", expenseType);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedUsername", username);
        model.addAttribute("selectedStartDate", startDate);
        model.addAttribute("selectedEndDate", endDate);
    }

    private ExpenseDto convertToDto(Expense expense) {
        ExpenseDto dto = new ExpenseDto();
        dto.setExpenseDate(expense.getExpenseDate());
        dto.setClient(expense.getClient());
        dto.setProject(expense.getProject());
        dto.setExpenseType(expense.getExpenseType());
        dto.setDescription(expense.getDescription());
        dto.setAmount(expense.getAmount());
        dto.setCurrency(expense.getCurrency());
        dto.setPaymentMethod(expense.getPaymentMethod());
        dto.setVendor(expense.getVendor());
        dto.setReferenceNumber(expense.getReferenceNumber());
        dto.setReceiptPath(expense.getReceiptPath());
        dto.setReceiptStatus(expense.getReceiptStatus());
        dto.setExpenseStatus(expense.getExpenseStatus());
        dto.setNotes(expense.getNotes());
        dto.setUsername(expense.getUsername());
        return dto;
    }
}
```

#### 2. expense-list.html Template

**Location**: `src/main/resources/templates/expense-list.html`

Pattern the template after `task-list.html` with these key elements:

- Header with "Expense List" title
- Filter section (Client, Project, Expense Type, Status, Date Range, Username for admin)
- Pagination controls
- Table columns: Date, Client, Project, Type, Amount, Status, Receipt, Actions
- Add/Export buttons
- Row click to view detail
- Admin sees all expenses with username column
- Users see only their own expenses

#### 3. expense-form.html Template

**Location**: `src/main/resources/templates/expense-form.html`

Pattern after `task-activity-form.html` with these fields:

- Expense Date (date picker, default today)
- Client (dropdown, required)
- Project (dropdown, optional)
- Expense Type (dropdown, required)
- Description (textarea, required)
- Amount (number input, required, min 0.01)
- Currency (text, default USD)
- Payment Method (dropdown, required)
- Vendor (text, optional)
- Reference Number (text, optional)
- Notes (textarea, optional)
- Submit/Cancel buttons

#### 4. expense-detail.html Template

**Location**: `src/main/resources/templates/expense-detail.html`

Pattern after `task-detail.html` with:

- All expense fields (editable for users if Draft/Rejected status)
- Receipt upload section (if no receipt)
- Receipt download link (if receipt exists)
- Status workflow buttons (Submit for Approval for users)
- Admin-only fields (Approved By, Approval Date, Reimbursed Amount, etc.)
- Delete button (with confirmation)
- Back to List button

#### 5. expense-sheet.html Template

**Location**: `src/main/resources/templates/expense-sheet.html`

Pattern after `weekly-timesheet.html` with:

- Week navigation (Previous/Current/Next)
- Date range display
- Table grouped by day showing all expenses
- Total by day
- Grand total for week
- Print/Export functionality

#### 6. admin/expense-approval-queue.html Template

**Location**: `src/main/resources/templates/admin/expense-approval-queue.html`

Admin-only page showing:

- List of expenses with status "Pending Approval" or "Submitted"
- Expense details (Date, User, Client, Project, Type, Amount, Receipt link)
- Approve button (with optional notes field)
- Reject button (with required notes field)
- View Detail link

### Integration Points

#### Dashboard/Home Page Updates

Add Expense section to main navigation and dashboard with links to:

- `/expenses/list` - Expense List
- `/expenses/add` - Add New Expense
- `/expenses/weekly-sheet` - Weekly Expense Sheet
- `/expenses/admin/approval-queue` - Approval Queue (admin only)

#### Navigation Updates

Update `base.css` and main navigation templates to include Expense menu items alongside Task Activity items.

### Key Implementation Notes

1. **Security**: All endpoints check user authentication and ownership (users can only see/edit own expenses, admins see all)

2. **Status Workflow**:
    - Users: Create (Draft) → Submit → Resubmit if rejected
    - Admins: Approve → Mark as Reimbursed

3. **Receipt Handling**: Receipt upload/download uses existing `ReceiptController` REST endpoints via JavaScript fetch

4. **Filtering**: All filters are optional, admins get additional username filter

5. **Pagination**: 20 items per page, sorted by expense date descending

6. **Validation**: Form validation matches ExpenseDto annotations

7. **Error Handling**: All errors display user-friendly messages via flash attributes

### Testing Checklist

- [ ] User can create expense
- [ ] User can view own expense list with filters
- [ ] User can edit own draft/rejected expenses
- [ ] User can delete own expenses
- [ ] User can submit expense for approval
- [ ] Admin can view all expenses
- [ ] Admin can approve/reject expenses
- [ ] Admin can view approval queue
- [ ] Receipt upload/download works
- [ ] Weekly expense sheet displays correctly
- [ ] Pagination works
- [ ] Security prevents unauthorized access

---

**Date**: November 14, 2025  
**Author**: GitHub Copilot  
**Status**: Planning Complete - Ready for Implementation  
**Last Updated**: November 29, 2025 - Added Thymeleaf View Layer section
