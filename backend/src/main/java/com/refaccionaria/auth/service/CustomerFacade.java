package com.refaccionaria.auth.service;

import com.refaccionaria.auth.dto.CustomerRegistrationRequest;
import com.refaccionaria.auth.dto.CustomerResponse;
import com.refaccionaria.auth.dto.CustomerUpdateRequest;
import com.refaccionaria.auth.exception.DuplicateCustomerException;
import com.refaccionaria.auth.model.Customer;
import com.refaccionaria.auth.model.CustomerStatus;
import com.refaccionaria.auth.model.Estatus;
import com.refaccionaria.auth.model.Taller;
import com.refaccionaria.auth.repository.CustomerRepository;
import com.refaccionaria.auth.repository.TallerRepository;
import com.refaccionaria.auth.repository.UserRepository;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.Period;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Facade that keeps the REST layer independent from customer persistence and file storage. */
@Service
public class CustomerFacade {
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final TallerRepository tallerRepository;
    private final CustomerPhotoStorageService photoStorage;
    private final EstatusFacade estatusFacade;

    public CustomerFacade(CustomerRepository customerRepository, UserRepository userRepository,
            TallerRepository tallerRepository, CustomerPhotoStorageService photoStorage, EstatusFacade estatusFacade) {
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.tallerRepository = tallerRepository;
        this.photoStorage = photoStorage;
        this.estatusFacade = estatusFacade;
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
            Taller taller = findTaller(request.getTallerId());
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
            customer.setRfc(optionalRfc(request.getRfc()));
            customer.setCurp(request.getCurp().trim().toUpperCase(Locale.ROOT));
            customer.setTaller(taller);
            customer.setEstatus(estatusFacade.activeByValue("Activo"));
            customer.setCreatedBy(userRepository.findByEmailIgnoreCase(authenticatedEmail)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario autenticado no encontrado")));
            return toResponse(customerRepository.saveAndFlush(customer));
        } catch (DataIntegrityViolationException ex) {
            photoStorage.deleteQuietly(photoStorageName);
            throw new DuplicateCustomerException("Ya existe un cliente con datos de identidad o contacto registrados");
        }
    }

    /** Server-side pagination and sorting are deliberately delegated to Spring Data/JPA. */
    @Transactional(readOnly = true)
    public Page<CustomerResponse> list(Long tallerId, int page, int size, String sortBy, String sortDir) {
        if (!"nombre".equals(sortBy) && !"fullName".equals(sortBy)) {
            throw new IllegalArgumentException("Solo se permite ordenar por nombre");
        }
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 10), direction, "fullName");
        Page<Customer> customers = tallerId == null ? customerRepository.findAll(pageable)
                : customerRepository.findByTallerId(tallerId, pageable);
        return customers.map(this::toResponse);
    }

    @Transactional
    public CustomerResponse update(Long id, CustomerUpdateRequest request, boolean canChangeTaller) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
        if (Period.between(request.birthDate(), LocalDate.now()).getYears() != request.age()) {
            throw new IllegalArgumentException("La edad no coincide con la fecha de nacimiento");
        }
        String fullName = clean(request.fullName());
        String email = normalizeEmail(request.email());
        String workEmail = optionalEmail(request.workEmail());
        assertNoDuplicatesForUpdate(customer.getId(), normalizeName(fullName), request.birthDate(), email, workEmail,
                request.personalPhone(), request.workPhone());
        customer.setFullName(fullName);
        customer.setAlternateFullName(clean(request.alternateFullName()));
        customer.setNormalizedFullName(normalizeName(fullName));
        customer.setPersonalPhone(request.personalPhone());
        customer.setWorkPhone(request.workPhone());
        customer.setAge(request.age());
        customer.setBirthDate(request.birthDate());
        customer.setEmail(email);
        customer.setWorkEmail(workEmail);
        customer.setStreet(clean(request.street()));
        customer.setNeighborhood(clean(request.neighborhood()));
        customer.setMunicipality(clean(request.municipality()));
        customer.setState(clean(request.state()));
        customer.setPostalCode(request.postalCode());
        customer.setRfc(optionalRfc(request.rfc()));
        customer.setCurp(request.curp().trim().toUpperCase(Locale.ROOT));
        if (!canChangeTaller && (customer.getTaller() == null || !customer.getTaller().getId().equals(request.tallerId()))) {
            throw new IllegalArgumentException("Solo un administrador puede modificar el taller del cliente");
        }
        if (canChangeTaller) customer.setTaller(findTaller(request.tallerId()));
        return toResponse(customerRepository.save(customer));
    }

    @Transactional
    public CustomerResponse suspend(Long id) {
        return changeStatus(id, CustomerStatus.SUSPENDIDO, "Suspendido");
    }

    /** Restores a logically suspended customer without altering its history or assigned workshop. */
    @Transactional
    public CustomerResponse activate(Long id) {
        return changeStatus(id, CustomerStatus.ACTIVO, "Activo");
    }

    /** Applies an active catalog status without deleting the customer record. */
    @Transactional
    public CustomerResponse updateStatus(Long id, Long statusId) {
        Customer customer = customerRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
        Estatus status = estatusFacade.activeById(statusId);
        customer.setEstatus(status);
        if ("SUSPENDIDO".equalsIgnoreCase(status.getStrValor())) customer.setStatus(CustomerStatus.SUSPENDIDO);
        else if ("ACTIVO".equalsIgnoreCase(status.getStrValor())) customer.setStatus(CustomerStatus.ACTIVO);
        return toResponse(customerRepository.save(customer));
    }

    private CustomerResponse changeStatus(Long id, CustomerStatus status, String statusValue) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
        customer.setStatus(status);
        customer.setEstatus(estatusFacade.activeByValue(statusValue));
        return toResponse(customerRepository.save(customer));
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

    private void assertNoDuplicatesForUpdate(Long id, String normalizedName, LocalDate birthDate, String email,
            String workEmail, String personalPhone, String workPhone) {
        customerRepository.findAll().stream().filter(customer -> !customer.getId().equals(id)).forEach(customer -> {
            boolean repeatedIdentity = customer.getNormalizedFullName().equals(normalizedName)
                    && customer.getBirthDate().equals(birthDate);
            boolean repeatedEmail = customer.getEmail().equalsIgnoreCase(email)
                    || (workEmail != null && workEmail.equalsIgnoreCase(customer.getEmail()))
                    || (customer.getWorkEmail() != null && (customer.getWorkEmail().equalsIgnoreCase(email)
                    || (workEmail != null && customer.getWorkEmail().equalsIgnoreCase(workEmail))));
            boolean repeatedPhone = customer.getPersonalPhone().equals(personalPhone) || customer.getWorkPhone().equals(personalPhone)
                    || customer.getPersonalPhone().equals(workPhone) || customer.getWorkPhone().equals(workPhone);
            if (repeatedIdentity || repeatedEmail || repeatedPhone) {
                throw new DuplicateCustomerException("Los datos de contacto o identidad ya pertenecen a otro cliente");
            }
        });
        if (personalPhone.equals(workPhone)) throw new DuplicateCustomerException("Los telefonos no pueden ser iguales");
    }

    private CustomerResponse toResponse(Customer customer) {
        Taller taller = customer.getTaller();
        return new CustomerResponse(customer.getId(), customer.getFullName(), customer.getAlternateFullName(),
                customer.getPersonalPhone(), customer.getWorkPhone(), customer.getAge(), customer.getBirthDate(),
                customer.getEmail(), customer.getWorkEmail(), customer.getStreet(), customer.getNeighborhood(),
                customer.getMunicipality(), customer.getState(), customer.getPostalCode(),
                customer.getRfc(), customer.getCurp(), taller == null ? null : taller.getId(),
                taller == null ? "Sin taller asignado" : taller.getNombreTaller(),
                customer.getEstatus() == null ? customer.getStatus().name() : customer.getEstatus().getStrValor(), customer.getCreatedAt(), customer.getCreatedBy().getEmail());
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

    private String optionalRfc(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private Taller findTaller(Long tallerId) {
        return tallerRepository.findById(tallerId)
                .filter(Taller::isActivo)
                .orElseThrow(() -> new IllegalArgumentException("El taller seleccionado no existe o esta inactivo"));
    }
}
