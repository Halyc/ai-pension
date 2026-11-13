const { marked } = require('marked');
marked.setOptions({ breaks: true, gfm: true });

// 模拟 preprocessMarkdown 函数
function preprocessMarkdown(text) {
  let s = text.replace(/\r\n/g, '\n').replace(/\r/g, '\n');
  s = s.replace(/(\S)(\d+\.\s*[^\d\s])/g, '$1\n$2');
  s = s.replace(/(\S)([·•])/g, '$1\n$2');
  s = s.replace(/([^\n\s])\n+(?=\s*[-*]\s)/g, '$1\n');
  s = s.replace(/([^\n])(#{1,6})\s/g, '$1\n$2 ');
  s = s.replace(/\n{3,}/g, '\n\n');
  return s.trim();
}

// 测试1: AI 没有换行，用 - 分隔
const t1 = `张叔叔您好！我帮您整理了最近三个月的健康档案记录，一共有4条，具体如下：

1. 2026年09月04日13:59 - 血压: 117/84mmHg - 血糖: 6.3mmol/L - 心率: 84次/分 - 体重: 70kg - 身高: 176cm - BMI: 22.62
2. 2026年08月31日16:10 - 血压: 115/87mmHg - 血糖: 6mmol/L - 心率: 98次/分 - 体重: 68kg - 身高: 176cm - BMI: 21.73
3. 2026年08月26日11:03 - 血压: 120/80mmHg - 血糖: 5.9mmol/L - 心率: 79次/分 - 体重: 70.5kg - 身高: 175cm - BMI: 23.4
4. 2026年08月25日21:03 - 血压: 120/80mmHg - 血糖: 6.2mmol/L - 心率: 88次/分 - 体重: 70.5kg - 身高: 175cm - BMI: 23`;

console.log("=== 测试1: AI 格式正确，每条有独立换行 ===");
console.log("--- marked 渲染结果 ---");
console.log(marked.parse(preprocessMarkdown(t1)));

// 测试2: AI 把所有记录拼成一行
const t2 = `张叔叔您好！我帮您整理了最近三个月的健康档案记录，一共有4条，具体如下：

1.2026年09月04日13:59-血压: 117/84mmHg-血糖: 6.3mmol/L-心率: 84次/分-体重: 70kg-身高: 176cm-BMI: 22.62.2026年08月31日16:10-血压: 115/87mmHg-血糖: 6mmol/L-心率: 98次/分-体重: 68kg-身高: 176cm-BMI: 21.73.2026年08月26日11:03-血压: 120/80mmHg-血糖: 5.9mmol/L-心率: 79次/分-体重: 70.5kg-身高: 175cm-BMI: 23.4.2026年08月25日21:03-血压: 120/80mmHg-血糖: 6.2mmol/L-心率: 88次/分-体重: 70.5kg-身高: 175cm-BMI: 23`;

console.log("\n\n=== 测试2: AI 拼成一行（关键！）===");
console.log("--- preprocess 后 ---");
const p2 = preprocessMarkdown(t2);
console.log(p2);
console.log("--- marked 渲染 ---");
console.log(marked.parse(p2));

// 测试3: 模拟用户截图里的真实情况 - 流式输出中途，AI 把 BMI 的小数拆成了两行
// 比如流式过程中 "BMI: 22." 先输出，然后 "\n62.2026..." 接着输出
// 这时候 marked 会把 "62." 当成有序列表！
const t3 = `张叔叔您好！我帮您整理了最近三个月的健康档案记录：

1. 2026年09月04日13:59
   血压: 117/84mmHg
   BMI: 22.
62. 2026年08月31日16:10
   血压: 115/87mmHg
   BMI: 21.
73. 2026年08月26日11:03
   血压: 120/80mmHg
   BMI: 23.
234. 2026年08月25日21:03
   血压: 120/80mmHg
   BMI: 23`;

console.log("\n\n=== 测试3: 模拟用户截图里的效果（BMI 被流式拆断）===");
console.log("--- preprocess 后 ---");
const p3 = preprocessMarkdown(t3);
console.log(p3);
console.log("--- marked 渲染 ---");
console.log(marked.parse(p3));