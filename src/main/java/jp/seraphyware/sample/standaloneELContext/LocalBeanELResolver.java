package jp.seraphyware.sample.standaloneELContext;

import java.beans.FeatureDescriptor;
import java.util.Iterator;
import java.util.Map;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.PropertyNotWritableException;

/**
 * ローカル変数用のELResolver.<br>
 * (EL2.2用).<br>
 */
public class LocalBeanELResolver extends ELResolver {

	/**
	 * ビーン定義.<br>
	 * キーはビーン名、値はビーンのオブジェクト
	 */
	private Map<String, Object> beansMap;

	/**
	 * コンストラクタ
	 * 
	 * @param beansMap
	 *            ビーン定義を保持するマップ
	 */
	public LocalBeanELResolver(Map<String, Object> beansMap) {
		if (beansMap == null) {
			throw new IllegalArgumentException();
		}
		this.beansMap = beansMap;
	}

	public void setBeansMap(Map<String, Object> beansMap) {
		if (beansMap == null) {
			throw new IllegalArgumentException();
		}
		this.beansMap = beansMap;
	}

	public Map<String, Object> getBeansMap() {
		return beansMap;
	}

	/**
	 * もしbaseがnullでありpropertyが文字列であればビーン名とし、 beansMapにビーン名が登録されていれば、その値を返す.<br>
	 * 登録されていない場合は未定義とする.<br>
	 * 
	 * @param context
	 *            The context of this evaluation.
	 * @param base
	 *            <code>null</code>
	 * @param property
	 *            The name of the bean.
	 * @return If the <code>propertyResolved</code> property of
	 *         <code>ELContext</code> was set to <code>true</code>, then the
	 *         value of the bean with the given name. Otherwise, undefined.
	 * @throws NullPointerException
	 *             if context is <code>null</code>.
	 * @throws ELException
	 *             if an exception was thrown while performing the property or
	 *             variable resolution. The thrown exception must be included as
	 *             the cause property of this exception, if available.
	 */
	@Override
	public Object getValue(ELContext context, Object base, Object property) {
		if (context == null) {
			throw new NullPointerException();
		}
		if (base == null && property instanceof String) {
			if (beansMap.containsKey((String) property)) {
				// ビーンが登録されている場合のみ
				context.setPropertyResolved(true);
				return beansMap.get((String) property);
			}
		}
		return null;
	}

	/**
	 * もし、baseがnullであり、propertyが文字列であれば、ビーン名とし、 ビーンマップに対して値を登録する.<br>
	 * 既存のものがあれば上書きされ、なければ新規に作成される.<br>
	 * 
	 * @param context
	 *            The context of this evaluation.
	 * @param base
	 *            <code>null</code>
	 * @param property
	 *            The name of the bean
	 * @param value
	 *            The value to set the bean with the given name to.
	 * @throws NullPointerException
	 *             if context is <code>null</code>
	 * @throws PropertyNotWritableException
	 *             if the BeanNameResolver does not allow the bean to be
	 *             modified.
	 * @throws ELException
	 *             if an exception was thrown while attempting to set the bean
	 *             with the given name. The thrown exception must be included as
	 *             the cause property of this exception, if available.
	 */
	@Override
	public void setValue(ELContext context, Object base, Object property,
			Object value) {
		if (context == null) {
			throw new NullPointerException();
		}

		if (base == null && property instanceof String) {
			beansMap.put((String) property, value);
			context.setPropertyResolved(true);
		}
	}

	/**
	 * もしbaseがnullでありpropertyが文字列であれば、ビーン名であるとし beansMapにビーン名が登録されていれば、そのタイプを返す.<br>
	 * 登録されていない場合は未定義とする.<br>
	 * 
	 * @param context
	 *            The context of this evaluation.
	 * @param base
	 *            <code>null</code>
	 * @param property
	 *            The name of the bean.
	 * @return If the <code>propertyResolved</code> property of
	 *         <code>ELContext</code> was set to <code>true</code>, then the
	 *         type of the bean with the given name. Otherwise, undefined.
	 * @throws NullPointerException
	 *             if context is <code>null</code>.
	 * @throws ELException
	 *             if an exception was thrown while performing the property or
	 *             variable resolution. The thrown exception must be included as
	 *             the cause property of this exception, if available.
	 */
	@Override
	public Class<?> getType(ELContext context, Object base, Object property) {

		if (context == null) {
			throw new NullPointerException();
		}

		if (base == null && property instanceof String) {
			if (beansMap.containsKey((String) property)) {
				// beansMapに登録がある場合のみ
				context.setPropertyResolved(true);
				Object val = beansMap.get((String) property);
				return val == null ? Object.class : val.getClass();
			}
		}
		return null;
	}

	/**
	 * もし、baseがnullでありpropertyが文字列であれば、ビーン名であると仮定し 常に書き込み可能であると返却する.<br>
	 * 
	 * @param context
	 *            The context of this evaluation.
	 * @param base
	 *            <code>null</code>
	 * @param property
	 *            The name of the bean.
	 * @return If the <code>propertyResolved</code> property of
	 *         <code>ELContext</code> was set to <code>true</code>, then
	 *         <code>true</code> if the property is read-only or
	 *         <code>false</code> if not; otherwise undefined.
	 * @throws NullPointerException
	 *             if context is <code>null</code>.
	 * @throws ELException
	 *             if an exception was thrown while performing the property or
	 *             variable resolution. The thrown exception must be included as
	 *             the cause property of this exception, if available.
	 */
	@Override
	public boolean isReadOnly(ELContext context, Object base, Object property) {
		if (context == null) {
			throw new NullPointerException();
		}

		if (base == null && property instanceof String) {
			context.setPropertyResolved(true);
			return false; // 常に書き込み可とする.
		}
		return false;
	}

	/**
	 * 常にnullを返す.<br>
	 * とくにビーンの名前を返却すべき理由がないため.<br>
	 * 
	 * @param context
	 *            The context of this evaluation.
	 * @param base
	 *            <code>null</code>.
	 * @return <code>null</code>.
	 */
	public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context,
			Object base) {
		return null;
	}

	/**
	 * 常にStringクラスを返す.<br>
	 * ビーン名は常に文字列であるため.<br>
	 * 
	 * @param 評価するコンテキスト
	 * @param base
	 *            <code>null</code>.
	 * @return <code>String.class</code>.
	 */
	@Override
	public Class<?> getCommonPropertyType(ELContext context, Object base) {
		return String.class;
	}
}
