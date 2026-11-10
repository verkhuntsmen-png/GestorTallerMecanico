package com.refaccionaria.auth.repository;
import com.refaccionaria.auth.model.Modelo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ModeloRepository extends JpaRepository<Modelo, Long> {
    List<Modelo> findByMarcaIdOrderByNombreAsc(Long marcaId);
    Optional<Modelo> findByMarcaIdAndNombreIgnoreCase(Long marcaId, String nombre);
}
