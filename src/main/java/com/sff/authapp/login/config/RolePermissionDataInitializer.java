package com.sff.authapp.login.config;

import com.sff.authapp.login.entities.*;
import com.sff.authapp.login.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class RolePermissionDataInitializer implements ApplicationRunner {

    private final AppResourceRepository resourceRepository;
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    private record ResourceDef(String key, String displayName, String category, String route, int sortOrder, List<String> actions) {}

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Checking and initializing resources, permissions, and role mappings...");

        // 1. Define all application resources
        List<ResourceDef> resourceDefs = List.of(
                new ResourceDef("dashboard", "Dashboard", "Overview", "/dashboard", 1, List.of("VIEW")),
                new ResourceDef("orders", "Orders Management", "Core Operations", "/orders", 2, List.of("VIEW", "CREATE", "EDIT", "DELETE", "PRINT", "EXPORT")),
                new ResourceDef("customers", "Customers", "Core Operations", "/customers", 3, List.of("VIEW", "CREATE", "EDIT", "DELETE", "EXPORT")),
                new ResourceDef("measurements", "Measurements", "Tailoring", "/measurements", 4, List.of("VIEW", "CREATE", "EDIT", "DELETE")),
                new ResourceDef("cutting", "Cutting Queue", "Production", "/cutting", 5, List.of("VIEW", "CREATE", "EDIT", "DELETE")),
                new ResourceDef("production", "Production Capacity", "Production", "/capacity", 6, List.of("VIEW", "EDIT")),
                new ResourceDef("workers", "Workers Management", "Workforce", "/workers", 7, List.of("VIEW", "CREATE", "EDIT", "DELETE")),
                new ResourceDef("worker_allotment", "Worker Work Allotment", "Workforce", "/worker-allotment", 8, List.of("VIEW", "EDIT")),
                new ResourceDef("alterations", "Alterations", "Production", "/alterations", 9, List.of("VIEW", "CREATE", "EDIT", "DELETE")),
                new ResourceDef("billing", "Billing & Invoicing", "Finance", "/billing", 10, List.of("VIEW", "CREATE", "EDIT", "DELETE", "PRINT")),
                new ResourceDef("payments", "Payments", "Finance", "/payments", 11, List.of("VIEW", "CREATE", "EDIT", "DELETE")),
                new ResourceDef("expenses", "Expenses", "Finance", "/expenses", 12, List.of("VIEW", "CREATE", "EDIT", "DELETE")),
                new ResourceDef("reports", "Analytics & Reports", "Reports", "/reports", 13, List.of("VIEW", "EXPORT")),
                new ResourceDef("services_pricing", "Services & Pricing", "Catalog", "/services", 14, List.of("VIEW", "CREATE", "EDIT", "DELETE")),
                new ResourceDef("addons", "Add-ons & Options", "Catalog", "/addons", 15, List.of("VIEW", "CREATE", "EDIT", "DELETE")),
                new ResourceDef("fabric_samples", "Fabric & Samples", "Catalog", "/fabric-samples", 16, List.of("VIEW", "CREATE", "EDIT", "DELETE")),
                new ResourceDef("historical_orders", "Historical Orders", "Archive", "/historical-orders", 17, List.of("VIEW", "EXPORT")),
                new ResourceDef("logs", "System & Message Logs", "Administration", "/logs", 18, List.of("VIEW")),
                new ResourceDef("roles_permissions", "Roles & Access Control", "Administration", "/roles", 19, List.of("VIEW", "CREATE", "EDIT", "DELETE")),
                new ResourceDef("settings", "Shop & System Settings", "Administration", "/settings", 20, List.of("VIEW", "EDIT")),
                new ResourceDef("users", "User Management", "Administration", "/users", 21, List.of("VIEW", "CREATE", "EDIT", "DELETE"))
        );

        // 2. Persist resources and permissions
        for (ResourceDef def : resourceDefs) {
            AppResource resource = resourceRepository.findByResourceKey(def.key())
                    .orElseGet(() -> resourceRepository.save(AppResource.builder()
                            .resourceKey(def.key())
                            .displayName(def.displayName())
                            .category(def.category())
                            .route(def.route())
                            .sortOrder(def.sortOrder())
                            .active(true)
                            .build()
                    ));

            for (String action : def.actions()) {
                String permKey = (def.key() + "." + action).toLowerCase();
                if (!permissionRepository.existsByPermissionKey(permKey)) {
                    permissionRepository.save(Permission.builder()
                            .permissionKey(permKey)
                            .resource(resource)
                            .action(action.toUpperCase())
                            .displayName(formatActionDisplayName(action, def.displayName()))
                            .description("Allows user to " + action.toLowerCase() + " " + def.displayName().toLowerCase())
                            .build());
                }
            }
        }

        // 3. Ensure baseline roles exist
        List<String> standardRoles = List.of(
                "ROLE_OWNER",
                "ROLE_ADMIN",
                "ROLE_SHOPKEEPER",
                "ROLE_TAILOR",
                "ROLE_WORKER",
                "ROLE_CUSTOMER"
        );

        for (String roleName : standardRoles) {
            if (!roleRepository.existsByName(roleName)) {
                roleRepository.save(Role.builder().name(roleName).build());
            }
        }

        // 4. Initialize baseline permissions for roles that have none mapped yet
        List<Permission> allPermissions = permissionRepository.findAll();

        for (String roleName : standardRoles) {
            Optional<Role> roleOpt = roleRepository.findByName(roleName);
            if (roleOpt.isEmpty()) continue;
            Role role = roleOpt.get();

            List<RolePermission> existing = rolePermissionRepository.findByRoleId(role.getId());
            if (existing.isEmpty()) {
                log.info("Seeding initial permissions for role: {}", roleName);
                List<RolePermission> toSave = new ArrayList<>();

                for (Permission p : allPermissions) {
                    boolean grant = shouldGrantByDefault(roleName, p.getPermissionKey().toLowerCase());
                    toSave.add(RolePermission.builder()
                            .role(role)
                            .permission(p)
                            .granted(grant)
                            .updatedBy("SYSTEM_INIT")
                            .build());
                }
                rolePermissionRepository.saveAll(toSave);
            }
        }

        log.info("Role & permission initialization complete.");
    }

    private String formatActionDisplayName(String action, String resourceName) {
        return switch (action.toUpperCase()) {
            case "VIEW" -> "View " + resourceName;
            case "CREATE" -> "Create " + resourceName;
            case "EDIT" -> "Edit " + resourceName;
            case "DELETE" -> "Delete " + resourceName;
            case "PRINT" -> "Print " + resourceName;
            case "EXPORT" -> "Export " + resourceName;
            default -> action + " " + resourceName;
        };
    }

    private boolean shouldGrantByDefault(String roleName, String permKey) {
        // OWNER and ADMIN get everything
        if ("ROLE_OWNER".equalsIgnoreCase(roleName) || "ROLE_ADMIN".equalsIgnoreCase(roleName)) {
            return true;
        }

        if ("ROLE_SHOPKEEPER".equalsIgnoreCase(roleName)) {
            if (permKey.startsWith("roles_permissions") || permKey.startsWith("settings") || permKey.startsWith("users")) {
                return false;
            }
            if (permKey.endsWith(".delete")) {
                return false; // prevent shopkeepers from deleting by default
            }
            return true;
        }

        if ("ROLE_TAILOR".equalsIgnoreCase(roleName)) {
            return permKey.startsWith("dashboard") ||
                    permKey.startsWith("cutting") ||
                    permKey.startsWith("alterations") ||
                    permKey.startsWith("worker_allotment") ||
                    permKey.equals("orders.view") ||
                    permKey.equals("measurements.view");
        }

        if ("ROLE_WORKER".equalsIgnoreCase(roleName)) {
            return permKey.startsWith("dashboard") ||
                    permKey.equals("worker_allotment.view") ||
                    permKey.equals("cutting.view");
        }

        if ("ROLE_CUSTOMER".equalsIgnoreCase(roleName)) {
            return permKey.equals("orders.view");
        }

        return false;
    }
}
