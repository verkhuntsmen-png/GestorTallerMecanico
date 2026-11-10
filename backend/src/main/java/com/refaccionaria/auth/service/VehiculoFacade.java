package com.refaccionaria.auth.service;

import com.refaccionaria.auth.dto.VehiculoDtos.BrandResponse;
import com.refaccionaria.auth.dto.VehiculoDtos.CustomerScopeResponse;
import com.refaccionaria.auth.dto.VehiculoDtos.DetailResponse;
import com.refaccionaria.auth.dto.VehiculoDtos.ModelResponse;
import com.refaccionaria.auth.dto.VehiculoDtos.Request;
import com.refaccionaria.auth.dto.VehiculoDtos.Response;
import com.refaccionaria.auth.dto.VehiculoDtos.StatusRequest;
import com.refaccionaria.auth.dto.VehiculoDtos.UpdateRequest;
import com.refaccionaria.auth.dto.VehiculoDtos.WorkshopScopeResponse;
import com.refaccionaria.auth.model.Customer;
import com.refaccionaria.auth.model.Estatus;
import com.refaccionaria.auth.model.Marca;
import com.refaccionaria.auth.model.Modelo;
import com.refaccionaria.auth.model.Vehiculo;
import com.refaccionaria.auth.repository.CustomerRepository;
import com.refaccionaria.auth.repository.MarcaRepository;
import com.refaccionaria.auth.repository.ModeloRepository;
import com.refaccionaria.auth.repository.VehiculoRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coordinates vehicle catalog, registration, ownership lookup and lifecycle changes. */
@Service
public class VehiculoFacade {
    private final MarcaRepository marcas; private final ModeloRepository modelos; private final VehiculoRepository vehiculos;
    private final CustomerRepository customers; private final EstatusFacade estatusFacade;
    private final VehiculoAuditoriaService auditoria;
    public VehiculoFacade(MarcaRepository marcas, ModeloRepository modelos, VehiculoRepository vehiculos,
            CustomerRepository customers, EstatusFacade estatusFacade, VehiculoAuditoriaService auditoria) {
        this.marcas = marcas; this.modelos = modelos; this.vehiculos = vehiculos; this.customers = customers;
        this.estatusFacade = estatusFacade; this.auditoria = auditoria;
    }
    @Transactional(readOnly = true) public List<BrandResponse> brands() { return marcas.findAll().stream().map(m -> new BrandResponse(m.getId(), m.getNombre())).toList(); }
    @Transactional(readOnly = true) public List<ModelResponse> models(Long brandId) { return modelos.findByMarcaIdOrderByNombreAsc(brandId).stream().map(this::modelResponse).toList(); }
    @Transactional(readOnly = true)
    public DetailResponse detail(Long id, String vin, String placa) {
        int identifiers = (id != null ? 1 : 0) + (hasText(vin) ? 1 : 0) + (hasText(placa) ? 1 : 0);
        if (identifiers != 1) throw new IllegalArgumentException("Indica exactamente uno: id, VIN o placa");

        Vehiculo vehicle;
        if (id != null) {
            if (id <= 0) throw new IllegalArgumentException("El id debe ser mayor que cero");
            vehicle = vehiculos.findById(id).orElseThrow(() -> new IllegalArgumentException("Vehiculo no encontrado"));
        } else if (hasText(vin)) {
            vehicle = vehiculos.findByVin(normalizeVin(vin)).orElseThrow(() -> new IllegalArgumentException("Vehiculo no encontrado"));
        } else {
            List<Vehiculo> matches = vehiculos.findAllByPlaca(normalizePlate(placa));
            if (matches.isEmpty()) throw new IllegalArgumentException("Vehiculo no encontrado");
            if (matches.size() > 1) throw new IllegalArgumentException("La placa corresponde a mas de un vehiculo; consulta por VIN o id");
            vehicle = matches.getFirst();
        }
        return detailResponse(vehicle);
    }
    @Transactional public Response create(Request request) {
        String vin = request.vin().trim().toUpperCase(Locale.ROOT); if (vehiculos.existsByVin(vin)) throw new IllegalArgumentException("El VIN ya esta registrado");
        Customer customer = customers.findById(request.clienteId()).orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
        if (customer.getEstatus() == null || !customer.getEstatus().isActivo() || !"ACTIVO".equalsIgnoreCase(customer.getEstatus().getStrValor())) throw new IllegalArgumentException("Solo se pueden registrar vehiculos para clientes Activo");
        Marca marca = resolveBrand(request); Modelo modelo = resolveModel(request, marca); Estatus activo = estatusFacade.activeByValue("Activo");
        Vehiculo vehicle = new Vehiculo(); vehicle.setVin(vin); vehicle.setPlaca(request.placa().replaceAll("\\s+", "").toUpperCase(Locale.ROOT)); vehicle.setMarca(marca); vehicle.setModelo(modelo);
        vehicle.setAnio(request.anio()); vehicle.setVersion(required(request.version())); vehicle.setColor(required(request.color())); vehicle.setNumeroSerie(required(request.numeroSerie())); vehicle.setKilometraje(request.kilometraje());
        vehicle.setTipoCombustible(required(request.tipoCombustible())); vehicle.setNumeroCilindros(required(request.numeroCilindros())); vehicle.setTipoTransmision(required(request.tipoTransmision())); vehicle.setCliente(customer); vehicle.setEstatus(activo);
        return toResponse(vehiculos.save(vehicle));
    }
    @Transactional
    public Response update(Long id, UpdateRequest request, String actorEmail) {
        Vehiculo vehicle = findVehicle(id);
        VehiculoAuditoriaService.Snapshot before = auditoria.snapshot(vehicle);
        Marca marca = resolveBrand(request);
        Modelo modelo = resolveModel(request, marca);
        vehicle.setPlaca(normalizePlate(request.placa()));
        vehicle.setMarca(marca);
        vehicle.setModelo(modelo);
        vehicle.setAnio(request.anio());
        vehicle.setVersion(required(request.version()));
        vehicle.setColor(required(request.color()));
        vehicle.setNumeroSerie(required(request.numeroSerie()));
        vehicle.setKilometraje(request.kilometraje());
        vehicle.setTipoCombustible(required(request.tipoCombustible()));
        vehicle.setNumeroCilindros(required(request.numeroCilindros()));
        vehicle.setTipoTransmision(required(request.tipoTransmision()));
        Vehiculo saved = vehiculos.save(vehicle);
        auditoria.register(saved, actorEmail, "ACTUALIZACION", before, auditoria.snapshot(saved));
        return toResponse(saved);
    }
    @Transactional
    public Response updateStatus(Long id, StatusRequest request, String actorEmail) {
        Vehiculo vehicle = findVehicle(id);
        Estatus nextStatus = estatusFacade.activeById(request.statusId());
        if (vehicle.getEstatus().getId().equals(nextStatus.getId())) throw new IllegalArgumentException("El vehiculo ya tiene el estatus seleccionado");
        VehiculoAuditoriaService.Snapshot before = auditoria.snapshot(vehicle);
        vehicle.setEstatus(nextStatus);
        Vehiculo saved = vehiculos.save(vehicle);
        auditoria.register(saved, actorEmail, "CAMBIO_ESTADO", before, auditoria.snapshot(saved));
        return toResponse(saved);
    }
    private Marca resolveBrand(Request request) {
        if (request.marcaId() != null) return marcas.findById(request.marcaId()).orElseThrow(() -> new IllegalArgumentException("Marca no encontrada"));
        if (request.nuevaMarca() == null || request.nuevaMarca().isBlank()) throw new IllegalArgumentException("Selecciona o captura una marca");
        String name = request.nuevaMarca().trim().toUpperCase(Locale.ROOT); return marcas.findByNombreIgnoreCase(name).orElseGet(() -> { Marca brand = new Marca(); brand.setNombre(name); return marcas.save(brand); });
    }
    private Marca resolveBrand(UpdateRequest request) {
        if (request.marcaId() != null) return marcas.findById(request.marcaId()).orElseThrow(() -> new IllegalArgumentException("Marca no encontrada"));
        if (request.nuevaMarca() == null || request.nuevaMarca().isBlank()) throw new IllegalArgumentException("Selecciona o captura una marca");
        String name = request.nuevaMarca().trim().toUpperCase(Locale.ROOT); return marcas.findByNombreIgnoreCase(name).orElseGet(() -> { Marca brand = new Marca(); brand.setNombre(name); return marcas.save(brand); });
    }
    private Modelo resolveModel(Request request, Marca brand) {
        if (request.modeloId() != null) { Modelo model = modelos.findById(request.modeloId()).orElseThrow(() -> new IllegalArgumentException("Modelo no encontrado")); if (!model.getMarca().getId().equals(brand.getId())) throw new IllegalArgumentException("El modelo no pertenece a la marca seleccionada"); return model; }
        if (request.nuevoModelo() == null || request.nuevoModelo().isBlank()) throw new IllegalArgumentException("Selecciona o captura un modelo");
        String name = request.nuevoModelo().trim().toUpperCase(Locale.ROOT); return modelos.findByMarcaIdAndNombreIgnoreCase(brand.getId(), name).orElseGet(() -> { Modelo model = new Modelo(); model.setMarca(brand); model.setNombre(name); model.setTipoCombustible(blankToNull(request.tipoCombustible())); model.setNumeroCilindros(blankToNull(request.numeroCilindros())); model.setTipoTransmision(blankToNull(request.tipoTransmision())); return modelos.save(model); });
    }
    private Modelo resolveModel(UpdateRequest request, Marca brand) {
        if (request.modeloId() != null) { Modelo model = modelos.findById(request.modeloId()).orElseThrow(() -> new IllegalArgumentException("Modelo no encontrado")); if (!model.getMarca().getId().equals(brand.getId())) throw new IllegalArgumentException("El modelo no pertenece a la marca seleccionada"); return model; }
        if (request.nuevoModelo() == null || request.nuevoModelo().isBlank()) throw new IllegalArgumentException("Selecciona o captura un modelo");
        String name = request.nuevoModelo().trim().toUpperCase(Locale.ROOT); return modelos.findByMarcaIdAndNombreIgnoreCase(brand.getId(), name).orElseGet(() -> { Modelo model = new Modelo(); model.setMarca(brand); model.setNombre(name); model.setTipoCombustible(required(request.tipoCombustible())); model.setNumeroCilindros(required(request.numeroCilindros())); model.setTipoTransmision(required(request.tipoTransmision())); return modelos.save(model); });
    }
    private DetailResponse detailResponse(Vehiculo v) {
        Customer customer = v.getCliente();
        CustomerScopeResponse customerScope = new CustomerScopeResponse(customer.getId(), customer.getFullName());
        WorkshopScopeResponse workshopScope = customer.getTaller() == null ? null : new WorkshopScopeResponse(
                customer.getTaller().getId(), customer.getTaller().getNombreTaller(),
                customer.getTaller().getRazonSocial(), customer.getTaller().isActivo());
        return new DetailResponse(v.getId(), v.getVin(), v.getPlaca(), v.getMarca().getId(), v.getMarca().getNombre(),
                v.getModelo().getId(), v.getModelo().getNombre(), v.getAnio(), v.getVersion(), v.getColor(),
                v.getNumeroSerie(), v.getKilometraje(), v.getTipoCombustible(), v.getNumeroCilindros(),
                v.getTipoTransmision(), v.getEstatus().getStrValor(), customerScope, workshopScope);
    }
    private ModelResponse modelResponse(Modelo m) { return new ModelResponse(m.getId(), m.getNombre(), m.getTipoCombustible(), m.getNumeroCilindros(), m.getTipoTransmision()); }
    private Response toResponse(Vehiculo v) { return new Response(v.getId(), v.getVin(), v.getPlaca(), v.getMarca().getId(), v.getMarca().getNombre(), v.getModelo().getId(), v.getModelo().getNombre(), v.getAnio(), v.getVersion(), v.getColor(), v.getTipoCombustible(), v.getNumeroCilindros(), v.getTipoTransmision(), v.getCliente().getId(), v.getEstatus().getStrValor()); }
    private Vehiculo findVehicle(Long id) { if (id == null || id <= 0) throw new IllegalArgumentException("El id debe ser mayor que cero"); return vehiculos.findById(id).orElseThrow(() -> new IllegalArgumentException("Vehiculo no encontrado")); }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String required(String value) { String clean = blankToNull(value); if (clean == null) throw new IllegalArgumentException("Los datos del vehiculo no pueden ser nulos o vacios"); return clean; }
    private boolean hasText(String value) { return value != null && !value.isBlank(); }
    private String normalizeVin(String vin) { return vin.trim().toUpperCase(Locale.ROOT); }
    private String normalizePlate(String plate) { return plate.replaceAll("\\s+", "").toUpperCase(Locale.ROOT); }
}
