/** 官网内容「扁平列表」分区的字段描述（metrics/outcomes/solutions/honors/channels/faq） */
export interface ContentFieldSchema {
  prop: string
  label: string
  placeholder?: string
  type?: 'input' | 'textarea'
  rows?: number
  /** 12 栅格中的占宽，默认 12（整行） */
  span?: number
}

export const METRIC_FIELDS: ContentFieldSchema[] = [
  { prop: 'value', label: '数值', placeholder: '如 15', span: 3 },
  { prop: 'label', label: '说明', placeholder: '如 年行业深耕', span: 9 }
]

export const OUTCOME_FIELDS: ContentFieldSchema[] = [
  { prop: 'mark', label: '数字', placeholder: '如 ↓38', span: 2 },
  { prop: 'suffix', label: '单位', placeholder: '如 %', span: 2 },
  { prop: 'label', label: '说明', placeholder: '如 交付周期缩短', span: 8 }
]

export const SOLUTION_FIELDS: ContentFieldSchema[] = [
  { prop: 'title', label: '方案名称', placeholder: '如 智能仓储方案', span: 5 },
  { prop: 'desc', label: '一句话说明', placeholder: '面向什么场景、解决什么问题', span: 7 }
]

export const HONOR_FIELDS: ContentFieldSchema[] = [
  { prop: 'name', label: '证书 / 奖项', placeholder: '名称', span: 4 },
  { prop: 'issuer', label: '颁发方', placeholder: '颁发机构', span: 3 },
  { prop: 'year', label: '年份', placeholder: '如 2024', span: 2 },
  { prop: 'cat', label: '分类', placeholder: '如 资质证书', span: 3 }
]

export const CHANNEL_FIELDS: ContentFieldSchema[] = [
  { prop: 'name', label: '渠道名称', placeholder: '如 电话咨询', span: 4 },
  { prop: 'desc', label: '说明', placeholder: '提示客户备注渠道', span: 8 }
]

export const FAQ_FIELDS: ContentFieldSchema[] = [
  { prop: 'q', label: '问题', placeholder: '客户最常问的问题', span: 12 },
  {
    prop: 'a',
    label: '答案',
    placeholder: '简洁回答（同步 FAQPage 结构化数据）',
    type: 'textarea',
    rows: 2,
    span: 12
  }
]
