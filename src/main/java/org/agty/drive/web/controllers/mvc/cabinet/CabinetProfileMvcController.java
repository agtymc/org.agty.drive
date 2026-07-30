package org.agty.drive.web.controllers.mvc.cabinet;

import org.agty.drive.security.service.DriveUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/cabinet/profile")
public class CabinetProfileMvcController {

    private final CabinetMvcSupport cabinetMvcSupport;

    public CabinetProfileMvcController(CabinetMvcSupport cabinetMvcSupport) {
        this.cabinetMvcSupport = cabinetMvcSupport;
    }

    @GetMapping
    public String index(@AuthenticationPrincipal DriveUserDetails userDetails, Model model) {
        cabinetMvcSupport.fillProfileModel(model, userDetails);
        return "cabinet/profile";
    }
}
