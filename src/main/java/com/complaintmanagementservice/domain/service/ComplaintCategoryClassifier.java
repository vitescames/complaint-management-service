package com.complaintmanagementservice.domain.service;

import com.complaintmanagementservice.domain.model.Category;
import com.complaintmanagementservice.domain.model.ComplaintText;

import java.text.Normalizer;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public class ComplaintCategoryClassifier {

    public Set<Category> classify(ComplaintText complaintText, Collection<Category> categories) {
        String normalizedText = normalize(complaintText.value());
        Set<Category> matchedCategories = new LinkedHashSet<>();
        for (Category category : categories) {
            if (category.keywords().stream().anyMatch(keyword -> keyword.matches(normalizedText))) {
                matchedCategories.add(category);
            }
        }
        return Set.copyOf(matchedCategories);
    }

    private String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
