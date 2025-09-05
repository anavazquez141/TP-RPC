package RpcDonaciones.entities;

import jakarta.validation.Valid;
import RpcDonaciones.entities.Rol;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    @Column(length = 20)
    protected String nombre;

    @Column(length = 20)
    protected String apellido;

    @Column(length = 20)
    protected String telefono;

    @Column(nullable = false)
    protected String clave;

    @Column(length = 20)
    protected String email;

    protected boolean estado;

    @ManyToMany(fetch = FetchType.EAGER, targetEntity = Rol.class)
    @JoinTable(name = "usuarios_rol", joinColumns = @JoinColumn(name = "usuario_id"), inverseJoinColumns = @JoinColumn(name = "rol_id"))
    @OrderBy("id ASC")
    private Set<Rol> rolUsuario;

    public void agregarRoles(Rol rol) {
        if (this.rolUsuario == null) {
            this.rolUsuario = new HashSet<>();
        }
        this.rolUsuario.add(rol);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.rolUsuario.stream()
                .map(role -> new SimpleGrantedAuthority(role.getType().getPrefixedName())).collect(Collectors.toList());
    }

    @Override
    public String getUsername() {
        return this.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return !this.estado;
    }

    @Override
    public String getPassword() {
        return this.clave;
    }
}
