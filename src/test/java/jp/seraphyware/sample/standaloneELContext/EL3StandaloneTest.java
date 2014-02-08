package jp.seraphyware.sample.standaloneELContext;

import java.awt.Color;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import javax.el.ELClass;
import javax.el.ELProcessor;
import javax.el.PropertyNotWritableException;
import javax.el.ValueExpression;
import javax.el.VariableMapper;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class EL3StandaloneTest extends TestCase {

	/**
	 * Create the test case
	 *
	 * @param testName
	 *            name of the test case
	 */
	public EL3StandaloneTest(String testName) {
		super(testName);
	}

	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite() {
		return new TestSuite(EL3StandaloneTest.class);
	}

	public void testEL3Standalone() {
		// EL評価器の作成
		// (内部でEL3新設の「StandardELContext」等が作成され、
		// 環境一式が準備される.)
		ELProcessor elProc = new ELProcessor();

		// ローカル変数の定義
		// (VariableMapperには設定されない!)
		// (EL3新設の"BeanNameELResolver"で解決されるローカル変数)
		elProc.defineBean("foo", new BigDecimal("123"));
		elProc.defineBean("bar", "brabrabra");

		// 変数の定義
		// (引数のEL式のValueExpressionがVariableMapperに設定される.)
		// (Variableはローカル変数よりも優先され、setValueやEL式の代入では更新できずエラーとなる.)
		elProc.setVariable("v1", "foo * 2");

		{
			// evalによる評価
			// EL式は、内部でbracket${}で必ず囲まれるので二重にならないように注意.
			Number ret = (Number)elProc.eval("foo + 1");
			assertEquals(124, ret.intValue());
		}

		{
			// getValueによる評価

			String ret = (String)elProc.getValue("bar += '☆' += foo", String.class);
			// ※↑式全体が${}で必ず囲まれるので文字列結合する場合は
			// 新しい"文字列結合演算子 += "を使うと良い!!
			assertEquals("brabrabra☆123", ret);
		}

		{
			// ローカル変数への代入テスト
			elProc.setValue("foo", "1234");
			elProc.setValue("baz", "1234"); // ※ 存在しない変数への代入も可

			Integer retFoo = (Integer)elProc.getValue("foo", Integer.class);
			assertEquals(Integer.valueOf(1234), retFoo);
			Integer retBaz = (Integer)elProc.getValue("baz", Integer.class);
			assertEquals(Integer.valueOf(1234), retBaz);

			// (VariableMapperの確認)
			VariableMapper varMapper = elProc.getELManager().getELContext().getVariableMapper();

			ValueExpression veFoo = varMapper.resolveVariable("foo");
			assertNull(veFoo);
			ValueExpression veBaz = varMapper.resolveVariable("baz");
			assertNull(veBaz);
		}

		{
			// EL式からのローカル変数への代入テスト
			elProc.eval("qux = [1,2,3,4]"); // 配列も定義可能
			elProc.eval("map = {'a': 111, 'b': 222}"); // マップも定義可能

			System.out.println("qux=" + elProc.getValue("qux",Object.class));
			// qux=[1, 2, 3, 4]
			System.out.println("map=" + elProc.getValue("map",Object.class));
			// map={b=222, a=111}
		}

		{
			// ローカル変数とVariableMapper変数の混在
			Integer v = (Integer) elProc.getValue("v1 + foo", Integer.class);
			assertEquals(Integer.valueOf(123 + 123 * 2), v);

			// VariableMapper変数の更新はできない.
			try {
				elProc.setValue("v1", 4567);
				assertTrue(false);

			} catch (PropertyNotWritableException ex) {
				System.out.println(ex.toString());
				// javax.el.PropertyNotWritableException: Illegal Syntax for Set Operation
			}

			// VariableMapper変数はEL式からも代入できない.
			try {
				elProc.eval("v1 = 4567");
				assertTrue(false);

			} catch (PropertyNotWritableException ex) {
				System.out.println(ex.toString());
				// javax.el.PropertyNotWritableException: Illegal Syntax for Set Operation
			}
		}

		{
			// EL3の独自関数の定義方法
			Method method;
			try {
				method = getClass().getMethod("strJoin",
						new Class[] {String.class, List.class});
				elProc.defineFunction("myFn", "join", method); // staticメソッドでないとダメ

			} catch (NoSuchMethodException ex) {
				throw new RuntimeException(ex);
			}

			Object ret = elProc.eval("myFn:join(',', ['aaa', 'bbb', 'ccc', 123])");
			assertEquals("aaa,bbb,ccc,123", ret);
		}

		{
			// EL3による複数式の評価
			// (最後の式の結果が返される.)
			Integer ret = (Integer) elProc.getValue("x=1;y=2;z=x+y", Integer.class);
			assertEquals(Integer.valueOf(3), ret);
		}

		{
			// EL3のstaticフィールドへのアクセス方法
			// ※ EL3で新設されたELClassというオブジェクトでクラスをラップすると、
			// そのクラスのstaticフィールドへのアクセスが可能になる.
			// (StandardELContextはEL3で新設されたStaticFieldELResolverにより、
			// ELClassでラップされたクラスのフィールドへのアクセスを行う.)
			elProc.defineBean("Color", new ELClass(java.awt.Color.class));
			Color color = (Color) elProc.eval("Color.red");
			assertEquals(java.awt.Color.red, color);

			// スタティックメソッドの呼び出しも可
			Color color2 = (Color) elProc.eval("Color.decode('#ffffff')");
			assertEquals(java.awt.Color.white, color2);
		}

		{
			// staticメソッド式の呼び出し
			// EL3では、ELContextにimportという新しい機能が備わり、デフォルトでlana.lang.*は
			// インポート対象になっている.
			// そのため、java.lang.Mathは宣言しなくても自動的に利用可能になっている.
			Integer ret = (Integer) elProc.getValue("x=10;y=20;Math.max(x,y)", Integer.class);
			assertEquals(Integer.valueOf(20), ret);
		}

		{
			// staticフィールドのためのクラスのインポートのテスト
			elProc.getELManager().importClass("java.sql.Types");
			Integer ret = (Integer) elProc.getValue("Types.BLOB", Integer.class);
			assertEquals(Integer.valueOf(java.sql.Types.BLOB), ret);
		}

		{
			// EL3のラムダ式の利用例
			List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6);
			elProc.defineBean("list", list);

			// EL3の中間変数を使ったラムダ式とforEachによる繰り返し演算例
			String lambda1 = "sum=0;list.stream().forEach(x->(sum=sum+x));sum";
			Number ret1 = (Number) elProc.eval(lambda1);
			assertEquals(1 + 2 + 3 + 4 + 5 + 6, ret1.intValue());

			// EL3のストリームと集合演算
			String lambda1b = "list.stream().sum()";
			Number ret1b = (Number) elProc.eval(lambda1b);
			assertEquals(1 + 2 + 3 + 4 + 5 + 6, ret1b.intValue());

			// EL3のストリームと全体演算(reduce)、初期値0なのでストリームが空でも0となる。
			String lambda1c = "list.stream().reduce(0,(a,b)->a+b)";
			Number ret1c = (Number) elProc.eval(lambda1c);
			assertEquals(1 + 2 + 3 + 4 + 5 + 6, ret1c.intValue());

			// EL3のラムダ式を使ったリストのフィルタリングと型変換例
			String lambda2 ="lst=[];list.stream().forEach(x->((x % 2 == 0)?" +
					"lst.add(Integer.toString(x)):null));lst";
			@SuppressWarnings("unchecked")
			List<String> ret2 = (List<String>) elProc.eval(lambda2);
			assertEquals(Arrays.asList("2", "4", "6"), ret2);

			// EL3のラムダ式を使ったリストのフィルタリングと型変換例(filter+map版)
			String lambda2b = "list.stream().filter(x->(x % 2 == 0)).map(" +
					"x->Integer.toString(x)).toList()";
			@SuppressWarnings("unchecked")
			List<String> ret2b = (List<String>) elProc.eval(lambda2b);
			assertEquals(Arrays.asList("2", "4", "6"), ret2b);
		}

		{
			// EL3のストリームのデバッグに便利なpeek関数
			List<Integer> list = Arrays.asList(1, 2, 3);
			elProc.defineBean("list", list);
			Method printMethod;
			try {
				printMethod = getClass().getMethod(
						"debugPrint",
						new Class[] {String.class, Object.class});
				elProc.defineFunction("myFn", "print", printMethod);

			} catch (NoSuchMethodException ex) {
				throw new RuntimeException(ex);
			}

			// EL3のラムダ式を使ったリストのフィルタリングと型変換例(filter版)
			String lambda = "list.stream().peek(x->myFn:print('1>', x))." +
					"filter(x->(x % 2 == 0)).peek(x->myFn:print('2>', x))." +
					"map(x->Integer.toString(x)).toList()";
			@SuppressWarnings("unchecked")
			List<String> ret = (List<String>) elProc.eval(lambda);
			System.out.println("ret=" + ret);
		}

		{
			// 降順ソートの上位3件の取得 (sortedとlimit)
			String lambda = "list=[9,3,6,4];" +
					"cmp=(a,b)->-(a-b);" + // 比較関数をラムダ変数で定義
					"cnv=(x)->Integer.toString(x);" + // タイプ変換関数をラムダ変数で定義
					"list.stream().sorted(cmp).limit(3).map(cnv).toList()";
			assertEquals(Arrays.asList("9", "6", "4"), elProc.eval(lambda));

			// 最大値を求める。リストが空ならば0。(maxとorElse)
			Number ret = (Number) elProc.eval("list=[1,3,2];list.stream().max().orElse(0)");
			assertEquals(3, ret.intValue());

			// 最初のアイテムを求める。リストが空ならば空文字 (findFirstとorElse)
			assertEquals("", elProc.eval("list=[];list.stream().findFirst().orElse('')"));
		}

		{
			// ラムダ式を返すラムダ式の変数格納と、変数からのラムダ式の呼び出し
			Number ret = (Number) elProc.eval("fn=a->(b->a+b);fn2=(a,f)->f(a);fn2(12,fn(21))");
			assertEquals(33, ret.intValue());
		}
	}

	/**
	 * ELから呼び出される関数(デバッグ用)
	 * @param prefix
	 * @param val
	 */
	public static void debugPrint(String prefix, Object val) {
		System.out.println(prefix + val);
	}

	/**
	 * ELから呼び出される関数
	 * @param 区切り文字
	 * @param args 文字列として連結する要素が格納されたリスト
	 * @return 文字列とした連結された結果
	 */
	public static String strJoin(String sep, List<Object> args) {
		StringBuilder buf = new StringBuilder();
		if (args != null) {
			for (Object arg : args) {
				if (buf.length() > 0) {
					buf.append(sep);
				}
				buf.append(arg != null ? arg.toString() : "");
			}
		}
		return buf.toString();
	}
}
