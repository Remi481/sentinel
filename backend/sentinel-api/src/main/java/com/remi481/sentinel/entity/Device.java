package com.remi481.sentinel.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "devices")
@Getter
@Setter
@NoArgsConstructor
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID uuid;

    @Column(nullable = false)
    private String hostname;

    @Column(name = "operating_system")
    private String operatingSystem;

    private String architecture;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "agent_version")
    private String agentVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceStatus status;

    @Column(name = "last_seen")
    private Instant lastSeen;
}