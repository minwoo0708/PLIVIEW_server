package com.musicreview.server;

import com.musicreview.server.global.common.TimeUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.util.TimeZone;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MusicReviewServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(MusicReviewServerApplication.class, args);
	}

	/** createdAt 등 LocalDateTime이 항상 한국 시간으로 기록되도록 고정 */
	@PostConstruct
	void setDefaultTimeZone() {
		TimeZone.setDefault(TimeZone.getTimeZone(TimeUtils.ZONE));
	}

}
