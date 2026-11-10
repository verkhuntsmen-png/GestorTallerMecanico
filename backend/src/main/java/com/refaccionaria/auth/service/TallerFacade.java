package com.refaccionaria.auth.service;

import com.refaccionaria.auth.dto.TallerRegistrationRequest;
import com.refaccionaria.auth.dto.TallerResponse;
import com.refaccionaria.auth.model.Taller;
import com.refaccionaria.auth.repository.TallerRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Facade that centralizes workshop validation, persistence and photo storage. */
@Service
public class TallerFacade {
    private final TallerRepository tallerRepository;
    private final TallerPhotoStorageService photoStorage;

    public TallerFacade(TallerRepository tallerRepository, TallerPhotoStorageService photoStorage) {
        this.tallerRepository = tallerRepository;
        this.photoStorage = photoStorage;
    }

    @Transactional
    public TallerResponse create(TallerRegistrationRequest request) {
        String rfc = request.getRfc().trim().toUpperCase(Locale.ROOT);
        String razonSocial = clean(request.getRazonSocial());
        if (tallerRepository.existsByRfcIgnoreCase(rfc)) throw new IllegalArgumentException("Ya existe un taller con el RFC indicado");
        if (tallerRepository.existsByRazonSocialIgnoreCase(razonSocial)) throw new IllegalArgumentException("Ya existe un taller con la razon social indicada");

        String photoName = photoStorage.store(request.getFoto());
        try {
            Taller taller = new Taller();
            taller.setNombreTaller(clean(request.getNombreTaller()));
            taller.setRazonSocial(razonSocial);
            taller.setRfc(rfc);
            taller.setEmailContacto(request.getEmailContacto().trim().toLowerCase(Locale.ROOT));
            taller.setTelefono(request.getTelefono().trim());
            taller.setCalle(clean(request.getCalle()));
            taller.setMunicipio(clean(request.getMunicipio()));
            taller.setEstado(clean(request.getEstado()));
            taller.setCodigoPostal(request.getCodigoPostal().trim());
            taller.setColonia(clean(request.getColonia()));
            taller.setFotoUrl(photoName);
            return toResponse(tallerRepository.saveAndFlush(taller));
        } catch (DataIntegrityViolationException ex) {
            photoStorage.deleteQuietly(photoName);
            throw new IllegalArgumentException("RFC o razon social ya registrados");
        }
    }

    @Transactional(readOnly = true)
    public List<TallerResponse> listActive() {
        return tallerRepository.findByActivoTrueOrderByNombreTallerAsc().stream().map(this::toResponse).toList();
    }

    private TallerResponse toResponse(Taller taller) {
        return new TallerResponse(taller.getId(), taller.getNombreTaller(), taller.getRazonSocial(), taller.getRfc(),
                taller.getEmailContacto(), taller.getTelefono(), taller.getCalle(), taller.getMunicipio(), taller.getEstado(),
                taller.getCodigoPostal(), taller.getColonia(), taller.getFotoUrl(), taller.isActivo(), taller.getCreadoEn());
    }

    private String clean(String value) { return value.trim().replaceAll("\\s+", " "); }
}
