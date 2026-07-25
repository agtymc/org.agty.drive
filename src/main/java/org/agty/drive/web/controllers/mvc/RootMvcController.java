package org.agty.drive.web.controllers.mvc;

import org.agty.drive.security.service.DriveUserDetails;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RootMvcController {

    @GetMapping("/")
    public String root(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }

        if (authentication.getAuthorities().stream().anyMatch(it -> "ROLE_ADMIN".equals(it.getAuthority()))) {
            return "redirect:/cabinet";
        }

        return "redirect:/cabinet";
    }
}
