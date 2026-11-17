package cn.zhuatech.precisiongate;

import static cn.zhuatech.precisiongate.Checks.*;

import java.util.*;

/**
 * 扫描验证集所有不同分数，在满足精确率底线的候选中选择召回最高的阈值，输出每个阈值的混淆计数。 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ ·
 * 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
 */
final class Algorithm {
  /**
   * 同分样本整体纳入；召回相同保留较高阈值。没有可行阈值时不建议上线。 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ ·
   * 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
   */
  static Map<String, Object> compute(Map<String, Object> cfg, List<Map<String, Object>> rows) {
    var base = calculate(cfg, rows);
    var enriched = new LinkedHashMap<String, Object>(base);
    enriched.put("inspection", Inspection.evaluate(cfg, rows, base));
    enriched.put("diagnostics", Diagnostics.evaluate(cfg, rows, base));
    return enriched;
  }

  /**
   * 保留原始计算口径。上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
   */
  private static Map<String, Object> calculate(
      Map<String, Object> cfg, List<Map<String, Object>> rows) {
    must(rows != null && !rows.isEmpty() && rows.size() <= 200, "需要 1–200 行");
    unique(rows);
    var evidence = new ArrayList<Map<String, Object>>();
    double min = num(cfg, "minPrecision", 0, 1);
    var groups = new TreeMap<Double, int[]>(Comparator.reverseOrder());
    int positives = 0;
    for (var r : rows) {
      double s = num(r, "score", 0, 1);
      int y = integer(r, "truth", 0, 1);
      groups.computeIfAbsent(s == 0 ? 0 : s, k -> new int[2])[y]++;
      positives += y;
    }
    must(positives > 0, "缺少正例，无法评估召回");
    int tp = 0, fp = 0, best = -1;
    Double cut = null;
    for (var g : groups.entrySet()) {
      fp += g.getValue()[0];
      tp += g.getValue()[1];
      double p = (double) tp / (tp + fp);
      if (p >= min && tp > best) {
        best = tp;
        cut = g.getKey();
      }
      evidence.add(
          Map.of(
              "threshold",
              g.getKey(),
              "tp",
              tp,
              "fp",
              fp,
              "fn",
              positives - tp,
              "tn",
              rows.size() - positives - fp,
              "precision",
              p,
              "recall",
              (double) tp / positives));
    }
    var m = new LinkedHashMap<String, Object>();
    m.put("feasible", cut != null);
    m.put("threshold", cut);
    m.put("positives", positives);
    return result(m, evidence);
  }
}
