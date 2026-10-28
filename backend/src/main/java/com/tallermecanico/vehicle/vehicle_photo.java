package com.tallermecanico.vehicle;

import com.tallermecanico.security.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Persiste metadatos de una foto del vehículo; los bytes viven en el volumen Docker de uploads. */
@Entity
@Table(name="vehicle_photos")
public class vehicle_photo {
    /** UUIDv7 textual de la foto para identificarla sin autoincremento. */
    @Id @Column(length=36,nullable=false,updatable=false) private String id;
    /** Fecha UTC extraída del timestamp del UUIDv7 para consultas cronológicas. */
    @Column(nullable=false,updatable=false) private Instant fecha;
    /** Nombre original saneado que se muestra al personal autorizado. */
    @Column(name="file_name",nullable=false,length=255) private String file_name;
    /** Ruta relativa dentro del volumen persistente de fotografías. */
    @Column(name="storage_path",nullable=false,length=500) private String storage_path;
    /** MIME real validado al decodificar la imagen. */
    @Column(name="content_type",nullable=false,length=32) private String content_type;
    /** Tamaño individual de la fotografía en bytes. */
    @Column(nullable=false) private long size_bytes;
    /** Vehículo dueño del metadato; retirar el vehículo de la vista no elimina la imagen. */
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="vehicle_id",nullable=false) private vehicle vehicle;

    /** Constructor JPA requerido para cargar los metadatos persistidos. */
    protected vehicle_photo() {}

    /** Crea metadatos de imagen validados y genera su identidad UUIDv7 con fecha correlacionada. */
    public vehicle_photo(String file_name,String storage_path,String content_type,long size_bytes) {
        UUID uuid=UuidV7.generate();
        this.id=uuid.toString();
        this.fecha=Instant.ofEpochMilli((uuid.getMostSignificantBits()>>>16)&0xFFFFFFFFFFFFL);
        this.file_name=file_name;
        this.storage_path=storage_path;
        this.content_type=content_type;
        this.size_bytes=size_bytes;
    }

    /** Asocia este metadato a su agregado dueño durante la construcción por Factory Method. */
    public void setVehicle(vehicle value) { this.vehicle=value; }
    /** Devuelve el UUIDv7 de la fotografía. */
    public String getId() { return id; }
    /** Devuelve la fecha UTC codificada en el UUIDv7. */
    public Instant getFecha() { return fecha; }
    /** Devuelve el nombre original saneado. */
    public String getFile_name() { return file_name; }
    /** Devuelve la ruta interna persistente de la imagen. */
    public String getStorage_path() { return storage_path; }
    /** Devuelve el tipo MIME real de la imagen. */
    public String getContent_type() { return content_type; }
    /** Devuelve el tamaño de la imagen. */
    public long getSize_bytes() { return size_bytes; }
}
