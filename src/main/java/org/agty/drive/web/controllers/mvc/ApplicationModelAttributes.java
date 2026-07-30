package org.agty.drive.web.controllers.mvc;

import org.agty.drive.config.ApplicationInfo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = Controller.class)
public class ApplicationModelAttributes {

    private final ApplicationInfo applicationInfo;

    public ApplicationModelAttributes(ApplicationInfo applicationInfo) {
        this.applicationInfo = applicationInfo;
    }

    @ModelAttribute("applicationInfo")
    public ApplicationInfo applicationInfo() {
        return applicationInfo;
    }

    @ModelAttribute("applicationTitle")
    public String applicationTitle() {
        return applicationInfo.getTitle();
    }

    @ModelAttribute("applicationAbout")
    public String applicationAbout() {
        return applicationInfo.getAbout();
    }
}
