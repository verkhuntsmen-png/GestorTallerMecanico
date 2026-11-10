package com.refaccionaria.auth.repository;
import com.refaccionaria.auth.model.Estatus;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface EstatusRepository extends JpaRepository<Estatus, Long> {
    Optional<Estatus> findByStrValorIgnoreCase(String strValor);
    boolean existsByStrValorIgnoreCase(String strValor);
    List<Estatus> findByActivoTrueOrderByStrValorAsc();
}
