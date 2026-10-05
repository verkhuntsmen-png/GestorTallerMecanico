package com.refaccionaria.auth.service;

import com.refaccionaria.auth.dto.CustomerRegistrationRequest;
import com.refaccionaria.auth.dto.CustomerResponse;
import com.refaccionaria.auth.exception.DuplicateCustomerException;
import com.refaccionaria.auth.model.Customer;
import com.refaccionaria.auth.repository.CustomerRepository;
import com.refaccionaria.auth.repository.UserRepository;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.Period;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Facade that keeps the REST layer independent from customer persistence and file storage. */
@Service
public class CustomerFacade {
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final CustomerPhotoStorageService photoStorage;

    public CustomerFacade(CustomerRepository customerRepository, UserRepository userRepository,
            CustomerPhotoStorageService photoStorage) {
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.photoStorage = photoStorage;
    }

    @Transactional
    public CustomerResponse register(CustomerRegistrationRequest request, String authenticatedEmail) {
        LocalDate birthDate = request.getBirthDate();
        if (Period.between(birthDate, LocalDate.now()).getYears() != request.getAge()) {
            throw new IllegalArgumentException("La edad no coincide con la fecha de nacimiento");
        }

        String fullName = clean(request.getFullName());
        String normalizedName = normalizeName(fullName);
        String email = normalizeEmail(request.getEmail());
        String workEmail = optionalEmail(request.getWorkEmail());
        String personalPhone = request.getPersonalPhone();
        String workPhone = request.getWorkPhone();
        assertNoDuplicates(normalizedName, birthDate, email, workEmail, personalPhone, workPhone);

        String photoStorageName = photoStorage.store(request.getPhoto());
        try {
            Customer customer = new Customer();
            customer.setFullName(fullName);
            customer.setAlternateFullName(clean(request.getAlternateFullName()));
            customer.setNormalizedFullName(normalizedName);
            customer.setPersonalPhone(personalPhone);
            customer.setWorkPhone(workPhone);
            customer.setAge(request.getAge());
            customer.setBirthDate(birthDate);
            customer.setEmail(email);
            customer.setWorkEmail(workEmail);
            customer.setPhotoStorageName(photoStorageName);
            customer.setStreet(clean(request.getStreet()));
            customer.setNeighborhood(clean(request.getNeighborhood()));
            customer.setMunicipality(clean(request.getMunicipality()));
            customer.setState(clean(request.getState()));
            customer.setPostalCode(request.getPostalCode());
            customer.setCreatedBy(userRepository.findByEmailIgnoreCase(authenticatedEmail)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario autenticado no encontrado")));
            return toResponse(customerRepository.saveAndFlush(customer));
        } catch (DataIntegrityViolationException ex) {
            photoStorage.deleteQuietly(photoStorageName);
            throw new DuplicateCustomerException("Ya existe un cliente con datos de identidad o contacto registrados");
        }
    }

    private void assertNoDuplicates(String normalizedName, LocalDate birthDate, String email, String workEmail,
            String personalPhone, String workPhone) {
        if (customerRepository.existsByNormalizedFullNameAndBirthDate(normalizedName, birthDate)) {
            throw new DuplicateCustomerException("Ya existe un cliente con el mismo nombre y fecha de nacimiento");
        }
        if (customerRepository.existsByEmailIgnoreCase(email) || customerRepository.existsByWorkEmailIgnoreCase(email)) {
            throw new DuplicateCustomerException("El correo personal ya esta registrado");
        }
        if (workEmail != null && (customerRepository.existsByEmailIgnoreCase(workEmail)
                || customerRepository.existsByWorkEmailIgnoreCase(workEmail))) {
            throw new DuplicateCustomerException("El correo de trabajo ya esta registrado");
        }
        if (personalPhone.equals(workPhone) || customerRepository.existsByPersonalPhone(personalPhone)
                || customerRepository.existsByWorkPhone(personalPhone)) {
            throw new DuplicateCustomerException("El telefono personal ya esta registrado");
        }
        if (customerRepository.existsByPersonalPhone(workPhone) || customerRepository.existsByWorkPhone(workPhone)) {
            throw new DuplicateCustomerException("El telefono de trabajo ya esta registrado");
        }
    }

    private CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(customer.getId(), customer.getFullName(), customer.getAlternateFullName(),
                customer.getPersonalPhone(), customer.getWorkPhone(), customer.getAge(), customer.getBirthDate(),
                customer.getEmail(), customer.getWorkEmail(), customer.getStreet(), customer.getNeighborhood(),
                customer.getMunicipality(), customer.getState(), customer.getPostalCode(), customer.getCreatedAt(),
                customer.getCreatedBy().getEmail());
    }

    private String clean(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeName(String value) {
        return Normalizer.normalize(clean(value), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toUpperCase(Locale.ROOT);
    }

    private String normalizeEmail(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String optionalEmail(String value) {
        return value == null || value.isBlank() ? null : normalizeEmail(value);
    }
}
