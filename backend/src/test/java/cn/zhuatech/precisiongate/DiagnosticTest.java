package cn.zhuatech.precisiongate;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.*;
import tools.jackson.databind.ObjectMapper;

/**
 * Synthetic, repeatable numeric controls and keyed result comparisons. 上海如静知华信息科技有限公司 ·
 * https://www.zhuatech.cn/ · 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
 */
@SuppressWarnings("unchecked")
class DiagnosticTest {
  ObjectMapper json = new ObjectMapper();
  Map<String, Object> cfg;
  List<Map<String, Object>> rows;

  /** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
  @BeforeEach
  void setup() {
    var cat = json.readValue(getClass().getResourceAsStream("/static/catalog.json"), Map.class);
    cfg = (Map<String, Object>) cat.get("settings");
    rows = (List<Map<String, Object>>) cat.get("rows");
  }

  /** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
  static Map<String, Object> row(String id, Object... fields) {
    var m = new LinkedHashMap<String, Object>();
    m.put("id", id);
    for (int i = 0; i < fields.length; i += 2) m.put((String) fields[i], fields[i + 1]);
    return m;
  }

  /** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
  static List<Map<String, Object>> mutable(List<Map<String, Object>> rows) {
    var copy = new ArrayList<Map<String, Object>>();
    rows.forEach(r -> copy.add(new LinkedHashMap<>(r)));
    return copy;
  }

  /** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
  static Map<String, Object> diag(Map<String, Object> settings, List<Map<String, Object>> rows) {
    return (Map<String, Object>) Algorithm.compute(settings, rows).get("diagnostics");
  }

  /** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
  static Map<String, Object> metrics(Map<String, Object> d) {
    return (Map<String, Object>) d.get("metrics");
  }

  /** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
  static List<Map<String, Object>> values(Map<String, Object> d) {
    return (List<Map<String, Object>>) d.get("rows");
  }

  /** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
  static void eq(Map<String, Object> m, String key, double want) {
    assertEquals(want, ((Number) m.get(key)).doubleValue(), 1e-10, key);
  }

  /** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
  @Test
  void goldenAcceptanceForRealWorkedExample() {
    var d = diag(cfg, rows);
    var m = metrics(d);
    var lines = values(d);
    eq(m, "brier", 0.17);
    eq(m, "ece", 1.0 / 30);
    eq(m, "occupiedBins", 2);
    eq(lines.get(5), "observedRate", 0.5);
    var product = Algorithm.compute(cfg, rows);
    assertEquals(m.get("brier"), GateRules.value(product, "diagnostics.brier"));
  }

  /** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
  @Test
  void edgeCasesHaveExplicitBusinessMeaning() {
    var input =
        List.of(
            row("0", "score", 0, "truth", 0),
            row("1", "score", 1, "truth", 1),
            row("3", "score", 0.3, "truth", 1));
    var d = diag(cfg, input);
    var bins = values(d);
    eq(bins.get(9), "support", 1);
    eq(bins.get(3), "support", 1);
    assertNull(bins.get(2).get("absoluteGap"));
    eq(metrics(d), "brier", 0.49 / 3);
  }

  /** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
  @Test
  void evidenceUsesActualIdentityFiltersAndPaginatesAfterMatching() {
    var ref = new LinkedHashMap<String, Object>();
    var next = new LinkedHashMap<String, Object>();
    for (String k : EvidenceDiff.MAIN) {
      ref.put(k, "a*");
      next.put(k, "b");
    }
    ref.put("measure", 1);
    next.put("measure", 4);
    var revised = new LinkedHashMap<>(ref);
    revised.put("measure", 2);
    var from = Map.<String, Object>of("rows", List.of(ref, next));
    var extra = new LinkedHashMap<>(next);
    extra.put(EvidenceDiff.MAIN.getFirst(), "c");
    var to = Map.<String, Object>of("rows", List.of(revised, extra));
    var response = EvidenceDiff.compare(from, to, "main", "DIFFERENCES", "", 2, 1);
    assertEquals(3, response.get("total"));
    assertEquals(Map.of("ADDED", 1, "REMOVED", 1, "CHANGED", 1, "SAME", 0), response.get("counts"));
    var found = EvidenceDiff.compare(from, to, "main", "CHANGED", "A*", 1, 20);
    var item = ((List<Map<String, Object>>) found.get("items")).getFirst();
    assertEquals(List.of("measure"), item.get("fields"));
    assertEquals(ref, item.get("baseline"));
    assertEquals(
        "MISSING_SECTION",
        EvidenceDiff.compare(from, to, "inspection", "ALL", "", 1, 10).get("availability"));
    assertEquals(
        "AMBIGUOUS_IDENTITY",
        EvidenceDiff.compare(Map.of("rows", List.of(ref, ref)), to, "main", "ALL", "", 1, 10)
            .get("availability"));
    assertThrows(
        Model.Failure.class, () -> EvidenceDiff.compare(from, to, "main", "ALL", "", 0, 10));
  }

  /** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
  @Test
  void optionalStorageLimitIsExplicitAndPartialEvidenceCannotAppearAsRemovals() {
    var base = new LinkedHashMap<String, Object>();
    base.put("metrics", Map.of());
    base.put("rows", List.of());
    var big = Map.<String, Object>of("bin", "a", "value", "z".repeat(350005));
    base.put("diagnostics", Map.of("metrics", Map.of("summary", 7), "rows", List.of(big)));
    var persisted =
        (Map<String, Object>) json.readValue(ResultStorage.encode(base, json), Map.class);
    var part = (Map<?, ?>) persisted.get("diagnostics");
    assertEquals(true, part.get("truncated"));
    assertEquals(1, part.get("totalRows"));
    assertEquals(List.of(), part.get("rows"));
    assertEquals(Map.of("summary", 7), part.get("metrics"));
    assertEquals(
        "PARTIAL_SECTION",
        EvidenceDiff.compare(persisted, base, "diagnostics", "ALL", "", 1, 10).get("availability"));
    assertEquals(1, ((List<?>) ((Map<?, ?>) base.get("diagnostics")).get("rows")).size());
  }
}
