package com.refaccionaria.auth.repository;
import com.refaccionaria.auth.model.Marca;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface MarcaRepository extends JpaRepository<Marca, Long> { Optional<Marca> findByNombreIgnoreCase(String nombre); }
