package com.yike.aftersaleagent.knowledge;

import com.yike.aftersaleagent.agent.Intent;
import com.yike.aftersaleagent.chat.api.SourceCitation;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local & dashscope")
class QdrantKnowledgeRetriever implements KnowledgeRetriever, KnowledgeSceneRetriever {
    private static final int MAX_EXCERPT_LENGTH = 280;
    private static final Set<String> EVALUATION_SCENES = Set.of(
            "AFTER_SALE", "COUPON", "PRODUCT", "FAQ", "REFUND");

    private final VectorStore vectorStore;

    QdrantKnowledgeRetriever(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public List<SourceCitation> retrieve(Intent intent, String productType, String question, int topK) {
        String trustedScene = trustedSceneFor(intent);
        if (trustedScene == null) {
            return List.of();
        }
        return retrieveTrustedScene(trustedScene, question, topK);
    }

    @Override
    public List<SourceCitation> retrieveTrustedScene(String trustedScene, String question, int topK) {
        if (!EVALUATION_SCENES.contains(trustedScene)) {
            return List.of();
        }
        List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder()
                .query(question == null ? "" : question)
                .topK(topK)
                .filterExpression("scene == '" + trustedScene + "'")
                .build());
        return documents.stream()
                .map(this::citationFrom)
                .flatMap(java.util.Optional::stream)
                .toList();
    }

    private String trustedSceneFor(Intent intent) {
        return switch (intent) {
            case FAQ_QUERY -> "AFTER_SALE";
            case COUPON_ANALYSIS -> "COUPON";
            case REFUND_ELIGIBILITY -> "REFUND";
            case UNSUPPORTED -> null;
        };
    }

    private java.util.Optional<SourceCitation> citationFrom(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        Object sourceTitle = metadata.get("sourceTitle");
        Object sourcePath = metadata.get("sourcePath");
        String text = document.getText();
        if (sourceTitle == null || sourcePath == null || text == null || text.isBlank()) {
            return java.util.Optional.empty();
        }
        String excerpt = text.strip();
        if (excerpt.length() > MAX_EXCERPT_LENGTH) {
            excerpt = excerpt.substring(0, MAX_EXCERPT_LENGTH) + "…";
        }
        return java.util.Optional.of(new SourceCitation(
                String.valueOf(sourceTitle), String.valueOf(sourcePath), excerpt));
    }
}
