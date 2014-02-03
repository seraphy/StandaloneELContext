package jp.seraphyware.sample.standaloneELContext;

import java.beans.FeatureDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.el.ArrayELResolver;
import javax.el.BeanELResolver;
import javax.el.CompositeELResolver;
import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.FunctionMapper;
import javax.el.ListELResolver;
import javax.el.MapELResolver;
import javax.el.PropertyNotFoundException;
import javax.el.PropertyNotWritableException;
import javax.el.ValueExpression;
import javax.el.VariableMapper;

/**
 * EL式を単独で使用するためのELコンテキスト.<br>
 * このELコンテキストを構築後、setContextで、このクラスをキーとして、
 * キーは文字列、値はオブジェクトとするマップをコンテキスト値として設定する.<br>
 * EL式は、このコンテキストに設定されている値について解釈する.<br>
 * 
 * 参考:
 * ttp://grepcode.com/file/repo1.maven.org/maven2/javax.servlet.jsp/jsp-api/2.2.1-b03/javax/servlet/jsp/el/ScopedAttributeELResolver.java#ScopedAttributeELResolver
 * ttp://mk.hatenablog.com/entry/20041210/1132029220
 * ttp://oss.infoscience.co.jp/myfaces/cwiki.apache.org/confluence/display/MYFACES/ELResolver+ordering.html
 * ttp://d.hatena.ne.jp/shin/20090426/p1
 * ttp://kiruah.sblo.jp/pages/user/iphone/article?article_id=56792402
 */
public class StandaloneELContext extends ELContext {

	/**
	 * リゾルバ
	 */
	private ELResolver resolver;

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
		this.funcMapper = new FunctionMapper() {
			@Override
			public Method resolveFunction(String prefix, String localName) {
				System.out.println("*func prefix=" + prefix + "/localName="
						+ localName);
				return null; // 常に該当なし
			}
		};

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

		// 引数で指定されたハッシュマップから名前解決するリゾルバ.
		final ELResolver localResolver = new ELResolver() {
			
			protected Map<String, Object> getLocalContext(ELContext elContext) {
				@SuppressWarnings("unchecked")
				Map<String, Object> ctx = (Map<String, Object>)
						elContext.getContext(StandaloneELContext.class);
				if (ctx == null) {
					throw new IllegalStateException("missing context: " + StandaloneELContext.class );
				}
				return ctx;
			}
			
			@Override
			public Object getValue(ELContext elContext, Object base,
					Object property) throws NullPointerException,
					PropertyNotFoundException, ELException {
				if (elContext == null) {
					throw new NullPointerException();
				}

				if (base == null) {
					elContext.setPropertyResolved(true);
					if (property instanceof String) {
						Map<String, Object> ctx = getLocalContext(elContext);
						String name = (String) property;
						return ctx.get(name);
					}
				}

				// 未解決の場合は、そのまま次へ
				return null;
			}

			@Override
			public Class<?> getType(ELContext elContext, Object base, Object property)
					throws NullPointerException, PropertyNotFoundException,
					ELException {
				if (elContext == null) {
					throw new NullPointerException();
				}
				if (base == null) {
					elContext.setPropertyResolved(true);
					return Object.class;
				}
				return null;
			}

			@Override
			public boolean isReadOnly(ELContext elContext, Object base, Object property)
					throws NullPointerException, PropertyNotFoundException,
					ELException {
				if (elContext == null) {
					throw new NullPointerException();
				}
				if (base == null) {
					elContext.setPropertyResolved(true);
				}
				return false;
			}

			@Override
			public void setValue(ELContext elContext, Object base, Object property,
					Object value) throws NullPointerException,
					PropertyNotFoundException, PropertyNotWritableException,
					ELException {
				if (elContext == null) {
					throw new NullPointerException();
				}
				if (base == null) {
					elContext.setPropertyResolved(true);
					if (property instanceof String) {
						Map<String, Object> ctx = getLocalContext(elContext);
						String name = (String) property;
						ctx.put(name, value);
					}
				}
			}

			@Override
			public Iterator<FeatureDescriptor> getFeatureDescriptors(
					ELContext elContext, Object base) {
				 ArrayList<FeatureDescriptor> list = new ArrayList<FeatureDescriptor>();
					Map<String, Object> ctx = getLocalContext(elContext);
				 for (Map.Entry<String, Object> entry : ctx.entrySet()) {
					 String key = entry.getKey();
					 Object val = entry.getValue();
					 FeatureDescriptor descriptor = new FeatureDescriptor();
					 descriptor.setName(key);
					 descriptor.setDisplayName(key);
					 descriptor.setShortDescription("standalone attribute");
					 descriptor.setExpert(false);
					 descriptor.setHidden(false);
					 descriptor.setPreferred(true);
					 descriptor.setValue("type", val != null ? val.getClass() : Object.class);
					 descriptor.setValue("resolvableAtDesignTime", Boolean.TRUE);
					 list.add(descriptor);
				 }
				 return list.iterator();
			}

			@Override
			public Class<?> getCommonPropertyType(ELContext elContext,
					Object base) {
				if (base == null) {
					return String.class;
				}
				return null;
			}
		};

		// EL式のリゾルバの複合体.
		// (解決する順序で設定する.)
		final CompositeELResolver resolver = new CompositeELResolver();
		resolver.add(new MapELResolver());
		resolver.add(new ListELResolver());
		resolver.add(new ArrayELResolver());
		resolver.add(new BeanELResolver());
		resolver.add(localResolver);
		this.resolver = resolver;
	}

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
}
