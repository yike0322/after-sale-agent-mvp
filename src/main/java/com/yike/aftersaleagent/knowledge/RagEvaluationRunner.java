package com.yike.aftersaleagent.knowledge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yike.aftersaleagent.chat.api.SourceCitation;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Runs only when aftersale.rag-evaluation.run=true is explicitly supplied in
 * the local,dashscope profile. It measures retrieved evidence, never model-answer quality.
 */
@Component
@Profile("local & dashscope")
@ConditionalOnProperty(prefix = "aftersale.rag-evaluation", name = "run", havingValue = "true")
public class RagEvaluationRunner implements ApplicationRunner {
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private final KnowledgeSceneRetriever knowledgeSceneRetriever;
    private final ObjectMapper objectMapper;

    public RagEvaluationRunner(KnowledgeSceneRetriever knowledgeSceneRetriever, ObjectMapper objectMapper) {
        this.knowledgeSceneRetriever = knowledgeSceneRetriever;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        List<EvaluationCase> cases = loadCases();
        List<EvaluationResult> results = cases.stream().map(this::evaluate).toList();
        Path report = writeReport(results);
        System.out.println("RAG evaluation report written to " + report.toAbsolutePath());
    }

    private List<EvaluationCase> loadCases() throws IOException {
        try (var input = new ClassPathResource("rag-evaluation/questions.json").getInputStream()) {
            return objectMapper.readValue(input, new TypeReference<List<EvaluationCase>>() { });
        }
    }

    private EvaluationResult evaluate(EvaluationCase evaluationCase) {
        List<SourceCitation> citations = knowledgeSceneRetriever.retrieveTrustedScene(
                evaluationCase.scene(), evaluationCase.question(), 3);
        boolean sourceHit = citations.stream()
                .anyMatch(citation -> evaluationCase.expectedSourcePath().equals(citation.sourcePath()));
        boolean evidenceHit = citations.stream().anyMatch(citation ->
                evaluationCase.expectedSourcePath().equals(citation.sourcePath())
                        && citation.excerpt().contains(evaluationCase.expectedEvidencePhrase()));
        return new EvaluationResult(evaluationCase, citations, sourceHit, evidenceHit);
    }

    private Path writeReport(List<EvaluationResult> results) throws IOException {
        int recallHits = (int) results.stream().filter(EvaluationResult::sourceHit).count();
        int citationCorrect = (int) results.stream().filter(EvaluationResult::evidenceHit).count();
        int caseCount = results.size();
        Path directory = Path.of("target", "rag-evaluation");
        Files.createDirectories(directory);
        Path output = directory.resolve("rag-evaluation-" + FILE_TIMESTAMP.format(LocalDateTime.now()) + ".md");
        StringBuilder report = new StringBuilder("# RAG 检索评估报告\n\n")
                .append("- 案例数: ").append(caseCount).append("\n")
                .append("- Recall@3: ").append(recallHits).append("/").append(caseCount).append("\n")
                .append("- 引用正确数: ").append(citationCorrect).append("\n\n")
                .append("| # | 问题 | 预期来源 | 预期证据短语 | 实际引用 | 来源命中 | 证据命中 | 人工答案相关性 |\n")
                .append("| --- | --- | --- | --- | --- | --- | --- | --- |\n");
        for (int index = 0; index < results.size(); index++) {
            EvaluationResult result = results.get(index);
            EvaluationCase evaluationCase = result.evaluationCase();
            report.append("| ").append(index + 1)
                    .append(" | ").append(markdownCell(evaluationCase.question()))
                    .append(" | ").append(markdownCell(evaluationCase.expectedSourcePath()))
                    .append(" | ").append(markdownCell(evaluationCase.expectedEvidencePhrase()))
                    .append(" | ").append(markdownCell(citations(result.citations())))
                    .append(" | ").append(result.sourceHit() ? "是" : "否")
                    .append(" | ").append(result.evidenceHit() ? "是" : "否")
                    .append(" |  |\n");
        }
        Files.writeString(output, report.toString(), StandardCharsets.UTF_8);
        return output;
    }

    private String citations(List<SourceCitation> citations) {
        return citations.stream()
                .map(citation -> citation.sourcePath() + "：" + citation.excerpt())
                .collect(Collectors.joining("<br>"));
    }

    private String markdownCell(String value) {
        return value.replace("|", "\\|").replace("\n", " ");
    }

    private record EvaluationCase(
            String question,
            String scene,
            String expectedSourcePath,
            String expectedEvidencePhrase) { }

    private record EvaluationResult(
            EvaluationCase evaluationCase,
            List<SourceCitation> citations,
            boolean sourceHit,
            boolean evidenceHit) { }
}
