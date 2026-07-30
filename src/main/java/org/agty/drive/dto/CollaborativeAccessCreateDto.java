package org.agty.drive.dto;

import lombok.Data;

@Data
public class CollaborativeAccessCreateDto {
    private Long folderId;
    private String logins;
    private String password;
    private Boolean allowWrite;
    private Boolean allowDelete;
}
