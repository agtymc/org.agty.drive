package org.agty.drive.dto;

import lombok.Data;

@Data
public class UsersStatusDictionaryDto {
    private Long id;
    private String code;
    private String title;
    private Integer align;
    private Boolean disabled;
}
