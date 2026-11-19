## 中文

- 应用版本从 0.2.6（8）升至 0.2.7（9），沿用同一签名，覆盖安装保留已有记录。
- 多选批量删除改为长按列表项触发：长按任意一条记录进入多选（长按项自动选中并有振动反馈），不再占用一整行按钮空间；孕肚记录、每日打卡、产检记录、产检计划、孕期日记、个人攻略、购物账本、购物流程 8 个页面统一支持。
- 多选状态下点按行即勾选/取消（修复打卡页多选时误开编辑的问题）。
- 购物账本新增「消费分析」入口。
- 数据库升级到 v6（订单明细表与商品 brand/productUrl 字段），为后续导入功能预留。

## English

- The app advances from 0.2.6 (8) to 0.2.7 (9) with the same signing certificate; updating in place keeps existing records.
- Batch multi-select delete is now triggered by long-pressing any list item: long-press enters selection mode with the pressed item pre-checked plus haptic feedback, instead of a full-row "multi-select" button. Supported across all 8 record lists (belly, check-in, checkup records and plans, diary, remedies, shopping ledger and flow).
- While in selection mode, tapping a row toggles its checkbox (fixes the check-in page opening the editor by mistake in selection mode).
- The shopping ledger gains a "消费分析" (spending analysis) entry.
- Database advances to v6 (order-details table plus brand/productUrl columns), reserved for the upcoming import feature.
