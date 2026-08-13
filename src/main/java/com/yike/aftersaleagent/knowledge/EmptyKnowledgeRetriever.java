package com.yike.aftersaleagent.knowledge;

import com.yike.aftersaleagent.agent.Intent;
import com.yike.aftersaleagent.chat.api.SourceCitation;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!local | !dashscope")
class EmptyKnowledgeRetriever implements KnowledgeRetriever, KnowledgeSceneRetriever {
    @Override
    public List<SourceCitation> retrieve(Intent intent, String productType, String question, int topK) {
        return List.of();
    }

    @Override
    public List<SourceCitation> retrieveTrustedScene(String trustedScene, String question, int topK) {
        return List.of();
    }
}
