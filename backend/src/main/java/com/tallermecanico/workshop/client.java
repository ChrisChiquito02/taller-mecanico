package com.tallermecanico.workshop;

import com.tallermecanico.security.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.ManyToMany;
import java.time.Instant;
import java.util.UUID;

/** Representa un cliente activo o suspendido y participa en la tabla junction de talleres. */
@Entity @Table(name="clients", uniqueConstraints=@UniqueConstraint(name="uk_clients_email", columnNames="email"))
public class client {
    @Id @Column(length=36, nullable=false, updatable=false) private String id;
    @Column(nullable=false, updatable=false) private Instant fecha;
    @Column(nullable=false, length=120) private String name;
    @Column(name="paternal_surname", nullable=false, length=120) private String paternal_surname;
    @Column(name="maternal_surname", nullable=false, length=120) private String maternal_surname;
    @Column(nullable=false, length=190) private String email;
    @Column(nullable=false, length=16) private String phone;
    @Column(name="personal_phone", nullable=false, length=16) private String personal_phone;
    @Column(name="work_phone", nullable=false, length=16) private String work_phone;
    @Column(name="alternate_contact_phone", nullable=false, length=16) private String alternate_contact_phone;
    @Column(name="alternate_contact_name", nullable=false, length=240) private String alternate_contact_name;
    @Column(nullable=false, length=120) private String street;
    @Column(name="street_number", nullable=false, length=20) private String street_number;
    @Column(nullable=false, length=120) private String colonia;
    @Column(nullable=false, length=100) private String state;
    @Column(nullable=false, length=100) private String municipality;
    @Column(name="postal_code", nullable=false, length=5) private String postal_code;
    @Column(nullable=false) private boolean active = true;
    /** Vínculos de taller mantenidos por el lado dueño para conservar la tabla junction existente. */
    @ManyToMany(mappedBy="clients") private java.util.Set<workshop> workshops = new java.util.LinkedHashSet<>();

    /** Constructor JPA requerido para materializar clientes persistidos. */ protected client() {}

    /** Construye un cliente con UUIDv7 y fecha derivada, usando datos ya normalizados. */
    public client(client_request data) {
        java.util.UUID uuid = com.tallermecanico.security.UuidV7.generate();
        this.id = uuid.toString();
        this.fecha = Instant.ofEpochMilli((uuid.getMostSignificantBits() >>> 16) & 0xFFFFFFFFFFFFL);
        update(data);
        this.active = true;
    }

    /** Reemplaza los datos editables de este cliente sin alterar su identidad, fecha ni estado lógico. */
    public void update(client_request data) {
        this.name=data.name().trim(); this.paternal_surname=data.paternal_surname().trim();
        this.maternal_surname=data.maternal_surname().trim(); this.email=data.email().trim().toLowerCase(java.util.Locale.ROOT);
        this.phone=normalize_phone(data.phone()); this.personal_phone=normalize_phone(data.personal_phone());
        this.work_phone=normalize_phone(data.work_phone()); this.alternate_contact_phone=normalize_phone(data.alternate_contact_phone());
        this.alternate_contact_name=data.alternate_contact_name().trim(); this.street=data.street().trim();
        this.street_number=data.street_number().trim(); this.colonia=data.colonia().trim();
        this.state=data.state().trim(); this.municipality=data.municipality().trim(); this.postal_code=data.postal_code().trim();
    }

    /** Normaliza teléfonos a dígitos y conserva solo el signo internacional inicial. */
    private String normalize_phone(String value) { return value.replaceAll("[ ()-]", ""); }
    /** Devuelve el identificador UUIDv7 del cliente. */ public String getId(){return id;}
    /** Devuelve la fecha UTC codificada en el UUIDv7. */ public Instant getFecha(){return fecha;}
    /** Devuelve el nombre del cliente. */ public String getName(){return name;}
    /** Devuelve el apellido paterno. */ public String getPaternal_surname(){return paternal_surname;}
    /** Devuelve el apellido materno. */ public String getMaternal_surname(){return maternal_surname;}
    /** Devuelve el correo de contacto. */ public String getEmail(){return email;}
    /** Devuelve el teléfono general. */ public String getPhone(){return phone;}
    /** Devuelve el teléfono personal. */ public String getPersonal_phone(){return personal_phone;}
    /** Devuelve el teléfono de trabajo. */ public String getWork_phone(){return work_phone;}
    /** Devuelve el teléfono del contacto alternativo. */ public String getAlternate_contact_phone(){return alternate_contact_phone;}
    /** Devuelve el nombre del contacto alternativo. */ public String getAlternate_contact_name(){return alternate_contact_name;}
    /** Devuelve la calle de residencia. */ public String getStreet(){return street;}
    /** Devuelve el número exterior. */ public String getStreet_number(){return street_number;}
    /** Devuelve la colonia. */ public String getColonia(){return colonia;}
    /** Devuelve el estado. */ public String getState(){return state;}
    /** Devuelve el municipio. */ public String getMunicipality(){return municipality;}
    /** Devuelve el código postal. */ public String getPostal_code(){return postal_code;}
    /** Indica si el cliente aparece en el listado activo. */ public boolean isActive(){return active;}
    /** Marca al cliente como suspendido sin borrar sus datos. */ public void deactivate(){this.active=false;}
    /** Devuelve los talleres relacionados para actualizar la tabla junction. */ public java.util.Set<workshop> getWorkshops(){return workshops;}
    /** Mantiene el lado inverso de la relación sincronizado en memoria. */ public void addWorkshop(workshop value){this.workshops.add(value);}
    /** Retira del lado inverso una asociación seleccionada previamente. */ public void removeWorkshop(workshop value){this.workshops.remove(value);}
}
