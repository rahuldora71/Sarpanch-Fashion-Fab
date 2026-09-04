package com.sff.authapp.login.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "measurement_fields",
        indexes = {
                @Index(
                        name = "idx_measurement_field_name",
                        columnList = "name"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Field {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            nullable = false,
            length = 100,
            unique = true
    )
    private String name;
}