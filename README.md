# Synopsis
EL式をプログラム側より評価するための仕組みについてのサンプル.

# Description

変数等を用意し、EL式を与えることにより、EL式の評価結果を受け取ったり、EL式によって変数を設定する、あるいはアクションメソッドを呼び出す、といった一連のEL式の動きを実験している。

- 独自のELContextの設定方法
- ELResolverによる独自の変数の解決方法
- VariableResolverと、ValueWrapperによる変数の扱い方
- FunctionMapperによるEL式からの関数の呼び出し

# References
- http://www.techscore.com/tech/Java/JavaEE/JSP/15-4/
- http://grepcode.com/file/repo1.maven.org/maven2/javax.servlet.jsp/jsp-api/2.2.1b03/javax/servlet/jsp/el/ScopedAttributeELResolver.java#ScopedAttributeELResolver
- http://mk.hatenablog.com/entry/20041210/1132029220
- http://oss.infoscience.co.jp/myfaces/cwiki.apache.org/confluence/display/MYFACES/ELResolver+ordering.html
- http://d.hatena.ne.jp/shin/20090426/p1
- http://kiruah.sblo.jp/pages/user/iphone/article?article_id=56792402

