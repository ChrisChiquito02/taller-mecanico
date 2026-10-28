package com.tallermecanico.vehicle;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Valida los campos JSON enviados en el alta del vehículo antes del caso de uso. */
public record vehicle_request(
        /** VIN alfanumérico de 17 caracteres, excluyendo letras inválidas de la norma VIN. */
        @NotBlank @Pattern(regexp="^[A-HJ-NPR-Z0-9]{17}$") String vin,
        /** Placa cruda que la Strategy normalizará y validará según el formato mexicano soportado. */
        @NotBlank @Size(min=5,max=10) @Pattern(regexp="^[A-Za-z0-9 -]+$") String plate,
        /** Marca legible del vehículo sin caracteres de control. */
        @NotBlank @Size(min=1,max=80) @Pattern(regexp="^[\\p{L}0-9][\\p{L}0-9 .&'-]*$") String make,
        /** Modelo legible del vehículo sin caracteres de control. */
        @NotBlank @Size(min=1,max=100) @Pattern(regexp="^[\\p{L}0-9][\\p{L}0-9 .&'/-]*$") String model,
        /** Año de fabricación admitido por el formulario y por el servidor. */
        @NotNull @Min(1886) @Max(2100) Integer year,
        /** Versión o paquete comercial del vehículo. */
        @NotBlank @Size(min=1,max=100) @Pattern(regexp="^[\\p{L}0-9][\\p{L}0-9 .&'/-]*$") String version,
        /** Color del vehículo con nombre de color textual. */
        @NotBlank @Size(min=2,max=50) @Pattern(regexp="^[\\p{L}][\\p{L} .'-]*$") String color,
        /** Kilometraje opcional no negativo que puede omitirse durante el registro. */
        @Min(0) @Max(2000000) Long mileage,
        /** UUID del cliente que deberá continuar activo al persistir el vehículo. */
        @NotBlank @Pattern(regexp="^[0-9a-fA-F-]{36}$") String client_id
) {}
