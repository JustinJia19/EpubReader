package org.example.epub_demo1.entity.enums;

public enum BookCategory {
    //'FICTION', 'TECHNOLOGY', 'HISTORY', 'DETECTIVE','BIOGRAPHY', 'OTHER'
    FICTION("小说"),
    TECHNOLOGY("科学技术"),
    HISTORY("历史人文"),
    DETECTIVE("推理小说"),
    BIOGRAPHY("人物传记"),
    OTHER("其他分类");

    private final String chineseName;

    BookCategory(String chineseName) {
        this.chineseName = chineseName;
    }

    public String getChineseName() {
        return chineseName;
    }
}