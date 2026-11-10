package com.refaccionaria.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.web.multipart.MultipartFile;

public class CustomerRegistrationRequest {
    public static final String RFC = "^([A-ZÑ&]{3,4})([0-9]{2})(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])([A-Z0-9]{3})$";
    public static final String CURP = "^[A-Z][AEIOUX][A-Z]{2}\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])[HM](AS|BC|BS|CC|CL|CM|CS|CH|DF|DG|GT|GR|HG|JC|MC|MN|MS|NT|NL|OC|PL|QT|QR|SP|SL|SR|TC|TS|TL|VZ|YN|ZS)[B-DF-HJ-NP-TV-Z]{3}[A-Z\\d]\\d$";
    private static final String PERSON_NAME = "^[\\p{L}]+(?:[\\p{L} ]*[\\p{L}])?$";
    private static final String PHONE = "^\\+?[1-9]\\d{7,14}$";
    private static final String ADDRESS = "^[\\p{L}0-9 .,#-]+$";
    private static final String LOCATION = "^[\\p{L}0-9 .-]+$";

    @NotBlank @Size(max = 160) @Pattern(regexp = PERSON_NAME, message = "solo debe contener letras y espacios")
    private String fullName;
    @NotBlank @Size(max = 160) @Pattern(regexp = PERSON_NAME, message = "solo debe contener letras y espacios")
    private String alternateFullName;
    @NotBlank @Pattern(regexp = PHONE, message = "debe usar formato internacional, por ejemplo +525512345678")
    private String personalPhone;
    @NotBlank @Pattern(regexp = PHONE, message = "debe usar formato internacional, por ejemplo +525512345678")
    private String workPhone;
    @NotNull @Min(0) @Max(130)
    private Integer age;
    @NotNull @Past
    private LocalDate birthDate;
    @NotBlank @Email @Size(max = 160)
    private String email;
    @Email @Size(max = 160)
    private String workEmail;
    @NotNull
    private MultipartFile photo;
    @NotBlank @Size(max = 180) @Pattern(regexp = ADDRESS, message = "contiene caracteres no permitidos")
    private String street;
    @NotBlank @Size(max = 120) @Pattern(regexp = LOCATION, message = "contiene caracteres no permitidos")
    private String neighborhood;
    @NotBlank @Size(max = 120) @Pattern(regexp = PERSON_NAME, message = "solo debe contener letras y espacios")
    private String municipality;
    @NotBlank @Size(max = 120) @Pattern(regexp = PERSON_NAME, message = "solo debe contener letras y espacios")
    private String state;
    @NotBlank @Pattern(regexp = "^\\d{5}$", message = "debe tener cinco digitos")
    private String postalCode;
    @Pattern(regexp = RFC, message = "RFC mexicano invalido")
    private String rfc;
    @NotBlank @Pattern(regexp = CURP, message = "CURP mexicana invalida")
    private String curp;
    @NotNull
    private Long tallerId;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getAlternateFullName() { return alternateFullName; }
    public void setAlternateFullName(String alternateFullName) { this.alternateFullName = alternateFullName; }
    public String getPersonalPhone() { return personalPhone; }
    public void setPersonalPhone(String personalPhone) { this.personalPhone = personalPhone; }
    public String getWorkPhone() { return workPhone; }
    public void setWorkPhone(String workPhone) { this.workPhone = workPhone; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getWorkEmail() { return workEmail; }
    public void setWorkEmail(String workEmail) { this.workEmail = workEmail; }
    public MultipartFile getPhoto() { return photo; }
    public void setPhoto(MultipartFile photo) { this.photo = photo; }
    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }
    public String getNeighborhood() { return neighborhood; }
    public void setNeighborhood(String neighborhood) { this.neighborhood = neighborhood; }
    public String getMunicipality() { return municipality; }
    public void setMunicipality(String municipality) { this.municipality = municipality; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public String getRfc() { return rfc; }
    public void setRfc(String rfc) { this.rfc = rfc; }
    public String getCurp() { return curp; }
    public void setCurp(String curp) { this.curp = curp; }
    public Long getTallerId() { return tallerId; }
    public void setTallerId(Long tallerId) { this.tallerId = tallerId; }
}
