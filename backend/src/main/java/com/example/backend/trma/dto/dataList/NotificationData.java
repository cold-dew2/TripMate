package com.example.backend.trma.dto.dataList;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public class NotificationData {
    private long notiId;
    private String typeCd;
    private String title;
    private String content;
    private String linkUrl;
    private String isRead;
    private String createDt;
}
