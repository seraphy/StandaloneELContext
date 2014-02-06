package jp.seraphyware.sample.standaloneELContext;

import java.beans.FeatureDescriptor;
import java.util.Collections;
import java.util.Iterator;

import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.PropertyNotWritableException;

/**
 * 暗黙の変数"implicit"をELContextから取得するためのELResolverの実装例.
 */
class MyImplicitELResolver extends ELResolver {

	@Override
	public Class<?> getCommonPropertyType(ELContext context, Object base) {
		if (base == null) {
			// baseがnull、つまり${first.xxxx}のfirstの場合は、
			// 文字列として変数名を受け取ることを示す.
			return String.class;
		}
		return null;
	}

	@Override
	public Iterator<FeatureDescriptor> getFeatureDescriptors(
			ELContext context, Object base) {
		if (base != null) {
			// とりあえず空を返しておく.(手抜き、なくてもEL式は動く)
			return Collections.<FeatureDescriptor> emptyList().iterator();
		}
		return null;
	}

	@Override
	public Class<?> getType(ELContext context, Object base, Object property) {
		if (base == null && property != null) {
			String name = property.toString();
			if ("implicit".equals(name)) {
				// ${first.second}の、firstの場合、
				// それが"implicit"という名前であれば、ImplicitContextクラスを返す.
				context.setPropertyResolved(true);
				return ImplicitContext.class;
			}
		}
		return null;
	}

	@Override
	public Object getValue(ELContext context, Object base, Object property) {
		if (base == null && property != null) {
			String name = property.toString();
			if ("implicit".equals(name)) {
				// ${first.second}の、firstの場合、
				// それが"implicit"という名前であれば、ELContextに設定されている
				// ImplicitContextコンテキストを返す.
				// ※ ELContext#setContext()で事前に設定しておくこと.
				context.setPropertyResolved(true);
				return context.getContext(ImplicitContext.class);
			}
		}
		return null;
	}

	@Override
	public boolean isReadOnly(ELContext context, Object base,
			Object property) {
		return true;
	}

	@Override
	public void setValue(ELContext context, Object base, Object property,
			Object value) {
		throw new PropertyNotWritableException("代入はサポートされていません/base="
				+ base + "/property=" + property);
	}
}