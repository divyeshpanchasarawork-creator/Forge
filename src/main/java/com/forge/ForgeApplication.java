package com.forge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;

@SpringBootApplication
public class ForgeApplication {

	public static void main(String[] args) {
		assertProdProfileWhenDatabaseEnvPresent(args);
		SpringApplication.run(ForgeApplication.class, args);
	}

	/**
	 * Fails fast if database env vars are present (Render) but the prod profile is not active.
	 * Prevents accidentally booting prod on the in-memory dev database with the committed dev JWT secret.
	 */
	private static void assertProdProfileWhenDatabaseEnvPresent(String[] args) {
		boolean databaseEnvPresent = System.getenv("DB_HOST") != null || System.getenv("DATABASE_USERNAME") != null;
		if (!databaseEnvPresent) {
			return;
		}

		String envProfiles = System.getenv("SPRING_PROFILES_ACTIVE");
		String argProfiles = Arrays.stream(args)
				.filter(arg -> arg.startsWith("--spring.profiles.active="))
				.map(arg -> arg.substring("--spring.profiles.active=".length()))
				.findFirst()
				.orElse(null);

		String activeProfiles = (argProfiles != null && !argProfiles.isBlank()) ? argProfiles : envProfiles;
		if (activeProfiles == null || Arrays.stream(activeProfiles.split(","))
				.map(String::trim)
				.noneMatch(profile -> profile.equalsIgnoreCase("prod"))) {
			throw new IllegalStateException(
					"Database environment variables are set but the 'prod' Spring profile is not active. "
							+ "Set SPRING_PROFILES_ACTIVE=prod to avoid booting against the in-memory dev database.");
		}
	}

}
