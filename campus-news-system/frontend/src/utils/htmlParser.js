/**
 * HTML内容解析工具
 * 用于处理富文本内容，转换为纯文本
 */

/**
 * 将HTML内容转换为纯文本
 * @param {string} html - HTML内容
 * @param {boolean} preserveLineBreaks - 是否保留换行
 * @returns {string} 纯文本内容
 */
export function htmlToText(html, preserveLineBreaks = true) {
  if (!html) return ''
  
  // 如果不是HTML内容，直接返回
  if (!html.includes('<') || !html.includes('>')) {
    return html
  }
  
  // 创建临时DOM元素
  const tempDiv = document.createElement('div')
  
  // 处理特殊HTML实体
  html = html
    .replace(/&nbsp;/g, ' ')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&amp;/g, '&')
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
  
  // 如果需要保留换行
  if (preserveLineBreaks) {
    // 将<br>和<p>等标签替换为换行符
    html = html
      .replace(/<br\s*\/?>/gi, '\n')
      .replace(/<\/p>/gi, '\n\n')
      .replace(/<p\s*\/?>/gi, '')
      .replace(/<\/div>/gi, '\n')
      .replace(/<div\s*\/?>/gi, '')
      .replace(/<\/li>/gi, '\n')
      .replace(/<li\s*\/?>/gi, '• ')
  }
  
  tempDiv.innerHTML = html
  
  // 获取纯文本
  let text = tempDiv.textContent || tempDiv.innerText || ''
  
  // 清理多余的空白
  text = text
    .replace(/\n{3,}/g, '\n\n')  // 将多个换行替换为最多两个
    .replace(/\s+/g, ' ')         // 将多个空格替换为一个
    .trim()
  
  return text
}

/**
 * 截取文本并保持完整性
 * @param {string} text - 文本内容
 * @param {number} maxLength - 最大长度
 * @param {string} suffix - 后缀（如省略号）
 * @returns {string} 截取后的文本
 */
export function truncateText(text, maxLength = 100, suffix = '...') {
  if (!text || text.length <= maxLength) {
    return text
  }
  
  // 在最大长度附近找到合适的断点（句号、逗号等）
  const truncated = text.substring(0, maxLength)
  const lastPunctuation = Math.max(
    truncated.lastIndexOf('。'),
    truncated.lastIndexOf('，'),
    truncated.lastIndexOf('！'),
    truncated.lastIndexOf('？'),
    truncated.lastIndexOf('. '),
    truncated.lastIndexOf(', ')
  )
  
  if (lastPunctuation > maxLength * 0.8) {
    return text.substring(0, lastPunctuation + 1) + suffix
  }
  
  // 如果没有找到合适的标点，则在词边界截断
  const lastSpace = truncated.lastIndexOf(' ')
  if (lastSpace > maxLength * 0.8) {
    return text.substring(0, lastSpace) + suffix
  }
  
  return truncated + suffix
}

/**
 * 提取HTML中的纯文本摘要
 * @param {string} html - HTML内容
 * @param {number} maxLength - 摘要最大长度
 * @returns {string} 摘要文本
 */
export function extractSummary(html, maxLength = 200) {
  const text = htmlToText(html, false)
  return truncateText(text, maxLength)
}

/**
 * 统计文本字数（中文算一个字，英文单词算一个）
 * @param {string} text - 文本内容
 * @returns {number} 字数
 */
export function countWords(text) {
  if (!text) return 0
  
  // 移除空白字符
  text = text.trim()
  
  // 匹配中文字符
  const chineseChars = text.match(/[\u4e00-\u9fa5]/g) || []
  
  // 匹配英文单词
  const englishWords = text.match(/[a-zA-Z]+/g) || []
  
  return chineseChars.length + englishWords.length
}
