package com.tallermecanico.workshop;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Entrada validada del único caso de uso de alta de talleres. */
public record create_request(
 @NotBlank @Size(max=120) @Pattern(regexp="^[\\p{L}0-9 .,'&()/-]{2,120}$") String name,
 @NotBlank @Pattern(regexp="^[A-ZÑ&]{3,4}[0-9]{6}[A-Z0-9]{3}$") String rfc,
 @NotBlank @Email @Size(max=254) String contact_email,
 @NotBlank @Pattern(regexp="^(activo|inactivo|suspendido|mantenimiento)$") String commercial_status,
 @NotBlank @Size(max=120) @Pattern(regexp="^[\\p{L}0-9 .,'#/-]{1,120}$") String street,
 @NotBlank @Size(max=20) @Pattern(regexp="^[\\p{L}0-9 .#/-]{1,20}$") String street_number,
 @NotBlank @Size(max=120) @Pattern(regexp="^[\\p{L}0-9 .,'#/-]{2,120}$") String colonia,
 @NotBlank @Pattern(regexp="^[0-9]{5}$") String postal_code,
 @NotBlank @Size(max=100) @Pattern(regexp="^[\\p{L} .'-]{2,100}$") String state,
 @NotBlank @Size(max=100) @Pattern(regexp="^[\\p{L} .'-]{2,100}$") String municipality
) {}
