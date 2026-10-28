package com.tallermecanico.workshop;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Define y valida los datos completos necesarios para crear o actualizar un cliente. */
public record client_request(
        @NotBlank @Size(min=2,max=120) @Pattern(regexp="^[\\p{L}][\\p{L} .'-]*$") String name,
        @NotBlank @Size(min=2,max=120) @Pattern(regexp="^[\\p{L}][\\p{L} .'-]*$") String paternal_surname,
        @NotBlank @Size(min=2,max=120) @Pattern(regexp="^[\\p{L}][\\p{L} .'-]*$") String maternal_surname,
        @NotBlank @Email @Size(max=190) String email,
        @NotBlank @Pattern(regexp="^\\+?[0-9 ()-]{10,20}$") String phone,
        @NotBlank @Pattern(regexp="^\\+?[0-9 ()-]{10,20}$") String personal_phone,
        @NotBlank @Pattern(regexp="^\\+?[0-9 ()-]{10,20}$") String work_phone,
        @NotBlank @Pattern(regexp="^\\+?[0-9 ()-]{10,20}$") String alternate_contact_phone,
        @NotBlank @Size(min=2,max=240) @Pattern(regexp="^[\\p{L}][\\p{L} .'-]*$") String alternate_contact_name,
        @NotBlank @Size(max=120) @Pattern(regexp="^[\\p{L}0-9 .,'#/-]{1,120}$") String street,
        @NotBlank @Size(max=20) @Pattern(regexp="^[\\p{L}0-9 .#/-]{1,20}$") String street_number,
        @NotBlank @Size(max=120) @Pattern(regexp="^[\\p{L}0-9 .,'#/-]{2,120}$") String colonia,
        @NotBlank @Size(max=100) @Pattern(regexp="^[\\p{L} .'-]{2,100}$") String state,
        @NotBlank @Size(max=100) @Pattern(regexp="^[\\p{L} .'-]{2,100}$") String municipality,
        @NotBlank @Pattern(regexp="^[0-9]{5}$") String postal_code,
        @NotEmpty List<@NotBlank @Pattern(regexp="^[0-9a-fA-F-]{36}$") String> workshop_ids
) {}
