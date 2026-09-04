package com.sff.authapp.login.entities;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeasurementField {

    private Long fieldId;

    private String unit;

    @Builder.Default
    private boolean required = true;

    private String type;

    private Integer displayOrder;
}