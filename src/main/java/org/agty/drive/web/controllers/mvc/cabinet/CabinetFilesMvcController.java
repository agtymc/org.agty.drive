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

package org.agty.drive.web.controllers.mvc.cabinet;

import org.agty.drive.security.service.DriveUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/cabinet")
public class CabinetFilesMvcController {

    private final CabinetMvcSupport cabinetMvcSupport;

    public CabinetFilesMvcController(CabinetMvcSupport cabinetMvcSupport) {
        this.cabinetMvcSupport = cabinetMvcSupport;
    }

    @GetMapping
    public String index(@RequestParam(name = "folderId", required = false) Long folderId,
                        @RequestParam(name = "view", required = false) String viewMode,
                        @RequestParam(name = "sort", required = false) String sortMode,
                        @RequestParam(name = "q", required = false) String searchQuery,
                        @RequestParam(name = "scope", required = false) String searchScope,
                        @RequestParam(name = "page", required = false) Integer page,
                        @RequestParam(name = "size", required = false) Integer pageSize,
                        @AuthenticationPrincipal DriveUserDetails userDetails,
                        Model model) {
        cabinetMvcSupport.fillCabinetModel(model, userDetails, "files", folderId, viewMode, sortMode, searchQuery, searchScope, page, pageSize);
        return "cabinet/index";
    }
}
