package com.ammons.taskactivity.repository;

import com.ammons.taskactivity.entity.Roles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for managing {@link Roles} entities. Provides methods for querying roles and
 * checking role-permission associations.
 * 
 * @see Roles
 * @see com.ammons.taskactivity.entity.Permission
 */
@Repository
public interface RoleRepository extends JpaRepository<Roles, Long> {

    /**
     * Finds a role by its name.
     * 
     * @param name the role name (e.g., "ADMIN", "USER")
     * @return an Optional containing the role if found, or empty if not found
     */
    Optional<Roles> findByName(String name);

    /**
     * Checks if a role has a specific permission. Uses a JPQL query to join the role's permissions
     * and verify the existence of a permission with the specified resource and action.
     * 
     * @param roleId the ID of the role to check
     * @param resource the resource name (e.g., "TASK_ACTIVITY")
     * @param action the action name (e.g., "CREATE")
     * @return true if the role has the permission, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END "
            + "FROM Roles r JOIN r.permissions p "
            + "WHERE r.id = :roleId AND p.resource = :resource AND p.action = :action")
    boolean roleHasPermission(@Param("roleId") Long roleId, @Param("resource") String resource,
            @Param("action") String action);
}
