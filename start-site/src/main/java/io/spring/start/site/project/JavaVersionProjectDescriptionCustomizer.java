/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.spring.start.site.project;

import java.util.Arrays;
import java.util.List;

import io.spring.initializr.generator.buildsystem.gradle.GradleBuildSystem;
import io.spring.initializr.generator.language.Language;
import io.spring.initializr.generator.project.MutableProjectDescription;
import io.spring.initializr.generator.project.ProjectDescriptionCustomizer;
import io.spring.initializr.generator.version.Version;
import io.spring.initializr.generator.version.VersionParser;
import io.spring.initializr.generator.version.VersionRange;

/**
 * Validate that the requested java version is compatible with the chosen Spring Boot
 * generation and adapt the request if necessary.
 *
 * @author Stephane Nicoll
 * @author Madhura Bhave
 */
public class JavaVersionProjectDescriptionCustomizer implements ProjectDescriptionCustomizer {

	private static final VersionRange SPRING_BOOT_2_0_OR_LATER = VersionParser.DEFAULT.parseRange("2.0.0.M1");

	private static final VersionRange SPRING_BOOT_2_0_1_OR_LATER = VersionParser.DEFAULT.parseRange("2.0.1.RELEASE");

	private static final VersionRange SPRING_BOOT_2_1_OR_LATER = VersionParser.DEFAULT.parseRange("2.1.0.M1");

	private static final VersionRange SPRING_BOOT_2_2_OR_LATER = VersionParser.DEFAULT.parseRange("2.2.0.M1");

	private static final List<String> UNSUPPORTED_LANGUAGES = Arrays.asList("groovy", "kotlin");

	@Override
	public void customize(MutableProjectDescription description) {
		Integer javaGeneration = determineJavaGeneration(description.getLanguage().jvmVersion());
		if (javaGeneration == null) {
			return;
		}
		Version platformVersion = description.getPlatformVersion();
		// Not supported for Spring Boot 1.x
		if (!SPRING_BOOT_2_0_OR_LATER.match(platformVersion)) {
			updateTo(description, "1.8");
		}
		// Not supported for Kotlin & Groovy
		if (UNSUPPORTED_LANGUAGES.contains(description.getLanguage().id())) {
			updateTo(description, "1.8");
		}
		// 10 support only as of 2.0.1
		if (javaGeneration == 10 && !SPRING_BOOT_2_0_1_OR_LATER.match(platformVersion)) {
			updateTo(description, "1.8");
		}
		// 11 and 12 support only as of 2.1.x
		if ((javaGeneration == 11 || javaGeneration == 12) && !SPRING_BOOT_2_1_OR_LATER.match(platformVersion)) {
			updateTo(description, "1.8");
		}
		// 13 support only as of 2.2.x - does not work with Gradle
		if (javaGeneration == 13 && (!SPRING_BOOT_2_2_OR_LATER.match(platformVersion)
				|| description.getBuildSystem() instanceof GradleBuildSystem)) {
			updateTo(description, "11");
		}
	}

	private void updateTo(MutableProjectDescription description, String jvmVersion) {
		Language language = Language.forId(description.getLanguage().id(), jvmVersion);
		description.setLanguage(language);
	}

	private Integer determineJavaGeneration(String javaVersion) {
		try {
			int generation = Integer.parseInt(javaVersion);
			return ((generation > 8 && generation <= 13) ? generation : null);
		}
		catch (NumberFormatException ex) {
			return null;
		}
	}

}
