package com.ammons.taskactivity.repository;

import com.ammons.taskactivity.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for managing {@link Permission} entities. Provides methods for querying
 * permissions by resource and action.
 * 
 * @see Permission
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    /**
     * Finds a permission by its resource and action.
     * 
     * @param resource the resource name (e.g., "TASK_ACTIVITY")
     * @param action the action name (e.g., "CREATE")
     * @return an Optional containing the permission if found, or empty if not found
     */
    Optional<Permission> findByResourceAndAction(String resource, String action);

    /**
     * Checks if a permission exists with the specified resource and action.
     * 
     * @param resource the resource name
     * @param action the action name
     * @return true if a matching permission exists, false otherwise
     */
    boolean existsByResourceAndAction(String resource, String action);
}
