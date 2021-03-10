# Synopsis
EL式(2.2/3.0)をプログラム側より評価するための仕組みについてのサンプル. [![Build Status](https://travis-ci.com/seraphy/StandaloneELContext.svg?branch=master)](https://travis-ci.com/seraphy/StandaloneELContext)

# Description

変数等を用意し、EL式を与えることにより、EL式の評価結果を受け取ったり、EL式によって変数を設定する、あるいはアクションメソッドを呼び出す、といった一連のEL式の動きを実験している。

- 独自のELContextの設定方法
- ELResolverによる独自の変数の解決方法
- VariableResolverと、ValueWrapperによる変数の扱い方
- FunctionMapperによるEL式からの関数の呼び出し

# How to build

このプロジェクトはeclipseで作成されています。

Java7を使用しています。

ライブラリですので単体で実行できるものではありませんが、動作確認用プログラムが同梱されています。

プロジェクトのビルドにはmaven3を使用しています。

mvn clean test

で動作確認ができます。

# References, Bibliography
- 同じようなことをやってるサンプル例
 - http://illegalargumentexception.blogspot.jp/2008/04/java-using-el-outside-j2ee.html
 - http://stackoverflow.com/questions/17026863/java-how-to-evaluate-an-el-expression-standalone-outside-any-web-framework
- 参考にしたところ
 - 独自のELResolverの実装例 http://www.techscore.com/tech/Java/JavaEE/JSP/15-4/
 - 独自のELResolverの実装例 http://d.hatena.ne.jp/shin/20090426/p1
 - 独自のELResolverの実装例 http://kiruah.sblo.jp/pages/user/iphone/article?article_id=56792402
 - ELResolverの評価順序 http://mk.hatenablog.com/entry/20041210/1132029220
 - ELResolverの評価順序 http://oss.infoscience.co.jp/myfaces/cwiki.apache.org/confluence/display/MYFACES/ELResolver+ordering.html
- EL3の参考
 - https://jcp.org/en/jsr/detail?id=341
 - http://www.infoq.com/jp/news/2013/07/el3
 - https://weblogs.java.net/blog/swchan2/archive/2013/07/01/standard-deviation-illustration-expression-language-30-servlet-environment
