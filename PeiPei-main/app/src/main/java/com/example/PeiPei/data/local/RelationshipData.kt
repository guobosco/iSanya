// 文件说明：人际关系、标签等关系类静态或配置数据。

package com.example.Lulu.data.local

object RelationshipData {
    // 关系代码：
    // f: 父, m: 母, h: 夫, w: 妻, s: 子, d: 女, 
    // ob: 兄, lb: 弟, os: 姐, ls: 妹

    val data: Map<String, List<String>> = mapOf(
        // === 自身 ===
        "" to listOf("我"),

        // === 直系 & 核心家庭 ===
        "f" to listOf("爸爸"),
        "m" to listOf("妈妈"),
        "h" to listOf("老公"),
        "w" to listOf("老婆"),
        "s" to listOf("儿子"),
        "d" to listOf("女儿"),
        "ob" to listOf("哥哥"),
        "lb" to listOf("弟弟"),
        "os" to listOf("姐姐"),
        "ls" to listOf("妹妹"),

        // === 兄弟姐妹的互查 (多义性来源) ===
        // 哥哥的...
        "ob,ob" to listOf("哥哥"),
        "ob,lb" to listOf("哥哥", "弟弟", "我"), // 哥哥的弟弟：可能是另一个哥哥，或者我，或者我的弟弟
        "ob,os" to listOf("姐姐"),
        "ob,ls" to listOf("姐姐", "妹妹", "我"), // 哥哥的妹妹：可能是姐姐，或者我(女)，或者妹妹

        // 弟弟的...
        "lb,ob" to listOf("哥哥", "弟弟", "我"), // 弟弟的哥哥：可能是我的哥哥，或者我，或者另一个弟弟
        "lb,lb" to listOf("弟弟"),
        "lb,os" to listOf("姐姐", "妹妹", "我"), 
        "lb,ls" to listOf("妹妹"),

        // 姐姐的...
        "os,ob" to listOf("哥哥"),
        "os,lb" to listOf("哥哥", "弟弟", "我"),
        "os,os" to listOf("姐姐"),
        "os,ls" to listOf("姐姐", "妹妹", "我"),

        // 妹妹的...
        "ls,ob" to listOf("哥哥", "弟弟", "我"), // 妹妹的哥哥：可能是哥哥，或者我(男)，或者弟弟(比妹妹大的弟弟)
        "ls,lb" to listOf("弟弟"),
        "ls,os" to listOf("姐姐", "妹妹", "我"),
        "ls,ls" to listOf("妹妹"),

        // === 父母的兄弟姐妹 (父系) ===
        "f,f" to listOf("爷爷"),
        "f,m" to listOf("奶奶"),
        "f,ob" to listOf("伯父"),
        "f,lb" to listOf("叔叔"),
        "f,os" to listOf("姑妈"),
        "f,ls" to listOf("姑姑"),
        
        // 父母的兄弟姐妹 (母系)
        "m,f" to listOf("姥爷"),
        "m,m" to listOf("姥姥"),
        "m,ob" to listOf("舅舅"),
        "m,lb" to listOf("舅舅"),
        "m,os" to listOf("姨妈"),
        "m,ls" to listOf("阿姨"),

        // === 祖辈的扩展 ===
        "f,f,f" to listOf("太爷爷"),
        "f,f,m" to listOf("太奶奶"),
        "f,m,f" to listOf("太姥爷"),
        "f,m,m" to listOf("太姥姥"),
        "m,f,f" to listOf("太姥爷"),
        "m,f,m" to listOf("太姥姥"),
        "m,m,f" to listOf("太姥爷"),
        "m,m,m" to listOf("太姥姥"),

        // === 姻亲 (父系) ===
        "f,ob,w" to listOf("伯母"),
        "f,lb,w" to listOf("婶婶"),
        "f,os,h" to listOf("姑父"),
        "f,ls,h" to listOf("姑父"),
        
        // === 姻亲 (母系) ===
        "m,ob,w" to listOf("舅妈"),
        "m,lb,w" to listOf("舅妈"),
        "m,os,h" to listOf("姨父"),
        "m,ls,h" to listOf("姨父"),

        // === 兄弟姐妹的配偶 ===
        "ob,w" to listOf("嫂子"),
        "lb,w" to listOf("弟妹"),
        "os,h" to listOf("姐夫"),
        "ls,h" to listOf("妹夫"),

        // === 兄弟姐妹的子女 ===
        "ob,s" to listOf("侄子"),
        "ob,d" to listOf("侄女"),
        "lb,s" to listOf("侄子"),
        "lb,d" to listOf("侄女"),
        "os,s" to listOf("外甥"),
        "os,d" to listOf("外甥女"),
        "ls,s" to listOf("外甥"),
        "ls,d" to listOf("外甥女"),

        // === 配偶的父母 ===
        "h,f" to listOf("公公"),
        "h,m" to listOf("婆婆"),
        "w,f" to listOf("岳父"),
        "w,m" to listOf("岳母"),

        // === 配偶的兄弟姐妹 ===
        "h,ob" to listOf("大伯子"),
        "h,lb" to listOf("小叔子"),
        "h,os" to listOf("大姑子"),
        "h,ls" to listOf("小姑子"),
        "w,ob" to listOf("大舅哥"),
        "w,lb" to listOf("小舅子"),
        "w,os" to listOf("大姨子"),
        "w,ls" to listOf("小姨子"),
        
        // === 配偶的兄弟姐妹的配偶 (连襟/妯娌) ===
        "h,ob,w" to listOf("妯娌"), // 大伯子的妻子
        "h,lb,w" to listOf("妯娌"), // 小叔子的妻子
        "h,os,h" to listOf("姐夫"), // 大姑子的丈夫 (通常随孩子叫姑父，或者姐夫)
        "h,ls,h" to listOf("妹夫"), // 小姑子的丈夫

        "w,ob,w" to listOf("嫂子"), // 大舅哥的妻子
        "w,lb,w" to listOf("弟妹"), // 小舅子的妻子
        "w,os,h" to listOf("连襟"), // 大姨子的丈夫
        "w,ls,h" to listOf("连襟"), // 小姨子的丈夫

        // === 子女的配偶 & 子女 ===
        "s,w" to listOf("儿媳"),
        "d,h" to listOf("女婿"),
        "s,s" to listOf("孙子"),
        "s,d" to listOf("孙女"),
        "d,s" to listOf("外孙"),
        "d,d" to listOf("外孙女"),
        
        // === 孙辈的子女 ===
        "s,s,s" to listOf("曾孙"),
        "s,s,d" to listOf("曾孙女"),
        "s,d,s" to listOf("曾外孙"),
        "s,d,d" to listOf("曾外孙女"),

        // === 父母的父母的兄弟姐妹 (爷辈的旁系) ===
        // 爷爷的兄弟姐妹
        "f,f,ob" to listOf("伯公"), // 爷爷的哥哥
        "f,f,lb" to listOf("叔公"), // 爷爷的弟弟
        "f,f,os" to listOf("姑奶奶"), // 爷爷的姐姐
        "f,f,ls" to listOf("姑奶奶"), // 爷爷的妹妹
        
        // 奶奶的兄弟姐妹
        "f,m,ob" to listOf("舅公"), 
        "f,m,lb" to listOf("舅公"),
        "f,m,os" to listOf("姨奶奶"),
        "f,m,ls" to listOf("姨奶奶"),
        
        // 姥爷的兄弟姐妹
        "m,f,ob" to listOf("伯外公"), // 也有叫 舅公
        "m,f,lb" to listOf("叔外公"),
        "m,f,os" to listOf("姑姥姥"),
        "m,f,ls" to listOf("姑姥姥"),

        // 姥姥的兄弟姐妹
        "m,m,ob" to listOf("舅姥爷"),
        "m,m,lb" to listOf("舅姥爷"),
        "m,m,os" to listOf("姨姥姥"),
        "m,m,ls" to listOf("姨姥姥"),

        // === 堂表亲 (同辈) ===
        // 父亲的兄弟的子女 (堂)
        "f,ob,s" to listOf("堂哥"), 
        "f,ob,d" to listOf("堂姐"),
        "f,lb,s" to listOf("堂弟"),
        "f,lb,d" to listOf("堂妹"),
        
        // 父亲的姐妹的子女 (表)
        "f,os,s" to listOf("表哥"),
        "f,os,d" to listOf("表姐"),
        "f,ls,s" to listOf("表弟"), // 假设比我小
        "f,ls,d" to listOf("表妹"),

        // 母亲的兄弟的子女 (表)
        "m,ob,s" to listOf("表哥"),
        "m,ob,d" to listOf("表姐"),
        "m,lb,s" to listOf("表弟"),
        "m,lb,d" to listOf("表妹"),

        // 母亲的姐妹的子女 (表/姨兄弟姐妹)
        "m,os,s" to listOf("表哥"),
        "m,os,d" to listOf("表姐"),
        "m,ls,s" to listOf("表弟"),
        "m,ls,d" to listOf("表妹"),
        
        // === 自身多义性 ===
        "f,s" to listOf("哥哥", "弟弟", "我"),
        "f,d" to listOf("姐姐", "妹妹", "我"),
        "m,s" to listOf("哥哥", "弟弟", "我"),
        "m,d" to listOf("姐姐", "妹妹", "我")
    )
    
    // 简化版反向查找或推断逻辑（如果需要更复杂逻辑可以在此添加）
    fun getRelation(chain: List<String>): List<String> {
        if (chain.isEmpty()) return listOf("我")
        val key = chain.joinToString(",")
        return data[key] ?: listOf("未知亲戚")
    }
}
