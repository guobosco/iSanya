// 文件说明：资产或内容分类的数据模型定义。

package com.example.Lulu.data.model

object AssetCategories {
    data class CategoryNode(
        val name: String,
        val children: List<CategoryNode> = emptyList()
    )

    val allCategories = listOf(
        CategoryNode(
            "现金类",
            listOf(
                CategoryNode("现金", listOf(
                    CategoryNode("人民币"),
                    CategoryNode("外币"),
                    CategoryNode("其他")
                )),
                CategoryNode("银行存款", listOf(
                    CategoryNode("活期存款"),
                    CategoryNode("定期存款"),
                    CategoryNode("其他")
                )),
                CategoryNode("电子钱包", listOf(
                    CategoryNode("微信零钱"),
                    CategoryNode("支付宝余额"),
                    CategoryNode("其他")
                )),
                CategoryNode("货币基金", listOf(
                    CategoryNode("余额宝"),
                    CategoryNode("其他")
                ))
            )
        ),
        CategoryNode(
            "固定收益类",
            listOf(
                CategoryNode("银行理财产品", listOf(
                    CategoryNode("保本型"),
                    CategoryNode("非保本型"),
                    CategoryNode("其他")
                )),
                CategoryNode("债券", listOf(
                    CategoryNode("国债"),
                    CategoryNode("企业债"),
                    CategoryNode("可转债"),
                    CategoryNode("其他")
                )),
                CategoryNode("保险年金", listOf(
                    CategoryNode("养老年金"),
                    CategoryNode("教育金"),
                    CategoryNode("其他")
                )),
                CategoryNode("住房公积金账户", listOf(
                    CategoryNode("公积金余额"),
                    CategoryNode("其他")
                )),
                CategoryNode("混合型基金", listOf(
                    CategoryNode("偏债混合"),
                    CategoryNode("平衡混合"),
                    CategoryNode("其他")
                ))
            )
        ),
        CategoryNode(
            "股票权益类",
            listOf(
                CategoryNode("股票", listOf(
                    CategoryNode("A股账户"),
                    CategoryNode("美股账户"),
                    CategoryNode("港股账户"),
                    CategoryNode("其他账户")
                )),
                CategoryNode("股票型基金", listOf(
                    CategoryNode("指数基金"),
                    CategoryNode("主动管理型"),
                    CategoryNode("其他")
                )),
                CategoryNode("私募股权", listOf(
                    CategoryNode("PE/VC"),
                    CategoryNode("其他")
                ))
            )
        ),
        CategoryNode(
            "实物资产类",
            listOf(
                CategoryNode("不动产", listOf(
                    CategoryNode("自住房产"),
                    CategoryNode("投资房产"),
                    CategoryNode("商铺/写字楼"),
                    CategoryNode("其他")
                )),
                CategoryNode("贵金属", listOf(
                    CategoryNode("黄金"),
                    CategoryNode("白银"),
                    CategoryNode("其他")
                )),
                CategoryNode("收藏品", listOf(
                    CategoryNode("珠宝首饰"),
                    CategoryNode("古董字画"),
                    CategoryNode("其他")
                )),
                CategoryNode("交通工具", listOf(
                    CategoryNode("汽车"),
                    CategoryNode("其他")
                ))
            )
        ),
        CategoryNode(
            "保险类",
            listOf(
                CategoryNode("人寿保险", listOf(
                    CategoryNode("定期寿险"),
                    CategoryNode("终身寿险"),
                    CategoryNode("其他")
                )),
                CategoryNode("健康险", listOf(
                    CategoryNode("重疾险"),
                    CategoryNode("医疗险"),
                    CategoryNode("其他")
                )),
                CategoryNode("意外险", listOf(
                    CategoryNode("综合意外"),
                    CategoryNode("交通意外"),
                    CategoryNode("其他")
                )),
                CategoryNode("其他保险", listOf(
                    CategoryNode("投连险"),
                    CategoryNode("分红险"),
                    CategoryNode("其他")
                ))
            )
        ),
        CategoryNode(
            "其他资产",
            listOf(
                CategoryNode("债权", listOf(
                    CategoryNode("个人借款"),
                    CategoryNode("企业借款"),
                    CategoryNode("其他")
                )),
                CategoryNode("知识产权", listOf(
                    CategoryNode("专利"),
                    CategoryNode("著作权"),
                    CategoryNode("其他")
                )),
                CategoryNode("虚拟资产", listOf(
                    CategoryNode("游戏账号"),
                    CategoryNode("数字货币"),
                    CategoryNode("其他")
                ))
            )
        ),
        CategoryNode(
            "负债",
            listOf(
                CategoryNode("短期负债", listOf(
                    CategoryNode("信用卡欠款"),
                    CategoryNode("花呗/白条"),
                    CategoryNode("短期借款"),
                    CategoryNode("其他")
                )),
                CategoryNode("长期负债", listOf(
                    CategoryNode("房贷"),
                    CategoryNode("车贷"),
                    CategoryNode("装修贷"),
                    CategoryNode("其他")
                ))
            )
        )
    )
}
