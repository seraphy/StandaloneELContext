package jp.seraphyware.sample.standaloneELContext;

import java.beans.FeatureDescriptor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.PropertyNotFoundException;
import javax.el.PropertyNotWritableException;

/**
 * EL式の第一要素に指定されるオブジェクト名を解決するためのELResolver.<br>
 * ${first.second}のようなEL式がある場合、このfirstの変数名を解決する.<br>
 */
public abstract class StandaloneBaseELResolver extends ELResolver {

	/**
	 * EL式の第一要素に指定されるオブジェクト名からオフジェクトを索引するための
	 * Mapオブジェクトを、ELContextに設定されているコンテキストから取得する.<br>
	 * 派生クラスで、ELContextからマップとして設定されている
	 * コンテキストを取得するように実装することを想定している.<br>
	 *
	 * @param elContext
	 * @return このELResolverが解決する変数名をキーとし、そのオブジェクトを値とするマップ
	 */
	protected abstract Map<String, Object> getLocalContext(ELContext elContext);

	@Override
	public Object getValue(ELContext elContext, Object base,
			Object property) throws NullPointerException,
			PropertyNotFoundException, ELException {
		if (elContext == null) {
			throw new NullPointerException();
		}

		if (base == null) {
			// baseがnullということは、以下のEL式の"first"の要素を示している.
			// ${first.second}
			// この場合、propertyにはfirstの名前が格納されている.
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
}
