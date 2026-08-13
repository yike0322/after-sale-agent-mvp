package com.yike.aftersaleagent.knowledge;

import java.util.List;

interface KnowledgeDocumentWriter {
    void deleteBySourcePath(String sourcePath);

    void add(List<KnowledgeDocument> documents);
}
