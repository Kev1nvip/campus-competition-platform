/**
 * AI 推荐模块类型定义
 * 定义推荐请求参数、响应数据和相关类型
 */

import type { CompetitionVO } from './competition'

// 推荐请求参数
export interface RecommendRequest {
  // 用户输入的感兴趣方向
  direction: string
  // 可选：推荐数量
  count?: number
}

// 推荐结果项
export interface RecommendItem {
  // 竞赛信息
  competition: CompetitionVO
  // 匹配度（0-100）
  matchScore: number
  // 推荐理由
  reason: string
  // 相关标签
  tags: string[]
}

// 推荐响应数据
export interface RecommendResponse {
  // 推荐列表
  items: RecommendItem[]
  // 来源说明
  source: string
  // 推荐时间
  recommendTime: string
}