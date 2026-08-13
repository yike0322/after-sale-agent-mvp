package com.yike.aftersaleagent.knowledge;

import com.yike.aftersaleagent.chat.api.SourceCitation;
import java.util.List;

/** Internal, source-controlled evaluator boundary; it never accepts a user or model supplied scene. */
interface KnowledgeSceneRetriever {
    List<SourceCitation> retrieveTrustedScene(String trustedScene, String question, int topK);
}
