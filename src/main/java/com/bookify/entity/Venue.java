package com.bookify.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "venues")
public class Venue {

    @Id
    @GeneratedValue(strategy= GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 50)
    private String city;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime created_at;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updated_at;

}
