package org.agty.drive.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeZoneConfig {

    @PostConstruct
    public void applyJvmTimeZone() {
        AppTime.applyJvmDefaultTimeZone();
    }
}
