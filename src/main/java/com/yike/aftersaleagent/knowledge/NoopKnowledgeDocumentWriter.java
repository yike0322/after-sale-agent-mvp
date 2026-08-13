package com.yike.aftersaleagent.knowledge;

import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!local | !dashscope")
class NoopKnowledgeDocumentWriter implements KnowledgeDocumentWriter {
    @Override
    public void deleteBySourcePath(String sourcePath) {
        // Default/mock/test profiles deliberately keep all vector operations offline.
    }

    @Override
    public void add(List<KnowledgeDocument> documents) {
        // Default/mock/test profiles deliberately keep all vector operations offline.
    }
}
