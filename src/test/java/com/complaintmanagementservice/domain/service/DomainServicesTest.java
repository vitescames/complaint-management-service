package com.complaintmanagementservice.domain.service;

import com.complaintmanagementservice.TestFixtures;
import com.complaintmanagementservice.domain.model.Category;
import com.complaintmanagementservice.domain.model.CategoryKeyword;
import com.complaintmanagementservice.domain.model.Complaint;
import com.complaintmanagementservice.domain.model.ComplaintStatus;
import com.complaintmanagementservice.domain.model.ComplaintText;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DomainServicesTest {

    private final ComplaintCategoryClassifier complaintCategoryClassifier = new ComplaintCategoryClassifier();
    private final ComplaintSlaPolicy complaintSlaPolicy = new ComplaintSlaPolicy();

    @Test
    void shouldClassifyComplaintUsingNormalizedKeywords() {
        Category fraude = Category.builder()
                .id(6L)
                .name("fraude")
                .keywords(Set.of(
                        new CategoryKeyword(18L, "nao reconhece divida"),
                        new CategoryKeyword(19L, "fraude")
                ))
                .build();

        Set<Category> categories = complaintCategoryClassifier.classify(
                new ComplaintText("Nao reconheco divida na fatura e suspeito de fraude no aplicativo"),
                List.of(TestFixtures.cobrancaCategory(), fraude, TestFixtures.acessoCategory())
        );

        assertThat(categories).extracting(Category::name).containsExactlyInAnyOrder("cobranca", "fraude");
    }

    @Test
    void shouldReturnEmptyClassificationWhenNoKeywordMatches() {
        Set<Category> categories = complaintCategoryClassifier.classify(
                new ComplaintText("Assunto sem palavras relevantes"),
                List.of(TestFixtures.acessoCategory())
        );

        assertThat(categories).isEmpty();
    }

    @Test
    void shouldApplySlaRule() {
        Complaint complaint = TestFixtures.approachingSlaComplaint();

        assertThat(complaintSlaPolicy.deadlineFor(complaint)).isEqualTo(LocalDate.of(2026, 3, 26));
        assertThat(complaintSlaPolicy.warningTriggerComplaintDate(LocalDate.of(2026, 3, 23)))
                .isEqualTo(LocalDate.of(2026, 3, 16));
        assertThat(complaintSlaPolicy.isWarningDue(complaint, LocalDate.of(2026, 3, 23))).isTrue();

        Complaint resolvedComplaint = Complaint.builder()
                .id(complaint.id())
                .customer(complaint.customer())
                .complaintDate(complaint.complaintDate())
                .complaintText(complaint.complaintText())
                .documentUrls(complaint.documentUrls())
                .status(ComplaintStatus.RESOLVED)
                .categories(complaint.categories())
                .registeredAt(complaint.registeredAt())
                .buildReconstituted();

        assertThat(complaintSlaPolicy.isWarningDue(resolvedComplaint, LocalDate.of(2026, 3, 23))).isFalse();
        assertThat(complaintSlaPolicy.isWarningDue(complaint, LocalDate.of(2026, 3, 22))).isFalse();
    }
}
