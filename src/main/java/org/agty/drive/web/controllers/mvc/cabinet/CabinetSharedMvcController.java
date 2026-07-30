package org.agty.drive.web.controllers.mvc.cabinet;

import org.agty.drive.security.service.DriveUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/cabinet/shared")
public class CabinetSharedMvcController {

    private final CabinetMvcSupport cabinetMvcSupport;

    public CabinetSharedMvcController(CabinetMvcSupport cabinetMvcSupport) {
        this.cabinetMvcSupport = cabinetMvcSupport;
    }

    @GetMapping
    public String index(@RequestParam(name = "view", required = false) String viewMode,
                        @RequestParam(name = "sort", required = false) String sortMode,
                        @RequestParam(name = "q", required = false) String searchQuery,
                        @RequestParam(name = "page", required = false) Integer page,
                        @RequestParam(name = "size", required = false) Integer pageSize,
                        @AuthenticationPrincipal DriveUserDetails userDetails,
                        Model model) {
        cabinetMvcSupport.fillCabinetModel(model, userDetails, "shared", null, viewMode, sortMode, searchQuery, null, page, pageSize);
        return "cabinet/index";
    }
}
