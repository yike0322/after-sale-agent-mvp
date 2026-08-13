package com.yike.aftersaleagent.knowledge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeIndexerTest {

    @Test
    void indexesFiveTrustedSourcesWithExactlyTheRequiredMetadataKeys() {
        RecordingWriter writer = new RecordingWriter();
        KnowledgeIndexer indexer = new KnowledgeIndexer(writer, new DefaultResourceLoader());

        KnowledgeIndexResult result = indexer.reindex();

        assertThat(result.sourceCount()).isEqualTo(5);
        assertThat(result.chunkCount()).isPositive();
        assertThat(writer.documentsBySource).hasSize(5);
        assertThat(writer.documentsBySource.values()).allSatisfy(documents -> assertThat(documents).isNotEmpty());
        assertThat(writer.documentsBySource.values().stream().flatMap(List::stream))
                .allSatisfy(document -> assertThat(document.metadata().keySet())
                        .containsExactlyInAnyOrder(
                                "docType", "scene", "productType", "sourceTitle", "sourcePath"));
        assertThat(writer.documentsBySource.get("knowledge/after-sale-rule.md"))
                .allSatisfy(document -> assertThat(document.metadata()).isEqualTo(Map.of(
                        "docType", "AFTER_SALE_RULE",
                        "scene", "AFTER_SALE",
                        "productType", "NORMAL",
                        "sourceTitle", "售后服务规则",
                        "sourcePath", "knowledge/after-sale-rule.md")));
    }

    @Test
    void chunksAtFiveHundredJavaCharacterBoundaryWithEightyCharacterOverlap() {
        KnowledgeIndexer indexer = new KnowledgeIndexer(new RecordingWriter(), new DefaultResourceLoader());
        String content = sequence(501);

        List<String> atBoundary = indexer.chunk(content.substring(0, 500));
        List<String> overBoundary = indexer.chunk(content);

        assertThat(atBoundary).containsExactly(content.substring(0, 500));
        assertThat(overBoundary).hasSize(2);
        assertThat(overBoundary.get(0)).hasSize(500);
        assertThat(overBoundary.get(1)).isEqualTo(content.substring(420));
        assertThat(overBoundary.get(0).substring(420))
                .isEqualTo(overBoundary.get(1).substring(0, 80));
        assertThat(overBoundary).allSatisfy(chunk -> assertThat(chunk).isNotEmpty());
    }

    @Test
    void deletesEachSourceBeforeAddingReplacementChunksOnRepeatedReindex() {
        RecordingWriter writer = new RecordingWriter();
        KnowledgeIndexer indexer = new KnowledgeIndexer(writer, new DefaultResourceLoader());

        indexer.reindex();
        int firstRunChunks = writer.totalDocuments();
        indexer.reindex();

        assertThat(writer.totalDocuments()).isEqualTo(firstRunChunks);
        assertThat(writer.operations).hasSize(20);
        for (int index = 0; index < writer.operations.size(); index += 2) {
            assertThat(writer.operations.get(index)).startsWith("delete:");
            assertThat(writer.operations.get(index + 1)).isEqualTo(
                    "add:" + writer.operations.get(index).substring("delete:".length()));
        }
    }

    private String sequence(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            builder.append((char) ('a' + index % 26));
        }
        return builder.toString();
    }

    private static final class RecordingWriter implements KnowledgeDocumentWriter {
        private final Map<String, List<KnowledgeDocument>> documentsBySource = new LinkedHashMap<>();
        private final List<String> operations = new ArrayList<>();

        @Override
        public void deleteBySourcePath(String sourcePath) {
            operations.add("delete:" + sourcePath);
            documentsBySource.remove(sourcePath);
        }

        @Override
        public void add(List<KnowledgeDocument> documents) {
            String sourcePath = documents.getFirst().metadata().get("sourcePath");
            operations.add("add:" + sourcePath);
            documentsBySource.computeIfAbsent(sourcePath, ignored -> new ArrayList<>()).addAll(documents);
        }

        private int totalDocuments() {
            return documentsBySource.values().stream().mapToInt(List::size).sum();
        }
    }
}
