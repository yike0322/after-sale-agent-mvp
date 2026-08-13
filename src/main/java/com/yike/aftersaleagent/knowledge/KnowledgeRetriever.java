package com.yike.aftersaleagent.knowledge;

import com.yike.aftersaleagent.agent.Intent;
import com.yike.aftersaleagent.chat.api.SourceCitation;
import java.util.List;

public interface KnowledgeRetriever {
    List<SourceCitation> retrieve(Intent intent, String productType, String question, int topK);
}
