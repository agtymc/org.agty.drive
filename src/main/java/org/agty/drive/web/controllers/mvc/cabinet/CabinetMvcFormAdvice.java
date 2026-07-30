package org.agty.drive.web.controllers.mvc.cabinet;

import org.agty.drive.dto.ChangePasswordDto;
import org.agty.drive.dto.CabinetPageStateDto;
import org.agty.drive.dto.CollaborativeAccessCreateDto;
import org.agty.drive.dto.FileUploadDto;
import org.agty.drive.dto.FolderDto;
import org.agty.drive.dto.ItemMoveDto;
import org.agty.drive.dto.ItemPropertiesDto;
import org.agty.drive.dto.ItemRenameDto;
import org.agty.drive.dto.ProfileSecuritySettingsDto;
import org.agty.drive.dto.ShareLinkCreateDto;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(basePackageClasses = CabinetMvcController.class)
public class CabinetMvcFormAdvice {

    @ModelAttribute("cabinetState")
    public CabinetPageStateDto cabinetStateForm() {
        return new CabinetPageStateDto();
    }

    @ModelAttribute("folderDto")
    public FolderDto folderForm() {
        return new FolderDto();
    }

    @ModelAttribute("fileUploadDto")
    public FileUploadDto fileUploadForm() {
        return new FileUploadDto();
    }

    @ModelAttribute("changePasswordDto")
    public ChangePasswordDto changePasswordForm() {
        return new ChangePasswordDto();
    }

    @ModelAttribute("itemRenameDto")
    public ItemRenameDto itemRenameForm() {
        return new ItemRenameDto();
    }

    @ModelAttribute("itemMoveDto")
    public ItemMoveDto itemMoveForm() {
        return new ItemMoveDto();
    }

    @ModelAttribute("itemPropertiesDto")
    public ItemPropertiesDto itemPropertiesForm() {
        ItemPropertiesDto dto = new ItemPropertiesDto();
        dto.setResourceType("FILE");
        dto.setAutoDelete(false);
        return dto;
    }

    @ModelAttribute("profileSecuritySettingsDto")
    public ProfileSecuritySettingsDto profileSecuritySettingsForm() {
        return new ProfileSecuritySettingsDto();
    }

    @ModelAttribute("shareLinkCreateDto")
    public ShareLinkCreateDto shareLinkForm() {
        ShareLinkCreateDto dto = new ShareLinkCreateDto();
        dto.setResourceType("FILE");
        dto.setAllowDownload(true);
        dto.setAllowPreview(true);
        dto.setExpiresInHours(24);
        dto.setExpiresUnlimited(false);
        return dto;
    }

    @ModelAttribute("collaborativeAccessCreateDto")
    public CollaborativeAccessCreateDto collaborativeAccessForm() {
        CollaborativeAccessCreateDto dto = new CollaborativeAccessCreateDto();
        dto.setAllowWrite(false);
        dto.setAllowDelete(false);
        return dto;
    }
}
