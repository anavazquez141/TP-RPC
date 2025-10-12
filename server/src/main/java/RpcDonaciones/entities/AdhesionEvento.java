package RpcDonaciones.entities;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "adhesiones_evento")
public class AdhesionEvento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String idEvento;
    private String idOrganizacion;
    private String idVoluntario;
    private String nombre;
    private String apellido;
    private String telefono;
    private String email;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaAdhesion;
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIdEvento() {
        return idEvento;
    }

    public void setIdEvento(String idEvento) {
        this.idEvento = idEvento;
    }

    public String getIdOrganizacion() {
        return idOrganizacion;
    }

    public void setIdOrganizacion(String idOrganizacion) {
        this.idOrganizacion = idOrganizacion;
    }

    public String getIdVoluntario() {
        return idVoluntario;
    }

    public void setIdVoluntario(String idVoluntario) {
        this.idVoluntario = idVoluntario;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Date getFechaAdhesion() {
        return fechaAdhesion;
    }

    public void setFechaAdhesion(Date fechaAdhesion) {
        this.fechaAdhesion = fechaAdhesion;
    }
}