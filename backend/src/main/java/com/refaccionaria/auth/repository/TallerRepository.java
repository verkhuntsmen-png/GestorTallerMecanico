package com.refaccionaria.auth.repository;

import com.refaccionaria.auth.model.Taller;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** JPA gateway for workshop persistence; Spring Data binds every parameter safely. */
public interface TallerRepository extends JpaRepository<Taller, Long> {
    boolean existsByRfcIgnoreCase(String rfc);
    boolean existsByRazonSocialIgnoreCase(String razonSocial);
    List<Taller> findByActivoTrueOrderByNombreTallerAsc();
}
