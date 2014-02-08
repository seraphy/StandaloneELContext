package jp.seraphyware.sample.standaloneELContext;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import javax.el.ArrayELResolver;
import javax.el.BeanELResolver;
import javax.el.CompositeELResolver;
import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.el.FunctionMapper;
import javax.el.ListELResolver;
import javax.el.MapELResolver;
import javax.el.PropertyNotFoundException;
import javax.el.PropertyNotWritableException;
import javax.el.ResourceBundleELResolver;
import javax.el.ValueExpression;
import javax.el.VariableMapper;

/**
 * ELContextを簡易構築するためのコンビニエンスクラス.<br>
 * ローカル変数をサポートしている.<br>
 * getValue, setValue, eval等、EL3.0のELProcessorに似せた使い方を想定している.<br>
 */
public class SimpleELContext extends ELContext {

	public static ExpressionFactory exprFactory = ExpressionFactory
			.newInstance();

	/**
	 * 変数を保持するマップ.<br>
	 */
	private Map<String, ValueExpression> varMap = new HashMap<String, ValueExpression>();

	/**
	 * ローカル変数用のビーンマップ.<br>
	 */
	private Map<String, Object> beansMap = new HashMap<String, Object>();

	/**
	 * 関数を保持するマップ.<br>
	 * プリフィックスをキーとし、関数名をキーとし関数へのメソッドを値とするマップを値とするマップ.<br>
	 */
	private Map<String, Map<String, Method>> funcMap = new HashMap<String, Map<String, Method>>();

	/**
	 * VariableMapperの実装
	 */
	private VariableMapper varMapper;

	/**
	 * FunctionMapperの実装
	 */
	private FunctionMapper funcMapper;

	/**
	 * ELResolverの実装
	 */
	private ELResolver elResolver;

	/**
	 * カスタムELResolver
	 */
	private CompositeELResolver customResolvers;

	/**
	 * 初期化子
	 */
	{
		// 変数のマッパーを作成する.
		varMapper = new VariableMapper() {
			@Override
			public ValueExpression resolveVariable(String variable) {
				return varMap.get(variable);
			}

			@Override
			public ValueExpression setVariable(String variable,
					ValueExpression expression) {
				return varMap.put(variable, expression);
			}
		};

		// 関数のマッパーを作成する.
		funcMapper = new FunctionMapper() {
			@Override
			public Method resolveFunction(String prefix, String localName) {
				Map<String, Method> methods = funcMap.get(prefix);
				if (methods != null) {
					return methods.get(localName);
				}
				// 登録されていない場合
				return null;
			}
		};

		// ELResolverを拡張するためのエントリ
		customResolvers = new CompositeELResolver();

		// 標準のELResolverの定義
		CompositeELResolver resolver = new CompositeELResolver();
		resolver.add(new LocalBeanELResolver(beansMap));
		resolver.add(customResolvers);
		resolver.add(new MapELResolver());
		resolver.add(new ResourceBundleELResolver());
		resolver.add(new ListELResolver());
		resolver.add(new ArrayELResolver());
		resolver.add(new BeanELResolver());
		elResolver = resolver;
	}

	public static ExpressionFactory getFactory() {
		return exprFactory;
	}

	@Override
	public VariableMapper getVariableMapper() {
		return varMapper;
	}

	@Override
	public FunctionMapper getFunctionMapper() {
		return funcMapper;
	}

	@Override
	public ELResolver getELResolver() {
		return elResolver;
	}

	/**
	 * カスタムELResolverをコンテキストに追加する.<br>
	 * 
	 * @param cELResolver
	 *            The new ELResolver to be added to the context
	 */
	public void addELResolver(ELResolver cELResolver) {
		customResolvers.add(cELResolver);
	}

	/**
	 * ローカル変数を保持しているマップを取得する.<br>
	 * 
	 * @return the bean repository
	 */
	public Map<String, Object> getBeans() {
		return beansMap;
	}

	/**
	 * EL式を${}で囲む.<br>
	 * EL3のELProcessorに似せるため.<br>
	 * 
	 * @param expression
	 * @return
	 */
	private String bracket(String expression) {
		return "${" + expression + '}';
	}

	/**
	 * 変数を定義する.<br>
	 * VariableMapperに直接、オブジェクトをValueExpressionへのラッパにして設定する.<br>
	 * 
	 * @param variable
	 *            変数名
	 * @param expression
	 *            変数の値
	 */
	public void setVariable(String variable, Object expression) {
		ValueExpression exp = exprFactory.createValueExpression(expression,
				Object.class);
		getVariableMapper().setVariable(variable, exp);
	}

	/**
	 * ローカル変数を定義する.
	 * 
	 * @param name
	 *            ビーン名
	 * @param bean
	 *            ビーンのオブジェクト
	 */
	public void defineBean(String name, Object bean) {
		if (name == null) {
			throw new IllegalArgumentException();
		}
		beansMap.put(name, bean);
	}

	/**
	 * 関数を定義する.
	 * 
	 * @param prefix
	 * @param localName
	 *            関数名、空文字の場合はメソッド名を採用する.
	 * @param method
	 *            メソッド
	 * @throws NoSuchMethodException
	 *             メソッドがstaticでない場合
	 */
	public void defineFunction(String prefix, String localName, Method method)
			throws NoSuchMethodException {
		if (prefix == null || localName == null || method == null) {
			throw new NullPointerException("Null argument for defineFunction");
		}
		if (!Modifier.isStatic(method.getModifiers())) {
			throw new NoSuchMethodException(
					"The method specified in defineFunction must be static: "
							+ method);
		}
		if (localName.equals("")) {
			localName = method.getName();
		}

		Map<String, Method> methods = funcMap.get(prefix);
		if (methods == null) {
			methods = new HashMap<String, Method>();
			funcMap.put(prefix, methods);
		}
		methods.put(localName, method);
	}

	/**
	 * EL式を評価する.<br>
	 * 戻り値の型はObject型(汎用)とする.<br>
	 * 内部的にはgetValueと変わらない.<br>
	 * 
	 * @param expression
	 *            The EL expression to be evaluated.
	 * @return The result of the expression evaluation.
	 */
	public Object eval(String expression) {
		return getValue(expression, Object.class);
	}

	/**
	 * 戻り値の型を指定してEL式を評価する.<br>
	 * 
	 * @param expression
	 *            The EL expression to be evaluated.
	 * @param expectedType
	 *            Specifies the type that the resultant evaluation will be
	 *            coerced to.
	 * @return The result of the expression evaluation.
	 */
	public Object getValue(String expression, Class<?> expectedType) {
		ValueExpression exp = exprFactory.createValueExpression(this,
				bracket(expression), expectedType);
		return exp.getValue(this);
	}

	/**
	 * 戻り値の型を指定してEL式を評価する.<br>
	 * EL式は「${}」によって囲まれず、そのまま使用されます.<br>
	 * EL2.2では文字列係合演算子が使えないため、"${foo}${bar}"のように結合させる方法が必要なため.<br>
	 * 
	 * @param expression
	 *            The EL expression to be evaluated.
	 * @param expectedType
	 *            Specifies the type that the resultant evaluation will be
	 *            coerced to.
	 * @return The result of the expression evaluation.
	 */
	public Object getValueNb(String expression, Class<?> expectedType) {
		ValueExpression exp = exprFactory.createValueExpression(this,
				expression, expectedType);
		return exp.getValue(this);
	}

	/**
	 * EL式が示すプロパティを新しい値に更新する.<br>
	 * 
	 * @param expression
	 *            The target expression
	 * @param value
	 *            The new value to set.
	 * @throws PropertyNotFoundException
	 *             if one of the property resolutions failed because a specified
	 *             variable or property does not exist or is not readable.
	 * @throws PropertyNotWritableException
	 *             if the final variable or property resolution failed because
	 *             the specified variable or property is not writable.
	 * @throws ELException
	 *             if an exception was thrown while attempting to set the
	 *             property or variable. The thrown exception must be included
	 *             as the cause property of this exception, if available.
	 */
	public void setValue(String expression, Object value) {
		ValueExpression exp = exprFactory.createValueExpression(this,
				bracket(expression), Object.class);
		exp.setValue(this, value);
	}
}
