package com.tallermecanico.vehicle;

import com.tallermecanico.security.UuidV7;
import com.tallermecanico.user.User;
import com.tallermecanico.workshop.client;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Representa un vehículo activo o desactivado, su cliente y los metadatos de sus fotos. */
@Entity
@Table(name="vehicles",uniqueConstraints={@UniqueConstraint(name="uk_vehicles_vin",columnNames="vin"),@UniqueConstraint(name="uk_vehicles_plate",columnNames="plate")})
public class vehicle {
    /** UUIDv7 textual que funciona como identidad primaria sin autoincremento. */
    @Id @Column(length=36,nullable=false,updatable=false) private String id;
    /** Fecha UTC derivada del UUIDv7 que permite ordenar por momento de alta. */
    @Column(nullable=false,updatable=false) private Instant fecha;
    /** VIN en mayúsculas, único y de exactamente 17 caracteres. */
    @Column(nullable=false,length=17) private String vin;
    /** Placa mexicana normalizada y única para todas las filas activas e inactivas. */
    @Column(nullable=false,length=8) private String plate;
    /** Marca normalizada en mayúsculas para búsquedas consistentes. */
    @Column(nullable=false,length=80) private String make;
    /** Modelo normalizado en mayúsculas para búsquedas consistentes. */
    @Column(nullable=false,length=100) private String model;
    /** Año de fabricación solicitado en el formulario. */
    @Column(name="model_year",nullable=false) private int year;
    /** Versión del modelo normalizada. */
    @Column(nullable=false,length=100) private String version;
    /** Color normalizado del vehículo. */
    @Column(nullable=false,length=50) private String color;
    /** Kilometraje conocido o null cuando todavía no se conoce. */
    @Column private Long mileage;
    /** Estado lógico que conserva filas desactivadas para el historial. */
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=16) private vehicle_status status=vehicle_status.activo;
    /** Cliente obligatorio y activo elegido al crear el vehículo. */
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="client_id",nullable=false) private client owner;
    /** Mecánico futuro reservado en el esquema; se mantiene null en este alcance. */
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="mechanic_id") private User mechanic;
    /** Fotografías con borrado en cascada solo cuando se elimina físicamente el agregado. */
    @OneToMany(mappedBy="vehicle",cascade=CascadeType.ALL,orphanRemoval=true) private List<vehicle_photo> photos=new ArrayList<>();

    /** Constructor JPA usado exclusivamente por Hibernate para hidratar la entidad. */
    protected vehicle() {}

    /** Construye un vehículo normalizado y activo, vincula al cliente y agrega fotos validadas. */
    public vehicle(vehicle_request request,client owner,String normalized_vin,String normalized_plate,List<vehicle_photo> photos) {
        UUID uuid=UuidV7.generate();
        this.id=uuid.toString();
        this.fecha=Instant.ofEpochMilli((uuid.getMostSignificantBits()>>>16)&0xFFFFFFFFFFFFL);
        this.vin=normalized_vin;
        this.plate=normalized_plate;
        this.make=normalize_text(request.make());
        this.model=normalize_text(request.model());
        this.year=request.year();
        this.version=normalize_text(request.version());
        this.color=normalize_text(request.color());
        this.mileage=request.mileage();
        this.status=vehicle_status.activo;
        this.owner=owner;
        photos.forEach(this::add_photo);
    }

    /** Agrega una foto validada y sincroniza el lado propietario de la relación JPA. */
    public void add_photo(vehicle_photo photo) { photo.setVehicle(this); this.photos.add(photo); }
    /** Marca el vehículo inactivo sin eliminar registro, cliente, mecánico ni fotografías. */
    public void deactivate() { this.status=vehicle_status.inactivo; }
    /** Devuelve texto sin espacios externos, con espacios repetidos colapsados y mayúsculas estables. */
    private String normalize_text(String value) { return value.trim().replaceAll("\\s+"," ").toUpperCase(Locale.ROOT); }
    /** Devuelve el UUIDv7 del vehículo. */
    public String getId() { return id; }
    /** Devuelve la fecha UTC derivada de la identidad UUIDv7. */
    public Instant getFecha() { return fecha; }
    /** Devuelve el VIN normalizado. */
    public String getVin() { return vin; }
    /** Devuelve la placa normalizada. */
    public String getPlate() { return plate; }
    /** Devuelve la marca normalizada. */
    public String getMake() { return make; }
    /** Devuelve el modelo normalizado. */
    public String getModel() { return model; }
    /** Devuelve el año de fabricación. */
    public int getYear() { return year; }
    /** Devuelve la versión normalizada. */
    public String getVersion() { return version; }
    /** Devuelve el color normalizado. */
    public String getColor() { return color; }
    /** Devuelve el kilometraje opcional. */
    public Long getMileage() { return mileage; }
    /** Devuelve el estado lógico actual. */
    public vehicle_status getStatus() { return status; }
    /** Devuelve la asociación obligatoria del cliente. */
    public client getOwner() { return owner; }
    /** Devuelve el mecánico futuro o null mientras no se asigne. */
    public User getMechanic() { return mechanic; }
    /** Devuelve las fotos ordenadas por cómo fueron agregadas en la solicitud actual. */
    public List<vehicle_photo> getPhotos() { return photos; }
}
