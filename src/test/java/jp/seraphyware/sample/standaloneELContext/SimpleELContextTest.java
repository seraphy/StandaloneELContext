package jp.seraphyware.sample.standaloneELContext;

import java.math.BigDecimal;

import javax.el.ValueExpression;
import javax.el.VariableMapper;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class SimpleELContextTest extends TestCase {

	/**
	 * Create the test case
	 * 
	 * @param testName
	 *            name of the test case
	 */
	public SimpleELContextTest(String testName) {
		super(testName);
	}

	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite() {
		return new TestSuite(SimpleELContextTest.class);
	}

	public void testSimple() {
		SimpleELContext elProc = new SimpleELContext();

		// ローカル変数の定義
		elProc.defineBean("foo", new BigDecimal("123"));
		elProc.defineBean("bar", "brabrabra");

		{
			// evalによる評価
			// EL式は、内部でbracket${}で必ず囲まれるので二重にならないように注意.
			Number ret = (Number)elProc.eval("foo + 1");
			assertEquals(124, ret.intValue());
		}
		
		{
			// getValueによる評価
			String ret = (String)elProc.getValueNb("${bar}☆${foo}", String.class);
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
			VariableMapper varMapper = elProc.getVariableMapper();

			ValueExpression veFoo = varMapper.resolveVariable("foo");
			assertNull(veFoo);
			ValueExpression veBaz = varMapper.resolveVariable("baz");
			assertNull(veBaz);
		}
	}
	
}
