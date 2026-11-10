package com.refaccionaria.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.refaccionaria.auth.dto.VehiculoDtos.DetailResponse;
import com.refaccionaria.auth.dto.VehiculoDtos.Response;
import com.refaccionaria.auth.dto.VehiculoDtos.StatusRequest;
import com.refaccionaria.auth.dto.VehiculoDtos.UpdateRequest;
import com.refaccionaria.auth.model.Customer;
import com.refaccionaria.auth.model.Estatus;
import com.refaccionaria.auth.model.Marca;
import com.refaccionaria.auth.model.Modelo;
import com.refaccionaria.auth.model.Taller;
import com.refaccionaria.auth.model.Vehiculo;
import com.refaccionaria.auth.repository.VehiculoRepository;
import com.refaccionaria.auth.repository.EstatusRepository;
import com.refaccionaria.auth.repository.MarcaRepository;
import com.refaccionaria.auth.repository.ModeloRepository;
import com.refaccionaria.auth.repository.VehiculoAuditoriaRepository;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class VehiculoFacadeTest {
    @Test
    void findsVehicleByVinAndIncludesCustomerAndWorkshopScope() {
        Vehiculo vehicle = vehicleWithScope();
        VehiculoFacade facade = facadeWith(Optional.of(vehicle), List.of());

        DetailResponse detail = facade.detail(null, "1hgcm82633a004352", null);

        assertEquals("1HGCM82633A004352", detail.vin());
        assertEquals("Cliente de prueba", detail.cliente().nombreCompleto());
        assertEquals("Taller Centro", detail.taller().nombreTaller());
    }

    @Test
    void rejectsAmbiguousPlateMatches() {
        VehiculoFacade facade = facadeWith(Optional.empty(), List.of(new Vehiculo(), new Vehiculo()));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> facade.detail(null, null, "ABC-123"));

        assertEquals("La placa corresponde a mas de un vehiculo; consulta por VIN o id", exception.getMessage());
    }

    @Test
    void requiresExactlyOneSearchIdentifier() {
        VehiculoFacade facade = facadeWith(Optional.empty(), List.of());
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> facade.detail(null, "1HGCM82633A004352", "ABC-123"));

        assertEquals("Indica exactamente uno: id, VIN o placa", exception.getMessage());
    }

    @Test
    void updatesVehicleWithoutChangingVinAndCreatesAuditEvent() {
        Vehiculo vehicle = vehicleWithScope();
        List<Object> auditEvents = new ArrayList<>();
        VehiculoFacade facade = editableFacade(vehicle, vehicle.getEstatus(), auditEvents);

        Response updated = facade.update(vehicle.getId(), new UpdateRequest("XYZ-987", 10L, 20L, null, null,
                2025, "Touring", "Azul", "SERIE-2", 1200, "Gasolina", "4", "Automatica"),
                "recepcion@taller.local");

        assertEquals("1HGCM82633A004352", updated.vin());
        assertEquals("XYZ-987", updated.placa());
        assertEquals("Touring", vehicle.getVersion());
        assertEquals(1, auditEvents.size());
    }

    @Test
    void changesVehicleStatusAndCreatesAuditEvent() {
        Vehiculo vehicle = vehicleWithScope();
        Estatus suspended = new Estatus();
        suspended.setStrValor("Suspendido");
        suspended.setActivo(true);
        setId(suspended, 2L);
        List<Object> auditEvents = new ArrayList<>();
        VehiculoFacade facade = editableFacade(vehicle, suspended, auditEvents);

        Response updated = facade.updateStatus(vehicle.getId(), new StatusRequest(2L), "admin@taller.local");

        assertEquals("Suspendido", updated.estatus());
        assertEquals(1, auditEvents.size());
    }

    private VehiculoFacade facadeWith(Optional<Vehiculo> byVin, List<Vehiculo> byPlate) {
        VehiculoRepository repository = (VehiculoRepository) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[] {VehiculoRepository.class}, (proxy, method, arguments) -> {
                    if ("findByVin".equals(method.getName())) return byVin;
                    if ("findAllByPlaca".equals(method.getName())) return byPlate;
                    if ("findById".equals(method.getName())) return Optional.empty();
                    throw new UnsupportedOperationException(method.getName());
                });
        return new VehiculoFacade(null, null, repository, null, null, null);
    }

    private VehiculoFacade editableFacade(Vehiculo vehicle, Estatus selectedStatus, List<Object> auditEvents) {
        Marca brand = vehicle.getMarca();
        Modelo model = vehicle.getModelo();
        VehiculoRepository vehicles = (VehiculoRepository) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] {VehiculoRepository.class}, (proxy, method, arguments) -> {
                    if ("findById".equals(method.getName())) return Optional.of(vehicle);
                    if ("save".equals(method.getName())) return arguments[0];
                    throw new UnsupportedOperationException(method.getName());
                });
        MarcaRepository brands = (MarcaRepository) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] {MarcaRepository.class}, (proxy, method, arguments) -> {
                    if ("findById".equals(method.getName())) return Optional.of(brand);
                    throw new UnsupportedOperationException(method.getName());
                });
        ModeloRepository models = (ModeloRepository) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] {ModeloRepository.class}, (proxy, method, arguments) -> {
                    if ("findById".equals(method.getName())) return Optional.of(model);
                    throw new UnsupportedOperationException(method.getName());
                });
        EstatusRepository statuses = (EstatusRepository) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] {EstatusRepository.class}, (proxy, method, arguments) -> {
                    if ("findById".equals(method.getName())) return Optional.of(selectedStatus);
                    throw new UnsupportedOperationException(method.getName());
                });
        VehiculoAuditoriaRepository audits = (VehiculoAuditoriaRepository) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] {VehiculoAuditoriaRepository.class}, (proxy, method, arguments) -> {
                    if ("save".equals(method.getName())) { auditEvents.add(arguments[0]); return arguments[0]; }
                    throw new UnsupportedOperationException(method.getName());
                });
        return new VehiculoFacade(brands, models, vehicles, null, new EstatusFacade(statuses),
                new VehiculoAuditoriaService(audits, new ObjectMapper()));
    }

    private Vehiculo vehicleWithScope() {
        Marca brand = new Marca();
        brand.setNombre("HONDA");
        setId(brand, 10L);
        Modelo model = new Modelo();
        model.setNombre("ACCORD");
        model.setMarca(brand);
        setId(model, 20L);
        Estatus status = new Estatus();
        status.setStrValor("Activo");
        status.setActivo(true);
        setId(status, 1L);
        Taller workshop = new Taller();
        workshop.setNombreTaller("Taller Centro");
        workshop.setRazonSocial("Taller Centro SA de CV");
        setId(workshop, 30L);
        Customer customer = new Customer();
        customer.setFullName("Cliente de prueba");
        customer.setTaller(workshop);
        setId(customer, 40L);
        Vehiculo vehicle = new Vehiculo();
        vehicle.setVin("1HGCM82633A004352");
        vehicle.setPlaca("ABC-123");
        vehicle.setMarca(brand);
        vehicle.setModelo(model);
        vehicle.setAnio(2024);
        vehicle.setCliente(customer);
        vehicle.setEstatus(status);
        vehicle.setVersion("Base");
        vehicle.setColor("Blanco");
        vehicle.setNumeroSerie("SERIE-1");
        vehicle.setKilometraje(1000);
        vehicle.setTipoCombustible("Gasolina");
        vehicle.setNumeroCilindros("4");
        vehicle.setTipoTransmision("Manual");
        setId(vehicle, 50L);
        return vehicle;
    }

    private void setId(Object entity, Long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
