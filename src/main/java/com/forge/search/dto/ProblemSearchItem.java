package com.forge.search.dto;

import java.util.List;

public record ProblemSearchItem(String title, String titleSlug, String difficulty, List<String> tags) {
}
