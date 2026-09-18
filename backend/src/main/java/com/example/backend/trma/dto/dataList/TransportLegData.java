package com.example.backend.trma.dto.dataList;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public class TransportLegData {
    private int day;
    private String fromTourId;
    private String fromTourNm;
    private String toTourId;
    private String toTourNm;
    private String mode;
    private Integer durationMinutes;
    private Integer cost;
    private Integer transferCount;
}
