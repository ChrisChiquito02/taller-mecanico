package com.tallermecanico.workshop;

import com.tallermecanico.security.UuidV7;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** Entidad de dominio que representa un taller y su relación muchos a muchos con clientes. */
@Entity
@Table(name="workshops", uniqueConstraints={@UniqueConstraint(name="uk_workshops_rfc", columnNames="rfc"), @UniqueConstraint(name="uk_workshops_idempotency_key", columnNames="idempotency_key")})
public class workshop {
    @Id @Column(length=36, nullable=false, updatable=false) private String id;
    @Column(nullable=false, length=120) private String name;
    @Column(nullable=false, length=13) private String rfc;
    @Column(nullable=false, length=254) private String contact_email;
    @Column(nullable=false, length=500) private String photo_path;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=24) private workshop_status commercial_status;
    @Column(nullable=false, length=120) private String street;
    @Column(nullable=false, length=20) private String street_number;
    @Column(nullable=false, length=120) private String colonia;
    @Column(nullable=false, length=5) private String postal_code;
    @Column(nullable=false, length=100) private String state;
    @Column(nullable=false, length=100) private String municipality;
    @Column(nullable=false, length=36, updatable=false) private String idempotency_key;
    @Column(nullable=false, length=64, updatable=false) private String request_hash;
    @Column(nullable=false, updatable=false) private Instant fecha;
    @ManyToMany
    @JoinTable(name="workshop_clients", joinColumns=@JoinColumn(name="workshop_id"), inverseJoinColumns=@JoinColumn(name="client_id"))
    private Set<client> clients = new LinkedHashSet<>();

    /** Constructor requerido por JPA. */
    protected workshop() {}
    /** Construye el agregado con valores ya validados y un UUIDv7 con su fecha derivada. */
    public workshop(create_request request, String photo_path, String idempotency_key, String request_hash) {
        UUID uuid=UuidV7.generate(); this.id=uuid.toString(); this.fecha=Instant.ofEpochMilli((uuid.getMostSignificantBits()>>>16)&0xFFFFFFFFFFFFL);
        this.name=request.name().trim(); this.rfc=request.rfc().trim().toUpperCase(); this.contact_email=request.contact_email().trim().toLowerCase();
        this.photo_path=photo_path; this.commercial_status=workshop_status.valueOf(request.commercial_status()); this.street=request.street().trim();
        this.street_number=request.street_number().trim(); this.colonia=request.colonia().trim(); this.postal_code=request.postal_code().trim();
        this.state=request.state().trim(); this.municipality=request.municipality().trim(); this.idempotency_key=idempotency_key; this.request_hash=request_hash;
    }
    /** Devuelve la identidad UUIDv7 del taller. */ public String getId(){return id;}
    /** Devuelve el nombre del taller. */ public String getName(){return name;}
    /** Devuelve el RFC normalizado. */ public String getRfc(){return rfc;}
    /** Devuelve el correo de contacto. */ public String getContact_email(){return contact_email;}
    /** Devuelve la ruta relativa de la fotografía. */ public String getPhoto_path(){return photo_path;}
    /** Devuelve el estado comercial. */ public workshop_status getCommercial_status(){return commercial_status;}
    /** Devuelve la calle. */ public String getStreet(){return street;}
    /** Devuelve el número exterior. */ public String getStreet_number(){return street_number;}
    /** Devuelve la colonia. */ public String getColonia(){return colonia;}
    /** Devuelve el código postal. */ public String getPostal_code(){return postal_code;}
    /** Devuelve el estado. */ public String getState(){return state;}
    /** Devuelve el municipio. */ public String getMunicipality(){return municipality;}
    /** Devuelve la fecha UTC codificada en el UUIDv7. */ public Instant getFecha(){return fecha;}
    /** Devuelve la huella del contenido asociado a la clave de idempotencia. */ public String getRequest_hash(){return request_hash;}
    /** Vincula un cliente nuevo al taller como lado propietario de la relación junction. */ public void addClient(client value){this.clients.add(value);value.addWorkshop(this);}
    /** Retira la relación junction con el cliente sin borrar el registro del cliente. */ public void removeClient(client value){this.clients.remove(value);value.removeWorkshop(this);}
}
