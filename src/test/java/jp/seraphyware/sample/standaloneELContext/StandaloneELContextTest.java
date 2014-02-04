package jp.seraphyware.sample.standaloneELContext;

import java.awt.Color;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.el.CompositeELResolver;
import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.MethodExpression;
import javax.el.PropertyNotWritableException;
import javax.el.ValueExpression;
import javax.el.VariableMapper;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * EL式を単独で使用するためのELコンテキストの使用例および単体テストコード.<br>
 */
public class StandaloneELContextTest extends TestCase {

	/**
	 * Create the test case
	 *
	 * @param testName
	 *            name of the test case
	 */
	public StandaloneELContextTest(String testName) {
		super(testName);
	}

	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite() {
		return new TestSuite(StandaloneELContextTest.class);
	}

	/**
	 * EL式からBeanによるアクセスのテスト用のクラス.
	 */
	public static final class MyBean {
		private int x;
		private int y;

		public int getX() {
			return x;
		}
		
		public void setX(int x) {
			this.x = x;
		}

		public int getY() {
			return y;
		}
		
		public void setY(int y) {
			this.y = y;
		}
		
		public int init(int x, int y) {
			this.x = x;
			this.y = y;
			return x + y;
		}

		public String mes(String val) {
			return "!" + val + "!";
		}
	}

	/**
	 * [テスト1] 独自のELResolverを用いたEL式の評価と解決
	 */
	public void testCustomELResolverApp() {
		final Map<String, Object> data = createTestData();
		StandaloneELContext elContext = new StandaloneELContext();
		elContext.getELResolver().add(new StandaloneBaseELResolver() {
			@Override
			protected Map<String, Object> getLocalContext(ELContext elContext) {
				return data;
			}
		});
		doTest(elContext);
	}

	/**
	 * [テスト2] VariableMapperを用いたEL式の評価と解決
	 */
	public void testSimpleELResolverApp() {
		StandaloneELContext elContext = new StandaloneELContext();

		ExpressionFactory ef = ExpressionFactory.newInstance();
		VariableMapper varMapper = elContext.getVariableMapper();
		
		Map<String, Object> data = createTestData();
		for (Map.Entry<String, Object> entry : data.entrySet()) {
			String key = entry.getKey();
			Object val = entry.getValue();
			
			ValueExpression wrapValue = ef.createValueExpression(val, Object.class);
			varMapper.setVariable(key, wrapValue);
		}
		doTest(elContext);
	}

	/**
	 * テストデータの作成
	 * @return テストデータを格納したマップ
	 */
	private Map<String, Object> createTestData() {
		final Map<String, Object> data = new HashMap<String, Object>();

		data.put("str", "Hello, World!");
		data.put("num", BigDecimal.valueOf(1234));
		data.put("arr", Arrays.asList(11, 22, 33, 44));

		MyBean myBean = new MyBean();
		myBean.x = 100;
		myBean.y = 200;
		data.put("bean", myBean);

		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("key1", "VAL1");
		map.put("key2", "VAL2");
		map.put("key3", myBean);
		map.put("loop", map);
		data.put("map", map);

		data.put("num1", BigDecimal.valueOf(1));
		data.put("num2", BigDecimal.valueOf(2));
		data.put("int3", Integer.valueOf(3));
		data.put("int4", Integer.valueOf(4));
		
		return data;
	}
	
	/**
	 * EL式を評価する.
	 * @param elContext
	 */
	private void doTest(ELContext elContext) {
		ExpressionFactory ef = ExpressionFactory.newInstance();

		{
			// リテラルテスト
			String expression = "ok";
			ValueExpression ve = ef.createValueExpression(elContext,
					expression, String.class);
			Object ret = ve.getValue(elContext);
			assertEquals("ok", ret);
		}

		{
			// 単純変数のテスト(文字列)
			String expression = "${str}";
			ValueExpression ve = ef.createValueExpression(elContext,
					expression, String.class);
			Object ret = ve.getValue(elContext);
			assertEquals("Hello, World!", ret);
		}

		{
			// 単純変数のテスト(数値)
			String expression = "${num}";
			ValueExpression ve = ef.createValueExpression(elContext,
					expression, String.class);
			Object ret = ve.getValue(elContext);
			assertEquals("1234", ret);
		}

		{
			// 単純変数のテスト(数値BigDecimal)
			String expression = "${num1 + num2}";
			ValueExpression ve = ef.createValueExpression(elContext,
					expression, String.class);
			Object ret = ve.getValue(elContext);
			assertEquals("3", ret);
		}

		{
			// 単純変数のテスト(数値Integer)
			String expression = "${int3 - int4}";
			ValueExpression ve = ef.createValueExpression(elContext,
					expression, String.class);
			Object ret = ve.getValue(elContext);
			assertEquals("-1", ret);
		}

		{
			// ビーンのテスト(メソッド呼び出し)
			String expression = "${bean.mes('aaa')}";
			ValueExpression ve = ef.createValueExpression(elContext,
					expression, String.class);
			Object ret = ve.getValue(elContext);
			assertEquals("!aaa!", ret);
		}

		{
			// 文字列置換のテスト、およびアクセス
			String expression = "ok: ${str}:${num}:${bean.x - bean.y}";
			ValueExpression ve = ef.createValueExpression(elContext,
					expression, String.class);
			Object ret = ve.getValue(elContext);
			assertEquals("ok: Hello, World!:1234:-100", ret);
		}

		{
			// 計算値を文字列として受け取るテスト
			String expression = "${bean.x - bean.y - 10}";
			ValueExpression ve = ef.createValueExpression(elContext,
					expression, String.class);
			Object ret = ve.getValue(elContext);
			assertEquals("-110", ret);
		}

		{
			// BigDecimalとして計算値を戻すテスト
			String expression = "${bean.x - bean.y - 10}";
			ValueExpression ve = ef.createValueExpression(elContext,
					expression, BigDecimal.class);
			Object ret = ve.getValue(elContext);
			assertEquals(BigDecimal.valueOf(-110), ret);
		}

		{
			// 配列のテスト
			String expression = "${arr[1]}";
			ValueExpression ve = ef.createValueExpression(elContext,
					expression, BigDecimal.class);
			Object ret = ve.getValue(elContext);
			assertEquals(BigDecimal.valueOf(22), ret);
		}

		{
			// マップのテスト(添え字)
			String expression = "${map['key1']}";
			ValueExpression ve = ef.createValueExpression(elContext,
					expression, String.class);
			Object ret = ve.getValue(elContext);
			assertEquals("VAL1", ret);
		}

		{
			// マップのテスト(ドット構文)
			String expression = "${map.key2}";
			ValueExpression ve = ef.createValueExpression(elContext,
					expression, String.class);
			Object ret = ve.getValue(elContext);
			assertEquals("VAL2", ret);
		}

		{
			// マップのネストしたテスト1(ドット構文)
			String expression = "${map.key3.x}";
			ValueExpression ve = ef.createValueExpression(elContext,
					expression, BigDecimal.class);
			Object ret = ve.getValue(elContext);
			assertEquals(BigDecimal.valueOf(100), ret);
		}

		{
			// マップのネストしたテスト2(ドット構文)
			String expression = "${map.loop.key2}";
			ValueExpression ve = ef.createValueExpression(elContext,
					expression, String.class);
			Object ret = ve.getValue(elContext);
			assertEquals("VAL2", ret);
		}

		{
			// マップのネストしたテスト3(空キー)
			String expression = "${map.key4}";
			ValueExpression ve = ef.createValueExpression(elContext,
					expression, String.class);
			Object ret = ve.getValue(elContext);
			assertEquals("", ret);
		}

		{
			// 関数のテスト1
			String expression = "${fn:length(123)}";
			ValueExpression ve = ef.createValueExpression(elContext,
					expression, String.class);
			Object ret = ve.getValue(elContext);
			assertEquals("3", ret);
		}

		{
			// 関数のテスト2
			String expression = "${fn:length(map)}";
			ValueExpression ve = ef.createValueExpression(elContext,
					expression, String.class);
			Object ret = ve.getValue(elContext);
			assertEquals("4", ret);
		}

		{
			// 関数のテスト3 (関数のネスト)
			String expression = "${fn:length(fn:length(map))}";
			ValueExpression ve = ef.createValueExpression(elContext,
					expression, String.class);
			Object ret = ve.getValue(elContext);
			assertEquals("1", ret);
		}

		{
			// 未登録変数のテスト
			String expression = "#{xyz}#{abc}";
			ValueExpression ve = ef.createValueExpression(elContext,
					expression, String.class);
			assertTrue(ve != null); // 評価するまではエラーは発生しない. = OK
		}

		{
			// 変数のテスト
			String expression = "#{map}";
			ValueExpression ve = ef.createValueExpression(elContext,
					expression, Object.class);

			// 途中結果をエイリアスとして設定
			VariableMapper varMapper = elContext.getVariableMapper();
			varMapper.setVariable("mapAlias", ve);
			assertTrue(ve != null);

			// エイリアス名を使った式
			String expression2 = "${mapAlias.key1}";
			ValueExpression ve2 = ef.createValueExpression(elContext,
					expression2, String.class);
			assertTrue(ve2 != null);

			// エイリアス名を使った式を評価する.
			Object ret = ve2.getValue(elContext);
			assertEquals("VAL1", ret);

			varMapper.setVariable("mapAlias", null);
		}

		{
			// EL式による代入(map)
			String expression = "#{map.keyx}";
			ValueExpression ve = ef.createValueExpression(elContext,
					expression, Object.class);
			
			ve.setValue(elContext, "update!");

			ValueExpression ve2 = ef.createValueExpression(elContext, "${map.keyx}", String.class);
			Object ret = ve2.getValue(elContext);
			assertEquals("update!", ret);
		}

		{
			// EL式による代入(bean)
			String expression = "#{bean.x}";
			ValueExpression ve = ef.createValueExpression(elContext,
					expression, Integer.class);
			
			ve.setValue(elContext, "999");

			ValueExpression ve2 = ef.createValueExpression(elContext, "${bean.x}", Integer.class);
			Object ret = ve2.getValue(elContext);
			assertEquals(999, ret);
		}

		{
			// EL式による代入(bean)文法エラー
			String expression = "#{bean.x}x";
			ValueExpression ve = ef.createValueExpression(elContext,
					expression, Integer.class);
			try {
				ve.setValue(elContext, "999");
				assertTrue(false);

			} catch (PropertyNotWritableException e) {
				// 文法エラーなので例外が出て正しい
				assertTrue(true);
			}
		}
		
		{
			// メソッド呼び出し (引数がELに含まれるケース)
			String expression = "#{bean.init(int3, int4)}";
			MethodExpression me = ef.createMethodExpression(
					elContext,
					expression,
					Object.class,
					new Class<?>[0]);
			Object ret = me.invoke(elContext, new Object[0]);
			assertEquals(Integer.valueOf(7), ret);
		}

		{
			// メソッド呼び出し=アクションのバインド風 (引数はプログラム側より指定)
			String expression = "#{bean.init}";
			MethodExpression me = ef.createMethodExpression(
					elContext,
					expression,
					Object.class,
					new Class<?>[] {int.class, int.class});

			Object ret = me.invoke(
					elContext,
					new Object[]{Integer.valueOf(100), Integer.valueOf(200)});
			
			assertEquals(Integer.valueOf(300), ret);
		}
		
		{
			// 単純変数のラッピングと変数への設定と評価
			ValueExpression ve = ef.createValueExpression("FOOBAR", String.class);
			elContext.getVariableMapper().setVariable("fooBar", ve);

			String expression = "hello, ${fooBar}!";
			ValueExpression ve2 = ef.createValueExpression(elContext, expression, Object.class);
			Object ret = ve2.getValue(elContext);
			
			assertEquals("hello, FOOBAR!", ret);
		}

		assertTrue(true);
	}
	
	/**
	 * 独自のColorELResolverによる解釈のテスト
	 */
	public void testColorELResolver() {
		StandaloneELContext elContext = new StandaloneELContext() {
			@Override
			protected void initELResolver(CompositeELResolver resolver) {
				// ColorELResolverが返すマーカーオブジェクトを標準のBeanResolverが解決する前に
				// 独自に解釈する必要があるので、リゾルバの順序を前にもってくる必要がある。
				resolver.add(new ColorELResolver());
				super.initELResolver(resolver);
			}
		};
		
		ExpressionFactory ef = ExpressionFactory.newInstance();
		
		{
			// リテラルのテスト
			String expression = "${123 + 456}";
			ValueExpression ve = ef.createValueExpression(elContext, expression, Integer.class);
			Object ret = ve.getValue(elContext);
			assertEquals(123 + 456, ret);
		}
		
		{
			// ColorELResolverのテスト
			String expression = "${Color}";
			ValueExpression ve = ef.createValueExpression(elContext, expression, Object.class);
			Object ret = ve.getValue(elContext); // ColorELResolverが作成したマーカーオブジェクトが返される.
			assertTrue(ret != null);
		}

		{
			// ColorELResolverの子要素のテスト
			String expression = "${Color.red}";
			ValueExpression ve = ef.createValueExpression(elContext, expression, Object.class);
			Object ret = ve.getValue(elContext);
			assertTrue(ret instanceof Color);
			assertEquals(Color.red, ret);
		}
	}
}
