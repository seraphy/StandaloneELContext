package jp.seraphyware.sample.standaloneELContext;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.el.ArrayELResolver;
import javax.el.BeanELResolver;
import javax.el.CompositeELResolver;
import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.el.FunctionMapper;
import javax.el.ListELResolver;
import javax.el.MapELResolver;
import javax.el.MethodExpression;
import javax.el.PropertyNotFoundException;
import javax.el.ResourceBundleELResolver;
import javax.el.ValueExpression;
import javax.el.VariableMapper;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.sun.el.lang.VariableMapperImpl;

/**
 * ELContextを最低限の実装で使うための最小コード例.<br>
 */
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

	/**
	 * ビーンアクセスのテスト用のクラス
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

		public void mes(String mes) {
			System.out.println("mes=" + mes);
		}

		@Override
		public String toString() {
			return "(x: " + x + ", y:" + y + ")";
		}
	}

	/**
	 * [テスト] 最低限のEL式評価方法のテスト
	 */
	public void testMinimum() {
		final CompositeELResolver resolver = new CompositeELResolver();
		resolver.add(new ResourceBundleELResolver()); // リソースバンドルの解決用
		resolver.add(new MapELResolver()); // Map, Propertiesの解決用
		resolver.add(new ListELResolver()); // Listの解決用
		resolver.add(new ArrayELResolver()); // 配列の解決用
		resolver.add(new BeanELResolver()); // Beanのsetter/getterの解決用
		resolver.add(new MyImplicitELResolver()); // 独自の"implicit"という暗黙の変数のサポート

		final VariableMapper varMapper = new VariableMapperImpl(); // 借用

		final FunctionMapper funcMapper = new FunctionMapper() {

			private HashMap<String, Method> methods = new HashMap<String, Method>();

			{
				for (Method method : Math.class.getMethods()) {
					if (Modifier.isStatic(method.getModifiers())) {
						String name = method.getName();
						Class<?>[] params = method.getParameterTypes();
						boolean accept = true;
						if (params.length > 0) {
							if (!params[0].isAssignableFrom(double.class)) {
								// 第一引数がある場合、double型でなければ不可とする.
								accept = false;
							}
						}
						if (accept) {
							methods.put(name, method);
						}
					}
				}
			}

			@Override
			public Method resolveFunction(String prefix, String localName) {
				if ("math".equals(prefix)) {
					return methods.get(localName);
				}
				return null; // nullの場合は関数が未登録であることを示す
			}
		};

		ELContext elContext = new ELContext() {
			@Override
			public ELResolver getELResolver() {
				return resolver;
			}

			@Override
			public FunctionMapper getFunctionMapper() {
				return funcMapper;
			}

			@Override
			public VariableMapper getVariableMapper() {
				return varMapper;
			}
		};

		// EL式の評価ファクトリ
		ExpressionFactory ef = ExpressionFactory.newInstance();

		// EL式で評価するための変数を設定する.
		varMapper.setVariable("foo",
				ef.createValueExpression("FOO", String.class));

		MyBean myBean = new MyBean();
		myBean.setX(1);
		myBean.setY(2);
		varMapper.setVariable("bar",
				ef.createValueExpression(myBean, myBean.getClass()));

		// EL式を評価する.
		{
			String expression = "hello, ${foo}! ${bar.x + 234}";
			ValueExpression ve = ef.createValueExpression(elContext,
					expression, String.class);
			String ret = (String) ve.getValue(elContext);

			System.out.println("result=" + ret);
			// result=hello, FOO! 235
		}

		{
			// String expression2 = "Hello! ${bar.x}";
			// ValueExpression ve2 = ef.createValueExpression(elContext,
			// expression2, Object.class);
			// ve2.setValue(elContext, 123);
			// System.out.println("myBean.x=" + myBean.getX());
			// // myBean.x=123
		}

		{
			String expression = "${bar.mes('hello')}";
			MethodExpression me = ef.createMethodExpression(elContext,
					expression, Object.class, new Class[0]);
			me.invoke(elContext, new Object[0]);

			ValueExpression ve = ef.createValueExpression(elContext,
					expression, String.class);
			ve.getValue(elContext);
		}

		{
			String expression = "#{bar.mes}";
			MethodExpression me = ef.createMethodExpression(elContext,
					expression, Object.class, new Class[] { String.class });
			me.invoke(elContext, new Object[] { "こんにちは、世界!!" });
			// mes=こんにちは、世界!!
		}

		{
			Properties sysProps = System.getProperties();
			HashMap<String, Object> sysMap = new HashMap<String, Object>();
			for (String name : sysProps.stringPropertyNames()) {
				sysMap.put(name, sysProps.getProperty(name));
			}
			varMapper.setVariable("sys",
					ef.createValueExpression(sysMap, Map.class));

			// 文字列リストの変数を作成する.
			List<String> lst = Arrays.asList("aaa", "bbb", "ccc", "ddd");
			varMapper.setVariable("list",
					ef.createValueExpression(lst, List.class));

			// 暗黙の変数をもつEL式
			String expression = "${list[implicit.idx]} - ${sys['java.version']}";
			ValueExpression ve = ef.createValueExpression(elContext,
					expression, String.class);

			// "implicit"変数の値となるオブジェクト
			ImplicitContext implicitContext = new ImplicitContext();
			elContext.putContext(ImplicitContext.class, implicitContext);

			for (int idx = 0; idx < lst.size(); idx++) {
				implicitContext.put("idx", Integer.valueOf(idx));
				String ret = (String) ve.getValue(elContext);
				System.out.println("ret[" + idx + "]=" + ret);
			}
		}

		{
			varMapper.setVariable("a",
					ef.createValueExpression("10", String.class));
			varMapper.setVariable("b",
					ef.createValueExpression("20", String.class));
			String expression = "max=${math:max(a,b)}, min=${math:min(a,b)}";
			ValueExpression ve = ef.createValueExpression(elContext,
					expression, String.class);
			String ret = (String) ve.getValue(elContext);
			System.out.println("ret=" + ret);
		}
	}
	
	public void testUndefinedVarSet() {
		final CompositeELResolver resolver = new CompositeELResolver();
		resolver.add(new ResourceBundleELResolver()); // リソースバンドルの解決用
		resolver.add(new MapELResolver()); // Map, Propertiesの解決用
		resolver.add(new ListELResolver()); // Listの解決用
		resolver.add(new ArrayELResolver()); // 配列の解決用
		resolver.add(new BeanELResolver()); // Beanのsetter/getterの解決用

		final VariableMapper varMapper = new VariableMapperImpl(); // 借用

		final FunctionMapper funcMapper = new FunctionMapper() {
			@Override
			public Method resolveFunction(String prefix, String localName) {
				return null; // nullの場合は関数が未登録であることを示す
			}
		};

		ELContext elContext = new ELContext() {
			@Override
			public ELResolver getELResolver() {
				return resolver;
			}

			@Override
			public FunctionMapper getFunctionMapper() {
				return funcMapper;
			}

			@Override
			public VariableMapper getVariableMapper() {
				return varMapper;
			}
		};

		// EL式の評価ファクトリ
		ExpressionFactory ef = ExpressionFactory.newInstance();

		{
			String expression = "${baz}";
			ValueExpression ve = ef.createValueExpression(elContext,
					expression, String.class);
			try {
				ve.setValue(elContext, "BAZ!!");
				assertTrue(false);

			} catch (PropertyNotFoundException ex) {
				// javax.el.PropertyNotFoundException: 
				// "ELResolver cannot handle a null base Object with identifier 'baz'"
				assertTrue(true);
			}
		}
	}
}
