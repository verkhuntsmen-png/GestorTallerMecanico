package com.refaccionaria.auth.repository;
import com.refaccionaria.auth.model.Vehiculo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {
    boolean existsByVin(String vin);
    Optional<Vehiculo> findByVin(String vin);
    List<Vehiculo> findAllByPlaca(String placa);
}
