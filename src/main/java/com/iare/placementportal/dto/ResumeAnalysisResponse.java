package com.iare.placementportal.dto;

import java.util.List;

public record ResumeAnalysisResponse(
        int overallScore,
        String shortSummary,
        List<String> strengths,
        List<String> mistakes,
        List<String> missingSkills,
        List<String> atsSuggestions,
        List<String> projectImprovements,
        List<String> grammarFormattingIssues,
        String finalAdvice
) {
}
