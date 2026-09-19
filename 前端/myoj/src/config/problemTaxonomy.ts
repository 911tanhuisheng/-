export const PROBLEM_TYPE_OPTIONS = [
  { label: '全部题型', value: '' },
  { label: '传统编程题', value: 'TEXT' },
  { label: '图像目标计数题', value: 'IMAGE_OBJECT_COUNT' },
  { label: '图像分类题', value: 'IMAGE_CLASSIFICATION' },
  { label: '目标检测框题', value: 'IMAGE_OBJECT_DETECTION' },
  { label: 'OCR 文字识别题', value: 'IMAGE_OCR' },
  { label: '图像属性分析题', value: 'IMAGE_ANALYSIS' },
] as const

export const PROBLEM_DIFFICULTIES = ['入门', '简单', '中等', '困难'] as const

export const PROBLEM_KNOWLEDGE_TAGS = [
  '基础语法', '分支', '循环', '字符串', '数组', '链表', '栈', '队列', '哈希表',
  '排序', '查找', '双指针', '枚举', '模拟', '贪心', '递归', '递推', '数学',
  '二分', '树', '图论', '搜索', '动态规划', '并查集', '最短路', '图像识别',
] as const

export function problemTypeLabel(type?: string): string {
  const option = PROBLEM_TYPE_OPTIONS.find((item) => item.value === type)
  return option?.label || '传统编程题'
}

export function difficultyFromTags(tags?: string[]): string {
  return tags?.find((tag) => (PROBLEM_DIFFICULTIES as readonly string[]).includes(tag)) || '未分级'
}

export function knowledgeFromTags(tags?: string[]): string[] {
  return (tags || []).filter((tag) => (PROBLEM_KNOWLEDGE_TAGS as readonly string[]).includes(tag))
}
