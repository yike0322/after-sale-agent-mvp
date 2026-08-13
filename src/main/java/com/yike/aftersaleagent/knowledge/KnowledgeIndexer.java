package com.yike.aftersaleagent.knowledge;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeIndexer {
    static final int CHUNK_SIZE = 500;
    static final int CHUNK_OVERLAP = 80;

    private final KnowledgeDocumentWriter writer;
    private final ResourceLoader resourceLoader;

    public KnowledgeIndexer(KnowledgeDocumentWriter writer, ResourceLoader resourceLoader) {
        this.writer = writer;
        this.resourceLoader = resourceLoader;
    }

    public KnowledgeIndexResult reindex() {
        int sourceCount = 0;
        int chunkCount = 0;
        for (KnowledgeSourceCatalog.KnowledgeSource source : KnowledgeSourceCatalog.sources()) {
            List<KnowledgeDocument> documents = chunksFor(source);
            writer.deleteBySourcePath(source.sourcePath());
            writer.add(documents);
            sourceCount++;
            chunkCount += documents.size();
        }
        return new KnowledgeIndexResult(sourceCount, chunkCount);
    }

    List<String> chunk(String content) {
        String safeContent = content == null ? "" : content;
        if (safeContent.isEmpty()) {
            return List.of();
        }
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < safeContent.length()) {
            int end = Math.min(start + CHUNK_SIZE, safeContent.length());
            String chunk = safeContent.substring(start, end);
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            if (end == safeContent.length()) {
                break;
            }
            start = end - CHUNK_OVERLAP;
        }
        return List.copyOf(chunks);
    }

    private List<KnowledgeDocument> chunksFor(KnowledgeSourceCatalog.KnowledgeSource source) {
        String content = normalize(read(source.sourcePath()));
        List<KnowledgeDocument> documents = new ArrayList<>();
        for (String chunk : chunk(content)) {
            documents.add(new KnowledgeDocument(chunk, source.metadata()));
        }
        if (documents.isEmpty()) {
            throw new IllegalStateException("Knowledge source is empty: " + source.sourcePath());
        }
        return List.copyOf(documents);
    }

    private String read(String sourcePath) {
        Resource resource = resourceLoader.getResource("classpath:" + sourcePath);
        try (var inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read knowledge source: " + sourcePath, exception);
        }
    }

    private String normalize(String markdown) {
        return markdown.lines()
                .map(line -> line.replaceFirst("^#{1,6}\\s*", "").strip())
                .filter(line -> !line.isEmpty())
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
    }
}
