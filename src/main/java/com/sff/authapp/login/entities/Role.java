package com.sff.authapp.login.entities;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "roles",
        indexes = {
                @Index(
                        name = "idx_role_name",
                        columnList = "name",
                        unique = true
                )
        }
)
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long id;

    @Column(
            nullable = false,
            unique = true,
            length = 50
    )
    private String name;
}