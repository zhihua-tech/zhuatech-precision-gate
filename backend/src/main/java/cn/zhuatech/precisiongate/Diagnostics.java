package cn.zhuatech.precisiongate;

import java.util.*;

/**
 * 将分数作为正例概率，按十个等宽分箱计算平均预测概率、真实正例率和加权校准误差（ECE），并报告二元 Brier 分数。空箱不冒充零误差；1 进入最后一箱。 上海如静知华信息科技有限公司 ·
 * https://www.zhuatech.cn/ · 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
 */
final class Diagnostics {
  /** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
  private static Map<String, Object> map(Object... pairs) {
    var m = new LinkedHashMap<String, Object>();
    for (int i = 0; i < pairs.length; i += 2) m.put((String) pairs[i], pairs[i + 1]);
    return m;
  }

  /** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
  private static double num(Map<String, Object> m, String k) {
    return ((Number) m.get(k)).doubleValue();
  }

  /** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
  private static Double ratio(double a, double b) {
    return b == 0 ? null : a / b;
  }

  /** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
  @SuppressWarnings("unchecked")
  static Map<String, Object> evaluate(
      Map<String, Object> cfg, List<Map<String, Object>> rows, Map<String, Object> base) {
    var metrics = (Map<String, Object>) base.get("metrics");
    var evidence = (List<Map<String, Object>>) base.get("rows");
    var details = new ArrayList<Map<String, Object>>();
    int[] count = new int[10], positives = new int[10];
    double[] sums = new double[10];
    double squares = 0;
    for (var r : rows) {
      double score = num(r, "score");
      int truth = (int) num(r, "truth"),
          bin = Math.min(9, java.math.BigDecimal.valueOf(score).movePointRight(1).intValue());
      count[bin]++;
      positives[bin] += truth;
      sums[bin] += score;
      squares += (score - truth) * (score - truth);
    }
    double ece = 0;
    int occupied = 0;
    for (int i = 0; i < 10; i++) {
      Double mean = null, rate = null, gap = null;
      if (count[i] > 0) {
        occupied++;
        mean = sums[i] / count[i];
        rate = (double) positives[i] / count[i];
        gap = Math.abs(mean - rate);
        ece += gap * count[i] / rows.size();
      }
      details.add(
          map(
              "bin",
              i + 1,
              "minInclusive",
              i / 10.0,
              "maxEdge",
              (i + 1) / 10.0,
              "support",
              count[i],
              "meanScore",
              mean,
              "observedRate",
              rate,
              "absoluteGap",
              gap));
    }
    return Checks.result(
        map("brier", squares / rows.size(), "ece", ece, "occupiedBins", occupied), details);
  }
}
