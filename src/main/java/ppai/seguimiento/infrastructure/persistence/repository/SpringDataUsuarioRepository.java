package ppai.seguimiento.infrastructure.persistence.repository;

import ppai.seguimiento.infrastructure.persistence.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SpringDataUsuarioRepository extends JpaRepository<UsuarioEntity, String> {

    @EntityGraph(attributePaths = "comisionMedica")
    @Query("select usuario from UsuarioEntity usuario where usuario.email = :email")
    Optional<UsuarioEntity> findWithCommissionByEmail(@Param("email") String email);
}
