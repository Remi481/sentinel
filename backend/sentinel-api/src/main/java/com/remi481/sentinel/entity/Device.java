package com.remi481.sentinel.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "devices",
        indexes = {
                @Index(name = "idx_devices_token_hash", columnList = "token_hash", unique = true)
        })
@Getter
@Setter
@NoArgsConstructor
public class Device {

    @Id
    @NotNull
    private UUID uuid;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String hostname;

    @Size(max = 255)
    @Column(name = "operating_system")
    private String operatingSystem;

    @Size(max = 255)
    private String architecture;

    @Size(max = 255)
    @Column(name = "ip_address")
    private String ipAddress;

    @Size(max = 255)
    @Column(name = "agent_version")
    private String agentVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceStatus status;

    @Column(name = "last_seen")
    private Instant lastSeen;

    @Column(name = "token_hash", length = 64)
    @JsonIgnore
    private String tokenHash;
}
