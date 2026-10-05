package com.refaccionaria.auth.repository;

import com.refaccionaria.auth.model.Customer;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByWorkEmailIgnoreCase(String workEmail);
    boolean existsByPersonalPhone(String personalPhone);
    boolean existsByWorkPhone(String workPhone);
    boolean existsByNormalizedFullNameAndBirthDate(String normalizedFullName, LocalDate birthDate);
}
