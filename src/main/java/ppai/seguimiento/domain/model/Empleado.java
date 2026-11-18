package ppai.seguimiento.domain.model;

public record Empleado(
        String nombre,
        String apellido,
        String email,
        ComisionMedica comisionMedicaAsignada
) {
}
