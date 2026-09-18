package com.example.backend.trma.mapper;

import com.example.backend.trma.dto.request.CreateSafetyReportRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SafetyReportMapper {
    //안심신고 등록
    int insertSafetyReport(@Param("request") CreateSafetyReportRequest request,
                           @Param("userId") String userId);
}
