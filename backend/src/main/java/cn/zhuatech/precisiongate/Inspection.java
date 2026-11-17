package cn.zhuatech.precisiongate;

import static cn.zhuatech.precisiongate.Checks.*;

import java.util.*;

/**
 * 按所选阈值回放，分别列出误报与漏报及其离阈值的距离；没有可行阈值时不输出虚假的零误判结论。 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ ·
 * 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
 */
final class Inspection {
  /**
   * 有序结果，可表达无法估计的数值。上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ ·
   * 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
   */
  private static Map<String, Object> fields(Object... pairs) {
    var row = new LinkedHashMap<String, Object>();
    for (int i = 0; i < pairs.length; i += 2) row.put((String) pairs[i], pairs[i + 1]);
    return row;
  }

  /**
   * 对已校验的输入生成复核证据。上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ ·
   * 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
   */
  @SuppressWarnings("unchecked")
  static Map<String, Object> evaluate(
      Map<String, Object> cfg, List<Map<String, Object>> rows, Map<String, Object> base) {
    var details = new ArrayList<Map<String, Object>>();
    var m = (Map<String, Object>) base.get("metrics");

    Number cut = (Number) m.get("threshold");
    if (cut == null)
      return result(
          fields("evaluated", false, "falsePositive", null, "falseNegative", null), details);
    int fp = 0, fn = 0;
    for (var r : rows) {
      double s = ((Number) r.get("score")).doubleValue();
      int truth = ((Number) r.get("truth")).intValue();
      boolean pos = s >= cut.doubleValue();
      if (pos == (truth == 1)) continue;
      if (pos) fp++;
      else fn++;
      details.add(
          fields(
              "id",
              r.get("id"),
              "errorType",
              pos ? "FP" : "FN",
              "score",
              s,
              "truth",
              truth,
              "distance",
              Math.abs(s - cut.doubleValue())));
    }
    details.sort(Comparator.comparingDouble(v -> ((Number) v.get("distance")).doubleValue()));
    return result(fields("evaluated", true, "falsePositive", fp, "falseNegative", fn), details);
  }
}
