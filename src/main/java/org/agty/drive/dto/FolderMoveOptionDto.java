package org.agty.drive.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FolderMoveOptionDto {
    private Long id;
    private String displayTitle;
    private String pathKey;
}
