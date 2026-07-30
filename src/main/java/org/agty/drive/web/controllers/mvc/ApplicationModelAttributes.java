/*
 * Copyright 2026 Vladimir V
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
