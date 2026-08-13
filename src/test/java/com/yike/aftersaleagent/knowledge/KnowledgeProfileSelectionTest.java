package com.yike.aftersaleagent.knowledge;

import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class KnowledgeProfileSelectionTest {
    @Autowired
    KnowledgeRetriever knowledgeRetriever;

    @Autowired
    KnowledgeDocumentWriter knowledgeDocumentWriter;

    @Autowired
    ApplicationContext applicationContext;

    @Test
    void testProfileUsesOfflineKnowledgeBoundariesWithoutAVectorStore() {
        assertThat(knowledgeRetriever).isInstanceOf(EmptyKnowledgeRetriever.class);
        assertThat(knowledgeDocumentWriter).isInstanceOf(NoopKnowledgeDocumentWriter.class);
        assertThat(applicationContext.getBeansOfType(VectorStore.class)).isEmpty();
    }
}
