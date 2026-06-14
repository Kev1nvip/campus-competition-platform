/**
 * 获奖模块类型定义
 * 定义获奖记录相关的请求参数、响应数据和枚举类型
 */

// 获奖状态枚举
export type AwardStatus = 
  | 'PENDING'   // 待审核
  | 'APPROVED'  // 审核通过
  | 'REJECTED'  // 审核驳回

// 奖项等级枚举
export type AwardLevel = 
  | 'NATIONAL_FIRST'    // 国家级一等奖
  | 'NATIONAL_SECOND'   // 国家级二等奖
  | 'NATIONAL_THIRD'    // 国家级三等奖
  | 'PROVINCIAL_FIRST'  // 省级一等奖
  | 'PROVINCIAL_SECOND' // 省级二等奖
  | 'PROVINCIAL_THIRD'  // 省级三等奖
  | 'OTHER'             // 其他奖项

// 业务类型枚举
export type AwardBizType = 
  | 'INDIVIDUAL'  // 个人赛
  | 'TEAM'        // 团队赛

// 获奖记录视图对象（接口返回）
export interface AwardRecordVO {
  id: number
  competitionId: number
  competitionName?: string
  submitterId: number
  bizType: AwardBizType
  bizId: number
  awardLevel: AwardLevel
  awardName: string
  certificateUrl: string
  awardDate: string
  status: AwardStatus
  createdAt: string
  updatedAt: string
}

// 提交获奖记录请求参数
export interface AwardSubmitRequest {
  competitionId: number
  bizType: AwardBizType
  bizId: number
  awardLevel: AwardLevel
  awardName: string
  certificateUrl: string
  awardDate: string
}

// 奖项等级中文映射
export const awardLevelMap: Record<AwardLevel, string> = {
  NATIONAL_FIRST: '国家级一等奖',
  NATIONAL_SECOND: '国家级二等奖',
  NATIONAL_THIRD: '国家级三等奖',
  PROVINCIAL_FIRST: '省级一等奖',
  PROVINCIAL_SECOND: '省级二等奖',
  PROVINCIAL_THIRD: '省级三等奖',
  OTHER: '其他奖项'
}

// 获奖状态中文映射
export const awardStatusMap: Record<AwardStatus, string> = {
  PENDING: '待审核',
  APPROVED: '已通过',
  REJECTED: '已驳回'
}
