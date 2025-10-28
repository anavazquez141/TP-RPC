package GRSDonaciones.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParticipacionResumen {
    private String usuario;
    private int totalEventos;
}