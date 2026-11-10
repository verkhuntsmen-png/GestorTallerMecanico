package com.refaccionaria.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

/** Multipart contract for creating a workshop with its representative photo. */
public class TallerRegistrationRequest {
    public static final String RFC = "^([A-ZÑ&]{3,4})([0-9]{2})(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])([A-Z0-9]{3})$";
    private static final String PHONE = "^\\+?[1-9]\\d{7,14}$";
    private static final String ADDRESS = "^[\\p{L}0-9 .,#-]+$";
    private static final String LOCATION = "^[\\p{L}0-9 .-]+$";

    @NotBlank @Size(max = 160) private String nombreTaller;
    @NotBlank @Size(max = 180) private String razonSocial;
    @NotBlank @Pattern(regexp = RFC, message = "RFC mexicano invalido") private String rfc;
    @NotBlank @Email @Size(max = 160) private String emailContacto;
    @NotBlank @Pattern(regexp = PHONE, message = "debe usar formato internacional, por ejemplo +525512345678") private String telefono;
    @NotBlank @Size(max = 180) @Pattern(regexp = ADDRESS, message = "contiene caracteres no permitidos") private String calle;
    @NotBlank @Size(max = 120) @Pattern(regexp = LOCATION, message = "contiene caracteres no permitidos") private String municipio;
    @NotBlank @Size(max = 120) @Pattern(regexp = LOCATION, message = "contiene caracteres no permitidos") private String estado;
    @NotBlank @Pattern(regexp = "^\\d{5}$", message = "debe tener cinco digitos") private String codigoPostal;
    @NotBlank @Size(max = 120) @Pattern(regexp = LOCATION, message = "contiene caracteres no permitidos") private String colonia;
    @NotNull private MultipartFile foto;

    public String getNombreTaller() { return nombreTaller; }
    public void setNombreTaller(String nombreTaller) { this.nombreTaller = nombreTaller; }
    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }
    public String getRfc() { return rfc; }
    public void setRfc(String rfc) { this.rfc = rfc; }
    public String getEmailContacto() { return emailContacto; }
    public void setEmailContacto(String emailContacto) { this.emailContacto = emailContacto; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getCalle() { return calle; }
    public void setCalle(String calle) { this.calle = calle; }
    public String getMunicipio() { return municipio; }
    public void setMunicipio(String municipio) { this.municipio = municipio; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getCodigoPostal() { return codigoPostal; }
    public void setCodigoPostal(String codigoPostal) { this.codigoPostal = codigoPostal; }
    public String getColonia() { return colonia; }
    public void setColonia(String colonia) { this.colonia = colonia; }
    public MultipartFile getFoto() { return foto; }
    public void setFoto(MultipartFile foto) { this.foto = foto; }
}
