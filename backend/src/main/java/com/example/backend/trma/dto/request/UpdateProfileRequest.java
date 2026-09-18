package com.example.backend.trma.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@NoArgsConstructor
@Setter
public class UpdateProfileRequest {
    private String userNm;
    private String areaNm;
    private String description;
    private String profileImageUrl;
    private List<String> langCds;
}
