package org.agty.drive.dto;

import lombok.Data;

@Data
public class ProfileSecuritySettingsDto {
    private String email;
    private Boolean twoFactorEmailEnabled;
    private Boolean twoFactorTotpEnabled;
    private String twoFactorTotpSecret;
    private String twoFactorTotpUri;
}
