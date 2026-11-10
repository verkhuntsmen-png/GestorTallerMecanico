package com.refaccionaria.auth.repository;

import com.refaccionaria.auth.model.Customer;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByWorkEmailIgnoreCase(String workEmail);
    boolean existsByPersonalPhone(String personalPhone);
    boolean existsByWorkPhone(String workPhone);
    boolean existsByNormalizedFullNameAndBirthDate(String normalizedFullName, LocalDate birthDate);
    Page<Customer> findByTallerId(Long tallerId, Pageable pageable);

    /** Converts the temporary value emitted by an old MySQL schema migration into an unassigned workshop. */
    @Modifying
    @Transactional
    @Query(value = "UPDATE customers SET taller_id = NULL WHERE taller_id = 0", nativeQuery = true)
    void clearLegacyInvalidTallerIds();

    @Modifying
    @Transactional
    @Query(value = "UPDATE customers SET status_id = :statusId WHERE status_id IS NULL", nativeQuery = true)
    void assignMissingStatusId(@Param("statusId") Long statusId);
}
