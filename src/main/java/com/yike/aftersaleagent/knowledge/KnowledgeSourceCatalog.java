package com.yike.aftersaleagent.knowledge;

import java.util.List;
import java.util.Map;

final class KnowledgeSourceCatalog {
    private static final List<KnowledgeSource> SOURCES = List.of(
            source("knowledge/after-sale-rule.md", "AFTER_SALE_RULE", "AFTER_SALE", "NORMAL", "售后服务规则"),
            source("knowledge/coupon-rule.md", "COUPON_RULE", "COUPON", "NORMAL", "优惠券使用规则"),
            source("knowledge/product-rule.md", "PRODUCT_MANUAL", "PRODUCT", "NORMAL", "商品说明"),
            source("knowledge/faq.md", "FAQ", "FAQ", "NORMAL", "常见问题"),
            source("knowledge/refund-rule.md", "AFTER_SALE_RULE", "REFUND", "NORMAL", "退款资格说明"));

    private KnowledgeSourceCatalog() {
    }

    static List<KnowledgeSource> sources() {
        return SOURCES;
    }

    private static KnowledgeSource source(
            String sourcePath, String docType, String scene, String productType, String sourceTitle) {
        return new KnowledgeSource(sourcePath, Map.of(
                "docType", docType,
                "scene", scene,
                "productType", productType,
                "sourceTitle", sourceTitle,
                "sourcePath", sourcePath));
    }

    record KnowledgeSource(String sourcePath, Map<String, String> metadata) { }
}
