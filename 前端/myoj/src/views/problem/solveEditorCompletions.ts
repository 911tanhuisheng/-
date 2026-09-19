/**
 * Monaco 编辑器：C++ / Java / Python 静态补全片段（类 IDE 常用模板）。
 * 与 ProblemSolveView 解耦，便于维护与扩充。
 */
import type { LoadedMonaco } from '@/lib/monacoEditor'
import type { IRange, languages } from 'monaco-editor'

type CompletionItem = languages.CompletionItem
type CompletionItemKind = languages.CompletionItemKind

function item(
  monaco: LoadedMonaco,
  range: IRange,
  label: string,
  kind: CompletionItemKind,
  insertText: string,
  detail: string,
  isSnippet: boolean,
): CompletionItem {
  return {
    label,
    kind,
    insertText,
    detail,
    range,
    sortText: isSnippet ? '0' : '1',
    ...(isSnippet
      ? { insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet }
      : {}),
  }
}

export function buildCppCompletions(monaco: LoadedMonaco, range: IRange): CompletionItem[] {
  const K = monaco.languages.CompletionItemKind
  return [
    item(monaco, range, 'for', K.Snippet, 'for (int ${1:i} = 0; ${1:i} < ${2:n}; ${1:i}++) {\n\t$0\n}', 'for 循环', true),
    item(monaco, range, 'for-range', K.Snippet, 'for (auto &${1:x} : ${2:container}) {\n\t$0\n}', '范围 for', true),
    item(monaco, range, 'while', K.Snippet, 'while (${1:condition}) {\n\t$0\n}', 'while 循环', true),
    item(monaco, range, 'if', K.Snippet, 'if (${1:condition}) {\n\t$0\n}', 'if 分支', true),
    item(monaco, range, 'if-else', K.Snippet, 'if (${1:condition}) {\n\t$2\n} else {\n\t$0\n}', 'if-else', true),
    item(monaco, range, 'ios-fast', K.Snippet, 'ios::sync_with_stdio(false);\ncin.tie(nullptr);', '关同步加速 cin', false),
    item(monaco, range, 'main', K.Snippet, 'int main() {\n\tios::sync_with_stdio(false);\n\tcin.tie(nullptr);\n\t$0\n\treturn 0;\n}', 'main 模板', true),
    item(monaco, range, 'bits/stdc++.h', K.File, '#include <bits/stdc++.h>', '万能头', false),
    item(monaco, range, 'include-vector', K.File, '#include <vector>', '', false),
    item(monaco, range, 'include-algorithm', K.File, '#include <algorithm>', '', false),
    item(monaco, range, 'include-queue', K.File, '#include <queue>', '', false),
    item(monaco, range, 'include-map', K.File, '#include <map>', '', false),
    item(monaco, range, 'include-unordered_map', K.File, '#include <unordered_map>', '', false),
    item(monaco, range, 'include-set', K.File, '#include <set>', '', false),
    item(monaco, range, 'include-string', K.File, '#include <string>', '', false),
    item(monaco, range, 'include-cmath', K.File, '#include <cmath>', '', false),
    item(monaco, range, 'include-iomanip', K.File, '#include <iomanip>', '', false),
    item(monaco, range, 'include-sstream', K.File, '#include <sstream>', '', false),
    item(monaco, range, 'vector-int', K.Constructor, 'vector<int> ${1:a};', 'vector<int>', true),
    item(monaco, range, 'vector-pair', K.Constructor, 'vector<pair<int,int>> ${1:a};', 'vector<pair<int,int>>', true),
    item(monaco, range, 'pair', K.Constructor, 'pair<int,int>', 'pair<int,int>', false),
    item(monaco, range, 'make_pair', K.Function, 'make_pair(${1:x}, ${2:y})', 'make_pair', true),
    item(monaco, range, 'priority_queue-max', K.Constructor, 'priority_queue<int> ${1:pq};', '大顶堆 int', true),
    item(monaco, range, 'priority_queue-min', K.Constructor, 'priority_queue<int, vector<int>, greater<int>> ${1:pq};', '小顶堆 int', true),
    item(monaco, range, 'sort', K.Function, 'sort(${1:a}.begin(), ${1:a}.end());', '升序 sort', true),
    item(monaco, range, 'sort-greater', K.Function, 'sort(${1:a}.begin(), ${1:a}.end(), greater<int>());', '降序 sort', true),
    item(monaco, range, 'sort-lambda', K.Function, 'sort(${1:a}.begin(), ${1:a}.end(), [](const auto &x, const auto &y) {\n\treturn $0;\n});', '自定义比较 sort', true),
    item(monaco, range, 'lower_bound', K.Function, 'lower_bound(${1:a}.begin(), ${1:a}.end(), ${2:key})', 'lower_bound', true),
    item(monaco, range, 'upper_bound', K.Function, 'upper_bound(${1:a}.begin(), ${1:a}.end(), ${2:key})', 'upper_bound', true),
    item(monaco, range, 'unique', K.Function, '${1:a}.erase(unique(${1:a}.begin(), ${1:a}.end()), ${1:a}.end());', '去重 + erase', true),
    item(monaco, range, 'reverse', K.Function, 'reverse(${1:a}.begin(), ${1:a}.end());', 'reverse', true),
    item(monaco, range, 'max_element', K.Function, '*max_element(${1:a}.begin(), ${1:a}.end())', 'max_element', true),
    item(monaco, range, 'min_element', K.Function, '*min_element(${1:a}.begin(), ${1:a}.end())', 'min_element', true),
    item(monaco, range, 'accumulate', K.Function, 'accumulate(${1:a}.begin(), ${1:a}.end(), 0LL)', '前缀和/累加', true),
    item(monaco, range, 'gcd', K.Function, 'gcd(${1:a}, ${2:b})', 'C++17 gcd', true),
    item(monaco, range, 'swap', K.Function, 'swap(${1:a}, ${2:b});', 'swap', true),
    item(monaco, range, 'cin-line', K.Snippet, 'cin >> ${1:x};', 'cin 读入', true),
    item(monaco, range, 'cout-line', K.Snippet, 'cout << ${1:x} << "\\n";', 'cout 输出换行', true),
    item(monaco, range, 'getline-string', K.Function, 'getline(cin, ${1:s});', '读一整行到 string', true),
    item(monaco, range, 'stringstream', K.Constructor, 'stringstream ss;\nss << ${1:expr};\n$0', 'stringstream', true),
    item(monaco, range, 'stoi', K.Function, 'stoi(${1:s})', 'string 转 int', true),
    item(monaco, range, 'stoll', K.Function, 'stoll(${1:s})', 'string 转 long long', true),
    item(monaco, range, 'to_string', K.Function, 'to_string(${1:x})', '数字转 string', true),
    item(monaco, range, 'constexpr-inf', K.Constant, 'const long long INF = 4e18;', '无穷大常量', false),
    item(monaco, range, 'using-ll', K.Reference, 'using ll = long long;', 'll 别名', false),
    item(monaco, range, 'using-pii', K.Reference, 'using pii = pair<int,int>;', 'pii 别名', false),
    item(monaco, range, 'lambda', K.Snippet, '[](${1:const auto &x}) { return $0; }', 'lambda 片段', true),
    item(monaco, range, 'unordered_map-int', K.Constructor, 'unordered_map<int,int> ${1:mp};', '哈希 map', true),
    item(monaco, range, 'set-int', K.Constructor, 'set<int> ${1:st};', '有序 set', true),
    item(monaco, range, 'multiset', K.Constructor, 'multiset<int> ${1:st};', 'multiset', true),
    item(monaco, range, 'deque', K.Constructor, 'deque<int> ${1:dq};', 'deque', true),
    item(monaco, range, 'stack', K.Constructor, 'stack<int> ${1:st};', 'stack', true),
    item(monaco, range, 'bitset', K.Constructor, 'bitset<${1:64}> ${2:b};', 'bitset', true),
    item(monaco, range, 'memset', K.Function, 'memset(${1:a}, 0, sizeof(${1:a}));', 'memset 0', true),
  ]
}

export function buildJavaCompletions(monaco: LoadedMonaco, range: IRange): CompletionItem[] {
  const K = monaco.languages.CompletionItemKind
  return [
    item(monaco, range, 'for', K.Snippet, 'for (int ${1:i} = 0; ${1:i} < ${2:n}; ${1:i}++) {\n\t$0\n}', 'for 循环', true),
    item(monaco, range, 'for-each', K.Snippet, 'for (${1:Integer} ${2:x} : ${3:list}) {\n\t$0\n}', '增强 for', true),
    item(monaco, range, 'while', K.Snippet, 'while (${1:condition}) {\n\t$0\n}', 'while', true),
    item(monaco, range, 'if', K.Snippet, 'if (${1:condition}) {\n\t$0\n}', 'if', true),
    item(monaco, range, 'try-catch', K.Snippet, 'try {\n\t$1\n} catch (Exception e) {\n\t$0\n}', 'try-catch', true),
    item(monaco, range, 'psvm', K.Snippet, 'public static void main(String[] args) throws Exception {\n\t$0\n}', 'main', true),
    item(monaco, range, 'BufferedReader', K.Snippet, 'BufferedReader br = new BufferedReader(new InputStreamReader(System.in));', '快读', false),
    item(monaco, range, 'StringTokenizer', K.Snippet, 'StringTokenizer st = new StringTokenizer(br.readLine());', 'StringTokenizer', false),
    item(monaco, range, 'nextInt', K.Method, 'Integer.parseInt(st.nextToken())', '读下一个 int', false),
    item(monaco, range, 'readLine', K.Method, 'br.readLine()', '读一行', false),
    item(monaco, range, 'ArrayList', K.Class, 'ArrayList<${1:Integer}> ${2:list} = new ArrayList<>();', 'ArrayList', true),
    item(monaco, range, 'HashMap', K.Class, 'HashMap<${1:Integer}, ${2:Integer}> ${3:map} = new HashMap<>();', 'HashMap', true),
    item(monaco, range, 'HashSet', K.Class, 'HashSet<${1:Integer}> ${2:set} = new HashSet<>();', 'HashSet', true),
    item(monaco, range, 'PriorityQueue-min', K.Class, 'PriorityQueue<${1:Integer}> ${2:pq} = new PriorityQueue<>();', '小顶堆', true),
    item(monaco, range, 'PriorityQueue-max', K.Class, 'PriorityQueue<${1:Integer}> ${2:pq} = new PriorityQueue<>(Collections.reverseOrder());', '大顶堆', true),
    item(monaco, range, 'Arrays.sort', K.Method, 'Arrays.sort(${1:a});', 'Arrays.sort', true),
    item(monaco, range, 'Arrays.binarySearch', K.Method, 'Arrays.binarySearch(${1:a}, ${2:key})', '二分查找', true),
    item(monaco, range, 'StringBuilder', K.Class, 'StringBuilder sb = new StringBuilder();', 'StringBuilder', false),
    item(monaco, range, 'sb-append', K.Method, 'sb.append(${1:x})', 'append', true),
    item(monaco, range, 'Math.max', K.Method, 'Math.max(${1:a}, ${2:b})', 'max', true),
    item(monaco, range, 'Math.min', K.Method, 'Math.min(${1:a}, ${2:b})', 'min', true),
    item(monaco, range, 'Math.abs', K.Method, 'Math.abs(${1:x})', 'abs', true),
    item(monaco, range, 'Comparator-lambda', K.Interface, 'Comparator.<${1:Integer}>comparingInt(x -> x)', '比较器', true),
    item(monaco, range, 'Stream-of', K.Method, 'Arrays.stream(${1:a})', '数组转流', true),
    item(monaco, range, 'Long-parse', K.Method, 'Long.parseLong(${1:s})', 'parse long', true),
    item(monaco, range, 'Integer-parse', K.Method, 'Integer.parseInt(${1:s})', 'parse int', true),
    item(monaco, range, 'System.out.println', K.Method, 'System.out.println(${1:x});', 'println', true),
    item(monaco, range, 'throws-Exception', K.Reference, 'throws Exception', 'throws Exception', false),
  ]
}

export function buildPythonCompletions(monaco: LoadedMonaco, range: IRange): CompletionItem[] {
  const K = monaco.languages.CompletionItemKind
  return [
    item(monaco, range, 'for', K.Snippet, 'for ${1:i} in range(${2:n}):\n    $0', 'for range', true),
    item(monaco, range, 'for-enumerate', K.Snippet, 'for ${1:i}, ${2:x} in enumerate(${3:arr}):\n    $0', 'enumerate', true),
    item(monaco, range, 'while', K.Snippet, 'while ${1:condition}:\n    $0', 'while', true),
    item(monaco, range, 'if', K.Snippet, 'if ${1:condition}:\n    $0', 'if', true),
    item(monaco, range, 'if-main', K.Snippet, 'if __name__ == "__main__":\n    $0', 'main 守卫', true),
    item(monaco, range, 'def', K.Snippet, 'def ${1:solve}(${2:args}):\n    $0', '函数 def', true),
    item(monaco, range, 'input-split', K.Snippet, '${1:a}, ${2:b} = map(int, input().split())', '一行两个 int', true),
    item(monaco, range, 'input-list', K.Snippet, 'list(map(int, input().split()))', '一行 int 列表', true),
    item(monaco, range, 'readline-int', K.Snippet, 'int(sys.stdin.readline())', 'stdin 读 int', true),
    item(monaco, range, 'readline-ints', K.Snippet, 'list(map(int, sys.stdin.readline().split()))', 'stdin 读 int 列表', true),
    item(monaco, range, 'import-sys', K.Module, 'import sys', '', false),
    item(monaco, range, 'import-math', K.Module, 'import math', '', false),
    item(monaco, range, 'import-itertools', K.Module, 'from itertools import combinations, permutations, product', '', false),
    item(monaco, range, 'import-bisect', K.Module, 'import bisect', '', false),
    item(monaco, range, 'import-heapq', K.Module, 'import heapq', '', false),
    item(monaco, range, 'import-collections', K.Module, 'from collections import deque, defaultdict, Counter', '', false),
    item(monaco, range, 'heapq-heappush', K.Method, 'heapq.heappush(${1:hq}, ${2:x})', 'heappush', true),
    item(monaco, range, 'heapq-heappop', K.Method, 'heapq.heappop(${1:hq})', 'heappop', true),
    item(monaco, range, 'heapq-heapify', K.Method, 'heapq.heapify(${1:arr})', 'heapify', true),
    item(monaco, range, 'bisect-left', K.Method, 'bisect.bisect_left(${1:a}, ${2:x})', 'bisect_left', true),
    item(monaco, range, 'bisect-right', K.Method, 'bisect.bisect_right(${1:a}, ${2:x})', 'bisect_right', true),
    item(monaco, range, 'defaultdict-int', K.Constructor, 'defaultdict(int)', 'defaultdict(int)', false),
    item(monaco, range, 'defaultdict-list', K.Constructor, 'defaultdict(list)', 'defaultdict(list)', false),
    item(monaco, range, 'Counter', K.Class, 'Counter(${1:iterable})', 'Counter', true),
    item(monaco, range, 'combinations', K.Method, 'combinations(${1:iterable}, ${2:r})', '组合', true),
    item(monaco, range, 'permutations', K.Method, 'permutations(${1:iterable}, ${2:r})', '排列', true),
    item(monaco, range, 'math-gcd', K.Method, 'math.gcd(${1:a}, ${2:b})', 'gcd', true),
    item(monaco, range, 'math-lcm', K.Method, 'math.lcm(${1:a}, ${2:b})', 'lcm (3.9+)', true),
    item(monaco, range, 'math-inf', K.Constant, 'float("inf")', '正无穷', false),
    item(monaco, range, 'list-comp', K.Snippet, '[${1:expr} for ${2:x} in ${3:iterable}]', '列表推导', true),
    item(monaco, range, 'set-comp', K.Snippet, '{${1:expr} for ${2:x} in ${3:iterable}}', '集合推导', true),
    item(monaco, range, 'sorted', K.Method, 'sorted(${1:iterable})', 'sorted', true),
    item(monaco, range, 'reversed', K.Method, 'reversed(${1:seq})', 'reversed', true),
    item(monaco, range, 'zip', K.Method, 'zip(${1:a}, ${2:b})', 'zip', true),
    item(monaco, range, 'enumerate', K.Method, 'enumerate(${1:iterable})', 'enumerate', true),
    item(monaco, range, 'functools-cache', K.Module, 'from functools import lru_cache\n\n@lru_cache(maxsize=None)\ndef ${1:f}(${2:n}):\n    $0', '记忆化递归', true),
  ]
}
