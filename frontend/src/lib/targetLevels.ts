export interface TargetLevelConfig {
  level: number;
  label: string;
  companies: string;
  easyPct: number;
  mediumPct: number;
  hardPct: number;
  targetTotal: number;
}

export const targetLevels: TargetLevelConfig[] = [
  { level: 1, label: 'Service-based', companies: 'TCS, Infosys, Wipro', easyPct: 80, mediumPct: 20, hardPct: 0, targetTotal: 50 },
  { level: 2, label: 'Service-based+', companies: 'Cognizant, Accenture', easyPct: 70, mediumPct: 30, hardPct: 0, targetTotal: 80 },
  { level: 3, label: 'Mid-tier Product', companies: 'Paytm, Zomato', easyPct: 50, mediumPct: 40, hardPct: 10, targetTotal: 120 },
  { level: 4, label: 'Product', companies: 'Swiggy, Ola', easyPct: 35, mediumPct: 50, hardPct: 15, targetTotal: 180 },
  { level: 5, label: 'Good Product', companies: 'Uber, Flipkart, Cred', easyPct: 20, mediumPct: 55, hardPct: 25, targetTotal: 250 },
  { level: 6, label: 'Strong Product', companies: 'Stripe, Atlassian', easyPct: 15, mediumPct: 50, hardPct: 35, targetTotal: 320 },
  { level: 7, label: 'Top Tech', companies: 'Google, Microsoft, Amazon', easyPct: 10, mediumPct: 40, hardPct: 50, targetTotal: 400 },
  { level: 8, label: 'Big Tech', companies: 'Meta, Apple, Netflix', easyPct: 5, mediumPct: 35, hardPct: 60, targetTotal: 500 },
  { level: 9, label: 'Elite', companies: 'FAANG+, Uber ATG', easyPct: 5, mediumPct: 25, hardPct: 70, targetTotal: 600 },
  { level: 10, label: 'God Tier', companies: 'OpenAI, Quant, DeepMind', easyPct: 0, mediumPct: 20, hardPct: 80, targetTotal: 800 },
];

export function getTargetLevel(level: number): TargetLevelConfig {
  return targetLevels[level - 1] || targetLevels[4];
}
