package com.yike.aftersaleagent.knowledge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local & dashscope")
class QdrantKnowledgeDocumentWriter implements KnowledgeDocumentWriter {
    private final VectorStore vectorStore;

    QdrantKnowledgeDocumentWriter(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void deleteBySourcePath(String sourcePath) {
        vectorStore.delete("sourcePath == '" + sourcePath + "'");
    }

    @Override
    public void add(List<KnowledgeDocument> documents) {
        List<Document> vectorDocuments = new ArrayList<>(documents.size());
        for (KnowledgeDocument document : documents) {
            Map<String, Object> metadata = new LinkedHashMap<>(document.metadata());
            vectorDocuments.add(new Document(document.text(), metadata));
        }
        vectorStore.add(vectorDocuments);
    }
}
