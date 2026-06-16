package com.wisecan.unified.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "company")
@Getter
@NoArgsConstructor
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, unique = true, length = 20)
    private String bizNumber;

    @Column(nullable = false, length = 20)
    private String billingMode;

    @Column(nullable = false, length = 20)
    private String status;

    @Column
    private LocalDateTime approvedAt;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Builder
    public Company(String name, String bizNumber, String billingMode, String status, LocalDateTime approvedAt) {
        this.name = name;
        this.bizNumber = bizNumber;
        this.billingMode = billingMode;
        this.status = status;
        this.approvedAt = approvedAt;
    }

    public void suspend() {
        this.status = "SUSPENDED";
    }
}
