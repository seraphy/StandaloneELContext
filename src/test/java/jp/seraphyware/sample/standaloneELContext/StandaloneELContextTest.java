package jp.seraphyware.sample.standaloneELContext;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.el.ExpressionFactory;
import javax.el.ValueExpression;

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

	public static final class MyBean {
		private int x;
		private int y;

		public int getX() {
			return x;
		}

		public int getY() {
			return y;
		}
	}

	/**
	 * Rigourous Test :-)
	 */
	public void testApp() {
		Map<String, Object> data = new HashMap<String, Object>();

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

		ExpressionFactory ef = ExpressionFactory.newInstance();

		StandaloneELContext elContext = new StandaloneELContext();
		elContext.putContext(StandaloneELContext.class, data);

		{
			// リテラルテスト
			String expression = "ok";
			ValueExpression ve = ef.createValueExpression(elContext, expression,
					String.class);
			Object ret = ve.getValue(elContext);
			assertEquals("ok", ret);
		}

		{
			// 単純変数のテスト(文字列)
			String expression = "${str}";
			ValueExpression ve = ef.createValueExpression(elContext, expression,
					String.class);
			Object ret = ve.getValue(elContext);
			assertEquals("Hello, World!", ret);
		}

		{
			// 単純変数のテスト(数値)
			String expression = "${num}";
			ValueExpression ve = ef.createValueExpression(elContext, expression,
					String.class);
			Object ret = ve.getValue(elContext);
			assertEquals("1234", ret);
		}

		{
			// 文字列置換のテスト、およびアクセス
			String expression = "ok: ${str}:${num}:${bean.x - bean.y}";
			ValueExpression ve = ef.createValueExpression(elContext, expression,
					String.class);
			Object ret = ve.getValue(elContext);
			assertEquals("ok: Hello, World!:1234:-100", ret);
		}
		
		{
			// 計算値を文字列として受け取るテスト
			String expression = "${bean.x - bean.y - 10}";
			ValueExpression ve = ef.createValueExpression(elContext, expression,
					String.class);
			Object ret = ve.getValue(elContext);
			assertEquals("-110", ret);
		}

		{
			// BigDecimalとして計算値を戻すテスト
			String expression = "${bean.x - bean.y - 10}";
			ValueExpression ve = ef.createValueExpression(elContext, expression,
					BigDecimal.class);
			Object ret = ve.getValue(elContext);
			assertEquals(BigDecimal.valueOf(-110), ret);
		}
		
		{
			// 配列のテスト
			String expression = "${arr[1]}";
			ValueExpression ve = ef.createValueExpression(elContext, expression,
					BigDecimal.class);
			Object ret = ve.getValue(elContext);
			assertEquals(BigDecimal.valueOf(22), ret);
		}

		{
			// マップのテスト(添え字)
			String expression = "${map['key1']}";
			ValueExpression ve = ef.createValueExpression(elContext, expression,
					String.class);
			Object ret = ve.getValue(elContext);
			assertEquals("VAL1", ret);
		}

		{
			// マップのテスト(ドット構文)
			String expression = "${map.key2}";
			ValueExpression ve = ef.createValueExpression(elContext, expression,
					String.class);
			Object ret = ve.getValue(elContext);
			assertEquals("VAL2", ret);
		}

		{
			// マップのネストしたテスト1(ドット構文)
			String expression = "${map.key3.x}";
			ValueExpression ve = ef.createValueExpression(elContext, expression,
					BigDecimal.class);
			Object ret = ve.getValue(elContext);
			assertEquals(BigDecimal.valueOf(100), ret);
		}

		{
			// マップのネストしたテスト2(ドット構文)
			String expression = "${map.loop.key2}";
			ValueExpression ve = ef.createValueExpression(elContext, expression,
					String.class);
			Object ret = ve.getValue(elContext);
			assertEquals("VAL2", ret);
		}

		{
			// マップのネストしたテスト3(空キー)
			String expression = "${map.key4}";
			ValueExpression ve = ef.createValueExpression(elContext, expression,
					String.class);
			Object ret = ve.getValue(elContext);
			assertEquals("", ret);
		}

		assertTrue(true);
	}
}
