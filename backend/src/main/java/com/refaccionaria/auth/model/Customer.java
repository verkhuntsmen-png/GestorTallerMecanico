package com.refaccionaria.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "customers", uniqueConstraints = {
        @UniqueConstraint(name = "uk_customer_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_customer_work_email", columnNames = "workEmail"),
        @UniqueConstraint(name = "uk_customer_personal_phone", columnNames = "personalPhone"),
        @UniqueConstraint(name = "uk_customer_work_phone", columnNames = "workPhone"),
        @UniqueConstraint(name = "uk_customer_identity", columnNames = {"normalizedFullName", "birthDate"})
})
public class Customer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 160)
    private String fullName;
    @Column(nullable = false, length = 160)
    private String alternateFullName;
    @Column(nullable = false, length = 160)
    private String normalizedFullName;
    @Column(nullable = false, length = 16)
    private String personalPhone;
    @Column(nullable = false, length = 16)
    private String workPhone;
    @Column(nullable = false)
    private Integer age;
    @Column(nullable = false)
    private LocalDate birthDate;
    @Column(nullable = false, length = 160)
    private String email;
    @Column(length = 160)
    private String workEmail;
    @Column(nullable = false, length = 120)
    private String photoStorageName;
    @Column(nullable = false, length = 180)
    private String street;
    @Column(nullable = false, length = 120)
    private String neighborhood;
    @Column(nullable = false, length = 120)
    private String municipality;
    @Column(nullable = false, length = 120)
    private String state;
    @Column(nullable = false, length = 5)
    private String postalCode;
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false, updatable = false)
    private AppUser createdBy;

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getAlternateFullName() { return alternateFullName; }
    public void setAlternateFullName(String alternateFullName) { this.alternateFullName = alternateFullName; }
    public String getNormalizedFullName() { return normalizedFullName; }
    public void setNormalizedFullName(String normalizedFullName) { this.normalizedFullName = normalizedFullName; }
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
    public String getPhotoStorageName() { return photoStorageName; }
    public void setPhotoStorageName(String photoStorageName) { this.photoStorageName = photoStorageName; }
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
    public Instant getCreatedAt() { return createdAt; }
    public AppUser getCreatedBy() { return createdBy; }
    public void setCreatedBy(AppUser createdBy) { this.createdBy = createdBy; }
}
