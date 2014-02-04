package jp.seraphyware.sample.standaloneELContext;

import java.util.HashMap;
import java.util.Map;

import javax.el.ArrayELResolver;
import javax.el.BeanELResolver;
import javax.el.CompositeELResolver;
import javax.el.ELContext;
import javax.el.FunctionMapper;
import javax.el.ListELResolver;
import javax.el.MapELResolver;
import javax.el.ValueExpression;
import javax.el.VariableMapper;

/**
 * EL式を単独で使用するためのELコンテキスト.<br>
 */
public class StandaloneELContext extends ELContext {

	/**
	 * リゾルバ
	 */
	private CompositeELResolver resolver;

	/**
	 * 関数マップ
	 */
	private FunctionMapper funcMapper;

	/**
	 * 変数マップ
	 */
	private VariableMapper varMapper;

	/**
	 * 変数のホルダ
	 */
	private Map<String, ValueExpression> variables = new HashMap<String, ValueExpression>();

	/**
	 * コンストラクタ.<br>
	 */
	public StandaloneELContext() {
		init();
	}

	/**
	 * 初期化します.<br>
	 */
	private final void init() {
		// 関数の解決
		this.funcMapper = new SimpleELFunctionMapper();

		// 変数の解決
		this.varMapper = new VariableMapper() {
			@Override
			public ValueExpression resolveVariable(String variable) {
				return variables.get(variable);
			}

			@Override
			public ValueExpression setVariable(String variable,
					ValueExpression expression) {
				return variables.put(variable, expression);
			}
		};

		// EL式のリゾルバの複合体.
		// (解決する順序で設定する.)
		final CompositeELResolver resolver = new CompositeELResolver();
		initELResolver(resolver);
		this.resolver = resolver;
	}

	/**
	 * ELResolverを初期化するためのフックをかけられる場所
	 * @param resolver
	 */
	protected void initELResolver(CompositeELResolver resolver) {
		resolver.add(new MapELResolver());
		resolver.add(new ListELResolver());
		resolver.add(new ArrayELResolver());
		resolver.add(new BeanELResolver());
	}

	@Override
	public CompositeELResolver getELResolver() {
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
}
