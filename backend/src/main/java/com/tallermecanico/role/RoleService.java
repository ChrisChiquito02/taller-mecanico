package com.tallermecanico.role;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Ejecuta el caso de uso solicitado: validar duplicados y crear un rol. */
@Service
public class RoleService {
    private final RoleRepository roles;
    private final RoleFactory roleFactory;

    /** Recibe el repositorio y el Factory Method que construye los objetos Rol. */
    public RoleService(RoleRepository roles, RoleFactory roleFactory) {
        this.roles = roles;
        this.roleFactory = roleFactory;
    }

    /** Comprueba si ya existe un rol con el mismo nombre, ignorando mayúsculas. */
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return roles.existsByNameIgnoreCase(name.trim());
    }

    /** Consulta los roles existentes en orden cronológico descendente para su visualización. */
    @Transactional(readOnly = true)
    public java.util.List<RoleDtos.RoleResponse> list() {
        return roles.findAllByOrderByFechaDesc().stream()
                .map(role -> new RoleDtos.RoleResponse(role.getId(), role.getName(), role.getFecha(), role.isActive())).toList();
    }

    /** Valida duplicados, crea el objeto mediante Factory Method y lo persiste. */
    @Transactional
    public RoleDtos.RoleResponse create(String name) {
        String normalizedName = name.trim().replaceAll("\\s+", " ");
        if (roles.existsByNameIgnoreCase(normalizedName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un rol con ese nombre.");
        }
        try {
            Role role = roles.saveAndFlush(roleFactory.createRole(normalizedName));
            return new RoleDtos.RoleResponse(role.getId(), role.getName(), role.getFecha(), role.isActive());
        } catch (DataIntegrityViolationException duplicateRace) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un rol con ese nombre.");
        }
    }

    /** Renombra un rol existente después de normalizar y validar que el nombre siga disponible. */
    @Transactional
    public RoleDtos.RoleResponse updateName(String id, String name) {
        Role role = roles.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el rol solicitado."));
        String normalizedName = name.trim().replaceAll("\\s+", " ");
        if (roles.existsByNameIgnoreCaseAndIdNot(normalizedName, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un rol con ese nombre.");
        }
        role.setName(normalizedName);
        try {
            Role saved = roles.saveAndFlush(role);
            return new RoleDtos.RoleResponse(saved.getId(), saved.getName(), saved.getFecha(), saved.isActive());
        } catch (DataIntegrityViolationException duplicateRace) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un rol con ese nombre.");
        }
    }

    /** Suspende o reactiva un rol mediante borrado lógico, sin eliminar su registro. */
    @Transactional
    public RoleDtos.RoleResponse updateStatus(String id, boolean active) {
        Role role = roles.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el rol solicitado."));
        role.setActive(active);
        Role saved = roles.save(role);
        return new RoleDtos.RoleResponse(saved.getId(), saved.getName(), saved.getFecha(), saved.isActive());
    }
}
