package com.tallermecanico.role;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints protegidos para validar nombre duplicado y crear roles. */
@Validated
@RestController
@RequestMapping("/api/roles")
public class RoleController {
    private static final String ROLE_NAME_PATTERN = "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+(?:[ '-][A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+)*$";
    private final RoleService roleService;

    /** Recibe el caso de uso de roles. */
    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    /** Consulta duplicados para la validación de la pantalla antes del POST de creación. */
    @GetMapping("/exists")
    public RoleDtos.ExistsResponse exists(@RequestParam @Size(max = 80) @Pattern(regexp = ROLE_NAME_PATTERN) String name) {
        return new RoleDtos.ExistsResponse(roleService.existsByName(name));
    }

    /** Lista los roles creados para consultarlos desde la página del catálogo. */
    @GetMapping
    public java.util.List<RoleDtos.RoleResponse> list() { return roleService.list(); }

    /** Crea un rol después de validar la entrada y devuelve el registro persistido. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoleDtos.RoleResponse create(@Valid @RequestBody RoleDtos.CreateRequest request) {
        return roleService.create(request.name());
    }

    /** Actualiza el nombre de un rol existente después de validar el cuerpo entrante. */
    @PutMapping("/{id}")
    public RoleDtos.RoleResponse updateName(@PathVariable @Pattern(regexp = "^[0-9a-fA-F-]{36}$") String id,
                                            @Valid @RequestBody RoleDtos.UpdateRequest request) {
        return roleService.updateName(id, request.name());
    }

    /** Suspende o reactiva un rol sin borrar físicamente su registro. */
    @PatchMapping("/{id}/status")
    public RoleDtos.RoleResponse updateStatus(@PathVariable @Pattern(regexp = "^[0-9a-fA-F-]{36}$") String id,
                                              @Valid @RequestBody RoleDtos.StatusRequest request) {
        return roleService.updateStatus(id, request.active());
    }
}
