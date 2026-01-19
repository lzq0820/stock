// src/utils/helpers.js
export function truncateText(text, maxLength = 14) {
  if (text.length <= maxLength) return text;
  return text.substring(0, maxLength) + '...';
}

export function copyToClipboard(text) {
  navigator.clipboard.writeText(text);
}

export function sortStocksByChangePercent(stocks) {
  return stocks.sort((a, b) => b.changePercent - a.changePercent);
}

export function groupStocksByLimitUpDays(stocks) {
  const groups = {};
  stocks.forEach(stock => {
    const days = stock.limitUpDays || 1;
    if (!groups[days]) groups[days] = [];
    groups[days].push(stock);
  });
  return groups;
}

export function calculatePromotionRate(groups, currentDay) {
  const prevDay = currentDay - 1;
  if (!groups[prevDay] || !groups[currentDay]) return 'N/A';
  const total = groups[prevDay].length;
  const success = groups[currentDay].length;
  const rate = total > 0 ? ((success / total) * 100).toFixed(2) : 0;
  return `${success}/${total}=${rate}%`;
}