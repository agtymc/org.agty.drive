package org.agty.drive.web.controllers.mvc.auth;

import jakarta.validation.Valid;
import org.agty.drive.dto.InviteAcceptDto;
import org.agty.drive.dto.OpenRegistrationDto;
import org.agty.drive.dto.UserDto;
import org.agty.drive.dto.UserInviteDto;
import org.agty.drive.services.AppSettingService;
import org.agty.drive.services.AuditLogService;
import org.agty.drive.services.UserInviteService;
import org.agty.drive.services.UserService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthMvcController {

    private final AppSettingService appSettingService;
    private final UserInviteService userInviteService;
    private final UserService userService;
    private final AuditLogService auditLogService;

    public AuthMvcController(AppSettingService appSettingService,
                             UserInviteService userInviteService,
                             UserService userService,
                             AuditLogService auditLogService) {
        this.appSettingService = appSettingService;
        this.userInviteService = userInviteService;
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    @ModelAttribute("inviteAcceptDto")
    public InviteAcceptDto inviteAcceptForm() {
        return new InviteAcceptDto();
    }

    @ModelAttribute("openRegistrationDto")
    public OpenRegistrationDto openRegistrationForm() {
        return new OpenRegistrationDto();
    }

    @GetMapping("/login")
    public String login(Authentication authentication, Model model) {
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/cabinet";
        }

        model.addAttribute("pageTitle", "Вход");
        model.addAttribute("openRegistrationEnabled", appSettingService.isOpenRegistrationEnabled());
        return "auth/login";
    }

    @GetMapping("/register")
    public String register(Authentication authentication, Model model) {
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/cabinet";
        }

        boolean openRegistrationEnabled = appSettingService.isOpenRegistrationEnabled();
        model.addAttribute("pageTitle", "Регистрация");
        model.addAttribute("openRegistrationEnabled", openRegistrationEnabled);
        if (!openRegistrationEnabled) {
            model.addAttribute("registrationError", "Открытая регистрация сейчас отключена.");
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String acceptOpenRegistration(@Valid @ModelAttribute("openRegistrationDto") OpenRegistrationDto openRegistrationDto,
                                         BindingResult bindingResult,
                                         Model model) {
        boolean openRegistrationEnabled = appSettingService.isOpenRegistrationEnabled();
        model.addAttribute("pageTitle", "Регистрация");
        model.addAttribute("openRegistrationEnabled", openRegistrationEnabled);

        if (!openRegistrationEnabled) {
            model.addAttribute("registrationError", "Открытая регистрация сейчас отключена.");
            return "auth/register";
        }
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        String error = userService.validateOpenRegistration(openRegistrationDto);
        if (error != null) {
            model.addAttribute("registrationError", error);
            return "auth/register";
        }

        UserDto user = userService.registerOpenUser(openRegistrationDto);
        if (user == null) {
            model.addAttribute("registrationError", "Не удалось завершить открытую регистрацию.");
            return "auth/register";
        }

        auditLogService.log(user.getId(), "OPEN_REGISTER", "USER", user.getId(),
                "Открытая регистрация пользователя " + user.getLogin());
        return "redirect:/login?registered";
    }

    @GetMapping("/invite/{token}")
    public String invite(@PathVariable("token") String token, Authentication authentication, Model model) {
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            SecurityContextHolder.clearContext();
        }

        UserInviteDto invite = userInviteService.findByToken(token);
        model.addAttribute("pageTitle", "Инвайт");
        model.addAttribute("invite", invite);
        model.addAttribute("inviteAvailable", userInviteService.isAvailable(invite));
        if (invite == null) {
            model.addAttribute("inviteError", "Инвайт не найден.");
        } else if (!userInviteService.isAvailable(invite)) {
            model.addAttribute("inviteError", "Инвайт недоступен или срок его действия истек.");
        }
        return "auth/invite";
    }

    @PostMapping("/invite/{token}")
    public String acceptInvite(@PathVariable("token") String token,
                               @Valid @ModelAttribute("inviteAcceptDto") InviteAcceptDto inviteAcceptDto,
                               BindingResult bindingResult,
                               Model model) {
        UserInviteDto invite = userInviteService.findByToken(token);
        model.addAttribute("pageTitle", "Инвайт");
        model.addAttribute("invite", invite);
        model.addAttribute("inviteAvailable", userInviteService.isAvailable(invite));

        if (bindingResult.hasErrors()) {
            return "auth/invite";
        }

        String error = userInviteService.validateAccept(invite, inviteAcceptDto);
        if (error != null) {
            model.addAttribute("inviteError", error);
            return "auth/invite";
        }

        UserDto user = userInviteService.accept(invite, inviteAcceptDto);
        if (user == null) {
            model.addAttribute("inviteError", "Не удалось завершить регистрацию по инвайту.");
            return "auth/invite";
        }

        auditLogService.log(invite.getCreatedBy(), "INVITE_ACCEPT", "USER", user.getId(),
                "Принят инвайт и создан пользователь " + user.getLogin());
        return "redirect:/login?invited";
    }
}
