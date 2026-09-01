package com.example.backend.trma.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public class MoimSearchRequest {
    private String keyword;
    private String cateCd;
    private int page;
    private int offset;
}
